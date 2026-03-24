package com.heartless.gamethread;

import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of the current game state,
 * containing the menu items and current event information.
 */
public class GameState {

    private final List<Map<String, Object>> menuItems;
    private final String currentEvent;
    private final String gameStatus;
    private final int round;
    private final String statusString;
 
    public GameState(List<Map<String, Object>> menuItems,
                     String currentEvent,
                     String gameStatus,
                     int round,
                     String statusString) {
        this.menuItems = menuItems;
        this.currentEvent = currentEvent;
        this.gameStatus = gameStatus;
        this.round = round;
        this.statusString = statusString;
    }

    public List<Map<String, Object>> getMenuItems() { return menuItems; }
    public String getCurrentEvent() { return currentEvent; }
    public String getGameStatus() { return gameStatus; }
    public int getRound() { return round; }
    public String getStatusString() { return statusString; }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("gameStatus", gameStatus);
        result.put("round", round);
        result.put("currentTask", currentEvent);
        result.put("statusString", statusString);
        result.put("menuItems", menuItems);
        return result;
    }

    /**
     * Build a GameState from a MenuControl and GameObject.
     */
    public static GameState fromMenuControl(MenuControl mc, GameObject game) {
        List<Map<String, Object>> menuItems = buildMenuItems(mc);
        String statusString = deriveStatusString(game);
        return new GameState(
                menuItems,
                game.getCurrentTask(),
                game.getGameStatus().name(),
                game.getRound(),
                statusString
        );
    }

    /**
     * Build the menu items list from a MenuControl.
     */
    public static List<Map<String, Object>> buildMenuItems(MenuControl mc) {
        List<Map<String, Object>> menuItems = new ArrayList<>();
        menuItems.add(Map.of("id", "traitor-chat", "label", "Traitor Chat",
                "enabled", mc.isTraitorChatEnabled()));
        menuItems.add(Map.of("id", "all-chat", "label", "All Chat",
                "enabled", mc.isAllChatEnabled()));
        menuItems.add(Map.of("id", "banish-vote", "label", "Banish Vote",
                "enabled", mc.isBanishVoteEnabled()));
        menuItems.add(Map.of("id", "murder-vote", "label", "Murder Vote",
                "enabled", mc.isMurderVoteEnabled()));
        menuItems.add(Map.of("id", "individual-chat", "label", "Individual Chat",
                "enabled", mc.isIndividualChatEnabled()));
        menuItems.add(Map.of("id", "dead-chat", "label", "Dead Chat",
                "enabled", false));
        menuItems.add(Map.of("id", "actions", "label", "Actions",
                "enabled", mc.isActionsEnabled()));
        menuItems.add(Map.of("id", "game-logs", "label", "Game Logs",
                "enabled", mc.isGameLogsEnabled()));
        menuItems.add(Map.of("id", "game-options", "label", "Game Options",
                "enabled", mc.isGameOptionsEnabled()));
        return menuItems;
    }

    private static String deriveStatusString(GameObject game) {
        switch (game.getGameStatus()) {
            case INIT: return "Lobby";
            case START: return "Round " + game.getRound();
            case END: return "Final Round";
            case OVER: return "Game Over";
            default: return game.getGameStatus().name();
        }
    }
}
