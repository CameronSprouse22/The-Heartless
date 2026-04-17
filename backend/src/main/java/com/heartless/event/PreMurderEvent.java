package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class PreMurderEvent implements EventObjectInterface {

    private final GameObject game;
    private final long startTime = System.currentTimeMillis();

    public PreMurderEvent(GameObject game) {
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
        mc.setTraitorChatEnabled(true);
        game.setMenuControl(mc);
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setTraitorChatEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.PRE_MURDER_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "The traitors are gathering in the shadows.";
    }

    @Override
    public String getInitialMessage() {
        return "Night is falling. Traitors — discuss your plan before the murder phase begins.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
