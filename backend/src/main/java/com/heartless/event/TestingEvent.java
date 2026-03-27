package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

public class TestingEvent implements EventObjectInterface {

    private final GameObject game;

    public TestingEvent(GameObject game) {
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
        MenuControl mc = buildAllEnabledMenu();
        game.setMenuControl(mc);
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = buildAllEnabledMenu();
        return GameState.fromMenuControl(mc, game);
    }

    private MenuControl buildAllEnabledMenu() {
        MenuControl mc = new MenuControl();
        mc.setTraitorChatEnabled(true);
        mc.setAllChatEnabled(true);
        mc.setBanishVoteEnabled(true);
        mc.setMurderVoteEnabled(true);
        mc.setIndividualChatEnabled(true);
        mc.setActionsEnabled(true);
        mc.setGameLogsEnabled(true);
        mc.setGameOptionsEnabled(true);
        return mc;
    }
}
