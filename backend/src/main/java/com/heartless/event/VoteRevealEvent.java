package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Vote reveal event — reveals the result of the banish vote.
 */
public class VoteRevealEvent implements EventObjectInterface {

    private final GameObject game;

    public VoteRevealEvent(GameObject game) {
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
        // Stub: reveal vote outcome to all players
    }
}
