package com.heartless.event;

import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

/**
 * Revel murder event — reveals the murder outcome to all players.
 */
public class RevelMurderEvent implements EventObjectInterface {

    private final GameObject game;

    public RevelMurderEvent(GameObject game) {
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
        mc.setGameLogsEnabled(true);
        return mc;
    }
}
