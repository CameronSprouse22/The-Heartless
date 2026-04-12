package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class RevelMurderEvent implements EventObjectInterface {

    private final GameObject game;
    private final long startTime = System.currentTimeMillis();

    public RevelMurderEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() { return true; }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) { return true; }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public void execute() {
        MenuControl mc = new MenuControl();
        mc.setAllChatEnabled(true);
        mc.setGameLogsEnabled(true);
        game.setMenuControl(mc);
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setAllChatEnabled(true);
        mc.setGameLogsEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.REVEL_MURDER_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "The murder is being revealed.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
