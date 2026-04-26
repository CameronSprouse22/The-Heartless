package com.heartless.event;

import com.heartless.config.GameConfigurations;
import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reveals the identity (Traitor or Faithful) of every player to all participants.
 * Players are revealed one at a time in a randomised order, spaced 4 seconds apart,
 * with a 5-second lead-in so the frontend can build suspense before the first card flips.
 */
public class RevealPlayerIdentityEvent implements EventObjectInterface {

    private static final long LEAD_IN_MS = 5_000L;
    private static final long INTERVAL_MS = 4_000L;

    private final GameObject game;
    private Long startTime = null;
    /** Shuffled snapshot of players taken when the event starts. */
    private List<Player> shuffledPlayers = null;

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
        // Snapshot and shuffle all players (including dead ones for a full identity reveal)
        List<Player> all = new ArrayList<>(game.getPlayerList());
        Collections.shuffle(all);
        shuffledPlayers = all;
    }

    @Override
    public boolean endConditonsMeet(GameObject gameObject) {
        // Ends only when the timer expires
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
        // Compute exactly how long the sequence takes, with a 5s buffer after the last reveal
        int count = (shuffledPlayers != null) ? shuffledPlayers.size() : game.getPlayerList().size();
        long computedDuration = LEAD_IN_MS + (long) count * INTERVAL_MS + 5_000L;
        return Math.max(computedDuration, GameConfigurations.REVEAL_PLAYER_IDENTITY_EVENT_DURATION_MS);
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
        return "The truth is coming. Watch closely as each player's identity is unveiled.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }

    /**
     * Returns a timed sequence of identity reveals — one EventAction per player.
     * Each action's object contains:
     * <ul>
     *   <li>{@code playerId}   — player's UUID</li>
     *   <li>{@code playerName} — player's display name</li>
     *   <li>{@code isTraitor}  — boolean</li>
     *   <li>{@code role}       — "TRAITOR" or "FAITHFUL"</li>
     *   <li>{@code isDead}     — boolean (for visual distinction)</li>
     * </ul>
     */
    @Override
    public List<EventAction> getEvents() {
        if (startTime == null || shuffledPlayers == null) return null;

        List<EventAction> actions = new ArrayList<>();
        for (int i = 0; i < shuffledPlayers.size(); i++) {
            Player p = shuffledPlayers.get(i);
            Map<String, Object> payload = new HashMap<>();
            payload.put("playerId", p.getId());
            payload.put("playerName", p.getName());
            payload.put("isTraitor", p.isTraitor());
            payload.put("role", p.isTraitor() ? "TRAITOR" : "FAITHFUL");
            payload.put("isDead", p.isDead());
            long executeTime = startTime + LEAD_IN_MS + (long) i * INTERVAL_MS;
            actions.add(new EventAction(payload, executeTime));
        }
        return actions;
    }
}
