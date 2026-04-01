package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class MiniGameEvent implements EventObjectInterface {

    private final GameObject game;

    public MiniGameEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() { return true; }

    @Override
    public boolean checkEndConditions() { return true; }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public void execute() {
        MenuControl mc = new MenuControl();
        mc.setActionsEnabled(true);
        mc.setAllChatEnabled(true);
        mc.setTraitorChatEnabled(true);
        mc.setIndividualChatEnabled(true);
        game.setMenuControl(mc);
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setActionsEnabled(true);
        mc.setAllChatEnabled(true);
        mc.setTraitorChatEnabled(true);
        mc.setIndividualChatEnabled(true);
        return GameState.fromMenuControl(mc, game);
    }
}
