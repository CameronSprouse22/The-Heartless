package com.heartless.event;

import com.heartless.model.GameObject;

/**
 * Lobby event — handles the waiting-for-players phase.
 */
public class LobbyEvent implements EventObjectInterface {

    private final GameObject game;

    public LobbyEvent(GameObject game) {
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
        // Stub: lobby logic (waiting for players to join)
    }
}
