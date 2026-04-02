package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class TieBreakEvent implements EventObjectInterface {

    private final GameObject game;
    private final long startTime = System.currentTimeMillis();

    public TieBreakEvent(GameObject game) {
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
        mc.setBanishVoteEnabled(true);
        mc.setAllChatEnabled(true);
        game.setMenuControl(mc);
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setBanishVoteEnabled(true);
        mc.setAllChatEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.TIE_BREAK_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        return startTime + getEventTime();
    }
}
