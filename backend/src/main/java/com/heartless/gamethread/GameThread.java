package com.heartless.gamethread;

import com.heartless.event.AfterLifeGameEvent;
import com.heartless.event.EventObjectInterface;
import com.heartless.event.BanishPreEvent;
import com.heartless.event.BanishRevealEvent;
import com.heartless.event.BanishVoteEvent;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.push.PushNotificationService;
import com.heartless.model.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

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
    private PushNotificationService pushNotificationService;
    private SimpMessagingTemplate messagingTemplate;
    private final java.util.ArrayList<GameRoundObject> roundObjects = new java.util.ArrayList<>();
    private GameRoundObject currentRoundObject;
    private List<Player> skipRequestedList;
    private List<Player> pauseRequestedList;
    private List<Player> kickForInactivityRequestedList;


    public GameThread(GameObject gameObject, GameCriteriaObject gameCriteriaObject) {
        this.gameObject = gameObject;
        this.gameCriteriaObject = gameCriteriaObject;
        this.eventList = new java.util.ArrayList<>();
    }

    public GameThread(GameObject gameObject, GameCriteriaObject gameCriteriaObject,
                      List<EventObjectInterface> eventList) {
        this.gameObject = gameObject;
        this.gameCriteriaObject = gameCriteriaObject;
        this.eventList = eventList != null ? new java.util.ArrayList<>(eventList) : new java.util.ArrayList<>();
    }

    /**
     * Initialize game: set up lobby state.
     */
    public void gameInit() {
        log.info("Game initialising — gameId={}", gameObject.getGameId());
        this.statusString = "Lobby";
        gameObject.setCurrentTask("Waiting for players");
        Thread gameThread = new Thread(this::gameStart, "game-thread-" + gameObject.getGameId());
        gameThread.setDaemon(true);
        gameThread.start();
        log.debug("gameInit complete — game loop started in background — status={}", this.statusString);
    }

    private static final int MAX_ROUNDS = 100;

    /**
     * Main game loop: iterate rounds executing events until criteria met.
     */
    public void gameStart() {
        
        log.info("Game starting — gameId={}", gameObject.getGameId());
        this.statusString = "Round " + gameObject.getRound();
        gameObject.setCurrentTask("In progress");

        eventList.add(new BanishPreEvent(gameObject)); 
        eventList.add(new BanishVoteEvent(gameObject));  
        eventList.add(new BanishRevealEvent(gameObject)); 

        for (EventObjectInterface event : eventList) {
            if (!event.checkStartConditions()) {
                log.trace("Skipping event {} (conditions not met) — gameId={}",
                        event.getClass().getSimpleName(), gameObject.getGameId());
                continue;
            }
            currentEvent = event;
            log.debug("Starting event {} — gameId={}", event.getClass().getSimpleName(), gameObject.getGameId());
            boolean completed = runCurrentEvent();
            if (!completed) {
                log.info("Event {} did not complete — ending game. gameId={}",
                        event.getClass().getSimpleName(), gameObject.getGameId());
                gameEnd();
                return;
            }
            if (!gameCriteriaObject.checkGameConditions(gameObject)) {
                log.info("Game criteria met after event {} — gameId={}",
                        event.getClass().getSimpleName(), gameObject.getGameId());
                break;
            }
        }
    }

    /**
     * Runs the current event until its end condition is met or its time expires.
     *
     * @return true if the event completed normally, false if it timed out
     */
    public boolean runCurrentEvent() {
        log.warn("---->Starting "+currentEvent.getClass().getSimpleName());
        if (currentEvent == null) {
            log.warn("runCurrentEvent called with no current event — gameId={}", gameObject.getGameId());
            return false;
        }
        if (System.currentTimeMillis() >= currentEvent.getEventEndTime()) {
            log.info("Event {} timed out before executing — gameId={}",
                    currentEvent.getClass().getSimpleName(), gameObject.getGameId());
            return false;
        }
        log.debug("Running event {} — round={} gameId={}",
                currentEvent.getClass().getSimpleName(), gameObject.getRound(), gameObject.getGameId());
        // Reset per-player initial-message dismissal so the intro overlay shows again for this event
        gameObject.initInitialMessageDismissalStates();
        // Initialise event state once on start
        currentEvent.onStart();
        // Broadcast event start over WebSocket so all connected clients update
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend(
                    "/topic/games/" + gameObject.getGameIdCode() + "/event",
                    java.util.Map.of(
                            "type", "EVENT_STARTED",
                            "event", currentEvent.getClass().getSimpleName()
                    )
            );
        }
        // Fire web push notification now that event state is ready
        if (currentEvent.checkForNotifications() && pushNotificationService != null) {
            String notif = currentEvent.getStartNotification();
            if (notif != null && !notif.isBlank()) {
                pushNotificationService.notifyGame(
                        gameObject.getGameIdCode(), notif, "", "eventStarted");
            }
        }
        // Loop until end condition is satisfied or time runs out
        while (!currentEvent.endConditonsMeet(gameObject) || System.currentTimeMillis() >= currentEvent.getEventEndTime()) {
            if (System.currentTimeMillis() >= currentEvent.getEventEndTime()) {
                log.info("Event {} timed out — gameId={}",
                        currentEvent.getClass().getSimpleName(), gameObject.getGameId());
                break;
            }
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        return true;
    }

    /**
     * End the game: transition to OVER.
     */
    public void gameEnd() {
        log.info("Game ending — gameId={} rounds={}", gameObject.getGameId(), gameObject.getRound());
        this.statusString = "Game Over";
        gameObject.setCurrentTask("Finished");
        gameObject.transitionToOver();
        if (pushNotificationService != null) {
            pushNotificationService.notifyGame(
                    gameObject.getGameIdCode(), "Game Over", "The game has ended.", "eventEnding");
        }
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

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void setPushNotificationService(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    public java.util.ArrayList<GameRoundObject> getRoundObjects() { return roundObjects; }
    public GameRoundObject getCurrentRoundObject() { return currentRoundObject; }
    public void setCurrentRoundObject(GameRoundObject currentRoundObject) {
        this.currentRoundObject = currentRoundObject;
    }

    /**
     * Finalises the current round (if any), then creates and starts a new round
     * in both the GameThread and the GameObject.
     *
     * @return the newly created GameRoundObject
     */
    public GameRoundObject newRound() {
        gameObject.startNewRound();
        currentRoundObject = new GameRoundObject(gameObject.getRound());
        log.debug("New round started — round={} gameId={}", gameObject.getRound(), gameObject.getGameId());
        return currentRoundObject;
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
        log.debug("No active event — returning empty menu state");
        return GameState.fromMenuControl(new MenuControl(), gameObject);
    }
}
