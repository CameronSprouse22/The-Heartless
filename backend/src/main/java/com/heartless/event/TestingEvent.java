package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class TestingEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;

    public TestingEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() { return true; }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        return System.currentTimeMillis() >= getEventEndTime();
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public void execute() {
        if (startTime == null) startTime = System.currentTimeMillis();
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
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "Testing event started.";
    }

    @Override
    public String getInitialMessage() {
        return "A testing event is underway.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
