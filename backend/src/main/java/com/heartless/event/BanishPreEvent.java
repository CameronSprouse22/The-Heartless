package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.PlayerStatusEnum;

import java.util.ArrayList;

public class BanishPreEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;

    public BanishPreEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onStart() {
        // Initialise a selection state for every active player so Ready can be tracked
        game.initSelectionStates(
                game.getPlayerList().stream()
                        .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                        .map(Player::getId)
                        .toList()
        );
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        // End early when every active player has pressed Ready
        java.util.Map<String, UserSelectionsState> states = game.getSelectionStateMap();
        if (states == null || states.isEmpty()) return false;
        return states.values().stream().allMatch(UserSelectionsState::isSubmitPressed);
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setAllChatEnabled(true);
        mc.setStatusEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.BANISH_PRE_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "Discussion phase has begun. Decide who to banish.";
    }

    @Override
    public String getInitialMessage() {
        return "Talk it out. Who do you think should be banished?";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
