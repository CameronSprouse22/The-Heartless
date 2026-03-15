package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Pre-vote event — handles pre-vote discussion and preparation.
 */
public class PreVoteEvent implements EventObjectInterface {

    private final GameObject game;

    public PreVoteEvent(GameObject game) {
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
        // Stub: pre-vote discussion phase
    }
}
