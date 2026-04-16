package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class RevealBanishVoteEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;

    public RevealBanishVoteEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() { return true; }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        return startTime != null && System.currentTimeMillis() >= getEventEndTime();
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public void execute() {
        if (startTime == null) startTime = System.currentTimeMillis();
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
        return GameConfigurations.REVEAL_BANISH_VOTE_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "The banish vote results are being revealed.";
    }

    @Override
    public String getInitialMessage() {
        return "The votes are in. See who the group has chosen to banish.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
