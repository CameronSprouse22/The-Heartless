package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class TestingEvent implements EventObjectInterface {

    private final GameObject game;
    private final long startTime = System.currentTimeMillis();

    public TestingEvent(GameObject game) {
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
        MenuControl mc = buildAllEnabledMenu();
        game.setMenuControl(mc);
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = buildAllEnabledMenu();
        return GameState.fromEvent(mc, game, this);
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

    @Override
    public long getEventTime() {
        return GameConfigurations.TESTING_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        return startTime + getEventTime();
    }
}
