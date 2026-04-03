package com.heartless.gamethread;

import com.heartless.event.AfterLifeGameEvent;
import com.heartless.event.EventObjectInterface;
import com.heartless.event.TestingEvent;
import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.RoundObject;
import com.heartless.model.enums.GameStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full game lifecycle: Init → Start → End → Over.
 * Pure Java — no Spring annotations.
 */
public class GameThread {

    private static final Logger log = LogManager.getLogger(GameThread.class);

    private final GameObject gameObject;
    private final GameCriteriaObject gameCriteriaObject;
    private List<EventObjectInterface> eventList;
    private EventObjectInterface currentEvent;
    private String statusString = "";
    private SimpMessagingTemplate messagingTemplate;


    public GameThread(GameObject gameObject, GameCriteriaObject gameCriteriaObject) {
        this.gameObject = gameObject;
        this.gameCriteriaObject = gameCriteriaObject;
        this.eventList = new java.util.ArrayList<>();
    }

    public GameThread(GameObject gameObject, GameCriteriaObject gameCriteriaObject,
                      List<EventObjectInterface> eventList) {
        this.gameObject = gameObject;
        this.gameCriteriaObject = gameCriteriaObject;
        this.eventList = eventList != null ? eventList : new java.util.ArrayList<>();
    }

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Initialize game: set up lobby state.
     */
    public void gameInit() {
        log.info("Game initialising — gameId={}", gameObject.getGameId());
        this.statusString = "Lobby";
        gameObject.setCurrentTask("Waiting for players");
        gameStart();
        log.debug("gameInit complete — status={}", this.statusString);
    }

    private static final int MAX_ROUNDS = 100;

    /**
     * Main game loop: runs in a background thread, waits for each event to
     * expire, then broadcasts ROUND_STARTED for the next round.
     */
    public void gameStart() {
        this.statusString = "Round 0";
        Thread gameLoop = new Thread(() -> {
            int roundsPlayed = 0;
            while (roundsPlayed < MAX_ROUNDS) {
                currentEvent = new TestingEvent(gameObject);
                this.statusString = "Round " + roundsPlayed++;
                log.info("Game loop round {} — gameId={}", roundsPlayed, gameObject.getGameIdCode());

                RoundObject round = new RoundObject(Math.max(1, gameObject.getRound()));
                gameObject.addRound(round);
                currentEvent.execute();

                broadcastRoundStarted();

                long waitTime = currentEvent.getEventEndTime() - System.currentTimeMillis();
                if (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.info("Game loop interrupted — gameId={}", gameObject.getGameIdCode());
                        return;
                    }
                }

                gameObject.incrementRound();
            }
            gameEnd();
        }, "game-loop-" + gameObject.getGameIdCode());
        gameLoop.setDaemon(true);
        gameLoop.start();
    }

    /**
     * Execute the current event for this round.
     * Ends the game early if the event has timed out.
     */
    public void runCurrentEvent() {
        RoundObject round = new RoundObject(
                Math.max(1, gameObject.getRound()));
        gameObject.addRound(round);

        if (currentEvent == null) {
            log.warn("No current event to run — gameId={}", gameObject.getGameId());
            return;
        }
        if (System.currentTimeMillis() >= currentEvent.getEventEndTime()) {
            log.info("Event {} timed out — ending round early. gameId={}",
                    currentEvent.getClass().getSimpleName(), gameObject.getGameId());
            gameEnd();
            return;
        }
        if (currentEvent.checkStartConditions()) {
            log.debug("Executing event {} — round={} gameId={}",
                    currentEvent.getClass().getSimpleName(), gameObject.getRound(), gameObject.getGameId());
            currentEvent.execute();
            while (System.currentTimeMillis() >= currentEvent.getEventEndTime() && !currentEvent.checkEndConditions()) {
                log.info("Event {} expired after execute — ending round. gameId={}",
                        currentEvent.getClass().getSimpleName(), gameObject.getGameId());
                gameEnd();
            }
        } else {
            log.trace("Skipping event {} (conditions not met) — round={}",
                    currentEvent.getClass().getSimpleName(), gameObject.getRound());
        }
    }

    /**
     * End the game: transition to OVER.
     */
    public void gameEnd() {
        log.info("Game ending — gameId={} rounds={}", gameObject.getGameId(), gameObject.getRound());
        this.statusString = "Game Over";
        gameObject.setCurrentTask("Finished");
        gameObject.transitionToOver();
        log.info("Game over — gameId={}", gameObject.getGameId());
    }

    private void broadcastRoundStarted() {
        if (messagingTemplate == null) return;
        String gameCode = gameObject.getGameIdCode();
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "ROUND_STARTED");
        msg.put("round", gameObject.getRound());
        msg.put("eventEndTime", currentEvent != null ? currentEvent.getEventEndTime() : 0L);
        messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/event", msg);
        log.debug("Broadcast ROUND_STARTED — gameId={} round={}", gameCode, gameObject.getRound());
    }

    public GameObject getGameObject() {
        return gameObject;
    }

    public GameCriteriaObject getGameCriteriaObject() {
        return gameCriteriaObject;
    }

    public List<EventObjectInterface> getEventList() {
        return eventList;
    }

    public String getStatusString() {
        return statusString;
    }

    public EventObjectInterface getCurrentEvent() {
        return currentEvent;
    }

    /**
     * Build a GameState snapshot for the given player.
     * If the player is dead, returns AfterLifeGameEvent state.
     * Otherwise delegates to the current event's getGameState.
     */
    public GameState buildGameState(Player player) {
        if (player.isDead()) {
            log.debug("Player {} is dead — returning AfterLifeGameEvent state", player.getName());
            return new AfterLifeGameEvent(gameObject).getGameState();
        }
        if (currentEvent != null) {
            log.debug("Active event: {}", currentEvent.getClass().getSimpleName());
            return currentEvent.getGameState();
        }
        log.debug("No active event — using stored MenuControl");
        return GameState.fromMenuControl(gameObject.getMenuControl(), gameObject);
    }
}
