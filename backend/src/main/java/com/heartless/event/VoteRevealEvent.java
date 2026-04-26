package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class VoteRevealEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;

    public VoteRevealEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) { return true; }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setRevealEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.VOTE_REVEAL_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "The votes are being revealed.";
    }

    @Override
    public String getInitialMessage() {
        return "The votes are being revealed. Prepare yourself.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
