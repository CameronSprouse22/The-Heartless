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

import java.util.List;

/**
 * Orchestrates the full game lifecycle: Init → Start → End → Over.
 * Pure Java — no Spring annotations.
 */
public class GameThread {

    private static final Logger log = LogManager.getLogger(GameThread.class);

    private final GameObject gameObject;
    private final GameCriteriaObject gameCriteriaObject;
    private 
    List<EventObjectInterface> eventList;
    private EventObjectInterface currentEvent;
    private String statusString = "";


    public GameThread(GameObject gameObject, GameCriteriaObject gameCriteriaObject) {
        this.gameObject = gameObject;
        this.gameCriteriaObject = gameCriteriaObject;
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
     * Main game loop: iterate rounds executing events until criteria met.
     */
    public void gameStart() {
        currentEvent = new TestingEvent(gameObject);
        log.info("Game starting — gameId={}", gameObject.getGameId());
        this.statusString = "Round " + gameObject.getRound();
        gameObject.setCurrentTask("In progress");
        int roundsPlayed = 0;

        // Run rounds while game should continue
        while (gameCriteriaObject.checkGameConditions(gameObject)
                && roundsPlayed < MAX_ROUNDS) {
            log.debug("Starting round {} — gameId={}", gameObject.getRound(), gameObject.getGameId());
            runSingleRound();
            roundsPlayed++;
            if (!gameCriteriaObject.checkGameConditions(gameObject)) {
                log.info("Game criteria met after round {} — gameId={}", roundsPlayed, gameObject.getGameId());
                break;
            }
            gameObject.incrementRound();
            this.statusString = "Round " + gameObject.getRound();
        }

        if (roundsPlayed >= MAX_ROUNDS) {
            log.warn("Game reached MAX_ROUNDS ({}) without satisfying criteria — gameId={}", MAX_ROUNDS, gameObject.getGameId());
        }
    }

    /**
     * Execute all events in sequence for a single round.
     */
    public void runSingleRound() {
        RoundObject round = new RoundObject(
                Math.max(1, gameObject.getRound()));
        gameObject.addRound(round);

        for (EventObjectInterface event : eventList) {
            if (event.checkStartConditions()) {
                log.debug("Executing event {} — round={} gameId={}",
                        event.getClass().getSimpleName(), gameObject.getRound(), gameObject.getGameId());
                this.currentEvent = event;
                event.execute();
            } else {
                log.trace("Skipping event {} (conditions not met) — round={}",
                        event.getClass().getSimpleName(), gameObject.getRound());
            }
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
    }p
}
