package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

public class LobbyEvent implements EventObjectInterface {

    private final GameObject game;
    private final long startTime = System.currentTimeMillis();

    public LobbyEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() { return true; }

    /**
     * Lobby ends when the VIP has triggered a start (forced or normal).
     * The ready-check enforcement is handled in the service layer before
     * transitionToStart() is called, so by the time this event runs, the
     * forced-start flag has already been set.
     */
    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        return gameObject.isLobbyVipForcedStart();
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>(game.getSelectionStateMap().values());
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setAllChatEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.LOBBY_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "Welcome to the lobby!";
    }

    @Override
    public String getInitialMessage() {
        return "Welcome! Press Ready when you are prepared to play.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
