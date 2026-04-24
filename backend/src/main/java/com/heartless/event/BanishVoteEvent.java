package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class BanishVoteEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;

    public BanishVoteEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) { return false; }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public void onStart() {
        if (game.getSelectionStateMap().isEmpty()) {
            game.initSelectionStates(game.getPlayerList().stream()
                    .map(p -> p.getId())
                    .toList());
        }
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setBanishVoteEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.BANISH_VOTE_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "Voting has begun!";
    }

    @Override
    public String getInitialMessage() {
        return "It's time to vote. Choose wisely — one player will be banished.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
