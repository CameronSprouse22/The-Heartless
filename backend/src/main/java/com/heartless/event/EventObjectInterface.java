package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;

/**
 * Contract for all game events.
 */
public interface EventObjectInterface {

    boolean checkStartConditions();

    boolean checkEndConditions();

    GameObject getGame();

    void execute();

    /**
     * Returns a GameState snapshot for this event,
     * custom to each event implementation.
     */
    GameState getGameState();
}
