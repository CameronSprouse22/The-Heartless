package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

/**
 * Pseudo-event returned for dead players.
 * Enables the Dead Chat tab via the allChatEnabled flag so the frontend
 * can route to DeadChatPage for After Life events.
 */
public class AfterLifeGameEvent implements EventObjectInterface {

    private final GameObject game;
    private final long startTime = System.currentTimeMillis();

    public AfterLifeGameEvent(GameObject game) {
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
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setAllChatEnabled(true);  // frontend routes to DeadChatPage for After Life events
        return GameState.fromEvent(mc, game, this);
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.AFTER_LIFE_GAME_EVENT_DURATION_MS;
    }

    @Override
    public long getEventEndTime() {
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "You have entered the afterlife.";
    }

    @Override
    public String getInitialMessage() {
        return "You have been eliminated, but the game is not over for you yet.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }
}
