package com.heartless.event;

import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

/**
 * Contract for all game events.
 */
public interface EventObjectInterface {

    boolean checkStartConditions();

    boolean checkEndConditions();

    GameObject getGame();

    void execute();

    /**
     * Creates and returns the MenuControl for this event,
     * defining which menu items are enabled while this event is active.
     */
    MenuControl setMenuItems();
}
