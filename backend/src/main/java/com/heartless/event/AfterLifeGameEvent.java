package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pseudo-event returned for dead players.
 * Enables only the Dead Chat and Game Logs menu items.
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
    public void execute() {
        // No-op: AfterLifeGameEvent is a read-only state, not a game phase.
    }

    @Override
    public GameState getGameState() {
        List<Map<String, Object>> menuItems = new ArrayList<>();
        menuItems.add(Map.of("id", "traitor-chat",   "label", "Traitor Chat",   "enabled", false, "visible", false));
        menuItems.add(Map.of("id", "all-chat",        "label", "All Chat",        "enabled", false, "visible", false));
        menuItems.add(Map.of("id", "banish-vote",     "label", "Banish Vote",     "enabled", false, "visible", false));
        menuItems.add(Map.of("id", "murder-vote",     "label", "Murder Vote",     "enabled", false, "visible", false));
        menuItems.add(Map.of("id", "individual-chat", "label", "Individual Chat", "enabled", false, "visible", false));
        menuItems.add(Map.of("id", "dead-chat",       "label", "Dead Chat",       "enabled", true,  "visible", true));
        menuItems.add(Map.of("id", "actions",         "label", "Actions",         "enabled", false, "visible", false));
        menuItems.add(Map.of("id", "game-logs",       "label", "Game Logs",       "enabled", true,  "visible", true));
        menuItems.add(Map.of("id", "game-options",    "label", "Game Options",    "enabled", false, "visible", false));
        return new GameState(menuItems, game.getCurrentTask(),
                game.getGameStatus().name(), game.getRound(), deriveStatus(),
                "After Life", getEventEndTime());
    }

    private String deriveStatus() {
        switch (game.getGameStatus()) {
            case INIT: return "Lobby";
            case START: return "Round " + game.getRound();
            case END:   return "Final Round";
            case OVER:  return "Game Over";
            default:    return game.getGameStatus().name();
        }
    }

    @Override
    public long getEventTime() {
        return GameConfigurations.AFTER_LIFE_EVENT_DURATION_MS;
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
    public boolean checkForNotifications() {
        return true;
    }
}
