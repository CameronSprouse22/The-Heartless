package com.heartless.gamethread;

import com.heartless.event.EventObjectInterface;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of the current game state.
 * The event is always served directly; menuItems carry only chat/status flags.
 */
public class GameState {

    private final List<Map<String, Object>> menuItems;
    private final String currentEvent;
    private final String gameStatus;
    private final int round;
    private final String statusString;
    private final String eventType;
    private final long eventEndTime;
    private final String startNotification;

    public GameState(List<Map<String, Object>> menuItems,
                     String currentEvent,
                     String gameStatus,
                     int round,
                     String statusString,
                     String eventType,
                     long eventEndTime,
                     String startNotification) {
        this.menuItems = menuItems;
        this.currentEvent = currentEvent;
        this.gameStatus = gameStatus;
        this.round = round;
        this.statusString = statusString;
        this.eventType = eventType;
        this.eventEndTime = eventEndTime;
        this.startNotification = startNotification != null ? startNotification : "";
    }

    public List<Map<String, Object>> getMenuItems() { return menuItems; }
    public String getCurrentEvent() { return currentEvent; }
    public String getGameStatus() { return gameStatus; }
    public int getRound() { return round; }
    public String getStatusString() { return statusString; }
    public String getEventType() { return eventType; }
    public long getEventEndTime() { return eventEndTime; }
    public String getStartNotification() { return startNotification; }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("gameStatus", gameStatus);
        result.put("round", round);
        result.put("currentTask", currentEvent);
        result.put("statusString", statusString);
        result.put("menuItems", menuItems);
        result.put("eventType", eventType);
        result.put("eventEndTime", eventEndTime);
        result.put("startNotification", startNotification);
        // Flat flags for the frontend tab model
        result.put("allChatEnabled", menuItems.stream().anyMatch(m -> "all-chat".equals(m.get("id")) && Boolean.TRUE.equals(m.get("enabled"))));
        result.put("individualChatEnabled", menuItems.stream().anyMatch(m -> "individual-chat".equals(m.get("id")) && Boolean.TRUE.equals(m.get("enabled"))));
        result.put("traitorChatEnabled", menuItems.stream().anyMatch(m -> "traitor-chat".equals(m.get("id")) && Boolean.TRUE.equals(m.get("enabled"))));
        result.put("statusEnabled", menuItems.stream().anyMatch(m -> "status".equals(m.get("id")) && Boolean.TRUE.equals(m.get("enabled"))));
        return result;
    }

    /**
     * Build a GameState from a MenuControl and GameObject (no event timing).
     */
    public static GameState fromMenuControl(MenuControl mc, GameObject game) {
        List<Map<String, Object>> menuItems = buildMenuItems(mc);
        String statusString = deriveStatusString(game);
        return new GameState(menuItems, game.getCurrentTask(),
                game.getGameStatus().name(), game.getRound(), statusString, "", 0L, "");
    }

    /**
     * Build a GameState from a MenuControl, GameObject, and active event.
     * Populates eventType (human-readable) and eventEndTime (epoch ms).
     */
    public static GameState fromEvent(MenuControl mc, GameObject game, EventObjectInterface event) {
        List<Map<String, Object>> menuItems = buildMenuItems(mc);
        String statusString = deriveStatusString(game);
        String eventType = deriveEventType(event);
        return new GameState(menuItems, game.getCurrentTask(),
                game.getGameStatus().name(), game.getRound(), statusString,
                eventType, event.getEventEndTime(), event.getStartNotification());
    }

    private static String deriveEventType(EventObjectInterface event) {
        String simple = event.getClass().getSimpleName();
        // MiniGameEvent strips to "Mini" via the GameEvent$ rule — handle explicitly
        if (simple.equals("MiniGameEvent")) return "Mini Game";
        String name = simple
                .replaceAll("GameEvent$", "")
                .replaceAll("Event$", "");
        return name.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    /**
     * Build the menu items list from a MenuControl.
     * Only chat and status access flags are included.
     */
    public static List<Map<String, Object>> buildMenuItems(MenuControl mc) {
        List<Map<String, Object>> menuItems = new ArrayList<>();
        menuItems.add(Map.of("id", "all-chat", "label", "All Chat",
                "enabled", mc.isAllChatEnabled(), "visible", mc.isAllChatEnabled()));
        menuItems.add(Map.of("id", "individual-chat", "label", "Individual Chat",
                "enabled", mc.isIndividualChatEnabled(), "visible", mc.isIndividualChatEnabled()));
        menuItems.add(Map.of("id", "traitor-chat", "label", "Traitor Chat",
                "enabled", mc.isTraitorChatEnabled(), "visible", mc.isTraitorChatEnabled()));
        menuItems.add(Map.of("id", "status", "label", "Status",
                "enabled", mc.isStatusEnabled(), "visible", mc.isStatusEnabled()));
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
