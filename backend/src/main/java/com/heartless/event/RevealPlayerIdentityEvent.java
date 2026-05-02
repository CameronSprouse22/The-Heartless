package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reveals the identity (Traitor or Faithful) of all players.
 */
public class RevealPlayerIdentityEvent implements EventObjectInterface {

    private static final long LEAD_IN_MS = 5_000L;
    private static final long INTERVAL_MS = 4_000L;

    private final GameObject game;
    private Long startTime = null;

    public RevealPlayerIdentityEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onStart() {
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        return false;
    }

    @Override
    public ArrayList<UserSelectionsState> getUsersSelections() {
        return new ArrayList<>();
    }

    @Override
    public GameState getGameState() {
        MenuControl mc = new MenuControl();
        mc.setIdentityRevealEnabled(true);
        return GameState.fromEvent(mc, game, this);
    }

    @Override 
    public long getEventTime() {
        int playerCount = game.getPlayerList().size();
        // Lead-in + one interval per player + buffer after the last card flips
        return LEAD_IN_MS + ((long) playerCount * INTERVAL_MS) + 5_000L;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "The identities of all players are about to be revealed.";
    }

    @Override
    public String getInitialMessage() {
        return "Find out who the Traitors were.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }

    /**
     * Returns one timed reveal action per player, staggered by {@code INTERVAL_MS}.
     * The first reveal fires {@code LEAD_IN_MS} after the event starts.
     */
    @Override
    public List<EventAction> getEvents() {
        if (startTime == null) return List.of();

        List<Player> players = game.getPlayerList();
        List<EventAction> actions = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            Map<String, Object> payload = new HashMap<>();
            payload.put("playerId", player.getId());
            payload.put("playerName", player.getName());
            payload.put("isTraitor", player.isTraitor());
            payload.put("role", player.isTraitor() ? "TRAITOR" : "FAITHFUL");
            long fireAt = startTime + LEAD_IN_MS + ((long) i * INTERVAL_MS);
            actions.add(new EventAction(payload, fireAt));
        }
        return actions;
    }
}
