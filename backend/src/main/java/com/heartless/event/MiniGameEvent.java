package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Mini-game event — handles the challenge/mini-game phase.
 */
public class MiniGameEvent implements EventObjectInterface {

    private final GameObject game;

    public MiniGameEvent(GameObject game) {
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
        // Stub: mini-game execution
    }
}
