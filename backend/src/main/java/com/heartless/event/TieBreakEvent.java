package com.heartless.event;

import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

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
        game.setMenuControl(setMenuItems());
    }

    @Override
    public MenuControl setMenuItems() {
        MenuControl mc = new MenuControl();
        mc.setBanishVoteEnabled(true);
        mc.setAllChatEnabled(true);
        return mc;
    }
}
