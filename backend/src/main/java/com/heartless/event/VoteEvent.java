package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Vote event — handles the banishment voting phase.
 */
public class VoteEvent implements EventObjectInterface {

    private final GameObject game;

    public VoteEvent(GameObject game) {
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
        // Stub: banish vote collection and tally
    }
}
