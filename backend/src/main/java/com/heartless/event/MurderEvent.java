package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class MurderEvent implements EventObjectInterface {

    private final GameObject game;

    public MurderEvent(GameObject game) {
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
        game.initSelectionStates(game.getPlayerList().stream()
                .map(p -> p.getId())
                .toList());
        MenuControl mc = new MenuControl();
        mc.setMurderVoteEnabled(true);
        mc.setTraitorChatEnabled(true);
        game.setMenuControl(mc);
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setMurderVoteEnabled(true);
        mc.setTraitorChatEnabled(true);
        return GameState.fromMenuControl(mc, game);
    }
}
