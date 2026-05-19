package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.PlayerStatusEnum;

import java.util.ArrayList;

/**
 * Scuttlebutt event — faithful players must privately message at least two
 * other players before they can mark themselves ready.
 * The event ends early once every active player has pressed Ready
 * (submitted via {@code markEventReady}), or when time runs out.
 */
public class ScuttlebuttEvent implements EventObjectInterface {

    private final GameObject game;
    private Long startTime = null;
    /** When set by a parent event, overrides the computed end time so the frontend countdown matches. */
    private Long overrideEndTime = null;

    public ScuttlebuttEvent(GameObject game) {
        this.game = game;
    }

    public void setOverrideEndTime(Long endTime) {
        this.overrideEndTime = endTime;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onStart() {
        game.initSelectionStates(
                game.getPlayerList().stream()
                        .filter(p -> !p.isDead() && p.getStatus() == PlayerStatusEnum.ACTIVE)
                        .map(Player::getId)
                        .toList()
        );
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
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
        mc.setScuttlebuttEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.SCUTTLEBUTT_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        if (overrideEndTime != null) return overrideEndTime;
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "Scuttlebutt has begun. Have a private word with your fellow players.";
    }

    @Override
    public String getInitialMessage() {
        return "Message at least two other players privately before marking yourself ready.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
