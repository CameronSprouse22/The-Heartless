package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Murder event — handles the traitors' murder phase.
 */
public class MurderEvent implements EventObjectInterface {

    private final GameObject game;

    public MurderEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() {
        return true;
    }

    @Override
    public boolean checkEndConditions() {
        return true;
    }

    @Override
    public GameObject getGame() {
        return game;
    }

    @Override
    public void execute() {
        // Stub: murder vote and resolution
    }
}
