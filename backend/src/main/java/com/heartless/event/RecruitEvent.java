package com.heartless.event;

import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

/**
 * Recruit event — traitors may attempt to recruit a faithful player.
 */
public class RecruitEvent implements EventObjectInterface {

    private final GameObject game;

    public RecruitEvent(GameObject game) {
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
        mc.setTraitorChatEnabled(true);
        mc.setIndividualChatEnabled(true);
        mc.setActionsEnabled(true);
        return mc;
    }
}
