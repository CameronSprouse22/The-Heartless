package com.heartless.gamethread;

import com.heartless.event.EventObjectInterface;
import com.heartless.model.GameObject;
import com.heartless.model.RoundObject;
import com.heartless.model.enums.GameStatusEnum;

import java.util.List;

/**
 * Orchestrates the full game lifecycle: Init → Start → End → Over.
 * Pure Java — no Spring annotations.
 */
public class GameThread {

    private final GameObject gameObject;
    private final GameCriteriaObject gameCriteriaObject;
    private final List<EventObjectInterface> eventList;
    private String statusString;

    public GameThread(GameObject gameObject,
                      GameCriteriaObject gameCriteriaObject,
                      List<EventObjectInterface> eventList) {
        this.gameObject = gameObject;
        this.gameCriteriaObject = gameCriteriaObject;
        this.eventList = eventList;
        this.statusString = "";
    }

    /**
     * Initialize game: set up lobby state.
     */
    public void gameInit() {
        this.statusString = "Lobby";
        gameObject.setCurrentTask("Waiting for players");
    }

    private static final int MAX_ROUNDS = 100;

    /**
     * Main game loop: iterate rounds executing events until criteria met.
     */
    public void gameStart() {
        this.statusString = "Round " + gameObject.getRound();
        gameObject.setCurrentTask("In progress");
        int roundsPlayed = 0;

        // Run rounds while game should continue
        while (gameCriteriaObject.checkGameConditions(gameObject)
                && roundsPlayed < MAX_ROUNDS) {
            runSingleRound();
            roundsPlayed++;
            if (!gameCriteriaObject.checkGameConditions(gameObject)) {
                break;
            }
            gameObject.incrementRound();
            this.statusString = "Round " + gameObject.getRound();
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
                event.execute();
            }
        }
    }

    /**
     * End the game: transition to OVER.
     */
    public void gameEnd() {
        this.statusString = "Game Over";
        gameObject.setCurrentTask("Finished");
        gameObject.transitionToOver();
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
}
