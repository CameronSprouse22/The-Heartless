package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Tie-break event — resolves ties in the banish vote.
 */
public class TieBreakEvent implements EventObjectInterface {

    private final GameObject game;

    public TieBreakEvent(GameObject game) {
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
        // Stub: tie-break resolution
    }
}
