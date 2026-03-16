package com.heartless.event;

import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

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
        game.setMenuControl(setMenuItems());
    }

    @Override
    public MenuControl setMenuItems() {
        MenuControl mc = new MenuControl();
        mc.setAllChatEnabled(true);
        mc.setTraitorChatEnabled(true);
        mc.setIndividualChatEnabled(true);
        mc.setGameLogsEnabled(true);
        return mc;
    }
}
