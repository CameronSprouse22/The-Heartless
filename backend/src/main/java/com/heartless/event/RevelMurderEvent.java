package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Revel murder event — reveals the murder outcome to all players.
 */
public class RevelMurderEvent implements EventObjectInterface {

    private final GameObject game;

    public RevelMurderEvent(GameObject game) {
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
        // Stub: reveal murder result
    }
}
