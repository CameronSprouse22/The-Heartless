package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Contract for all game events.
 */
public interface EventObjectInterface {

    boolean checkStartConditions();

    boolean checkEndConditions();

    GameObject getGame();

    void execute();
}
