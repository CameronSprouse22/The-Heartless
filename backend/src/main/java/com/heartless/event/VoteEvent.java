package com.heartless.event;

import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

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
        game.setMenuControl(setMenuItems());
    }

    @Override
    public MenuControl setMenuItems() {
        MenuControl mc = new MenuControl();
        mc.setBanishVoteEnabled(true);
        mc.setAllChatEnabled(true);
        mc.setTraitorChatEnabled(true);
        return mc;
    }
}
