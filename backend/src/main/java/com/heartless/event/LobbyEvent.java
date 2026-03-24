package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

public class LobbyEvent implements EventObjectInterface {

    private final GameObject game;

    public LobbyEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() { return true; }

    @Override
    public boolean checkEndConditions() { return true; }

    @Override
    public GameObject getGame() { return game; }

    @Override
    public void execute() {
        MenuControl mc = new MenuControl();
        mc.setAllChatEnabled(true);
        mc.setGameOptionsEnabled(true);
        game.setMenuControl(mc);
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setAllChatEnabled(true);
        mc.setGameOptionsEnabled(true);
        return GameState.fromMenuControl(mc, game);
    }
}
