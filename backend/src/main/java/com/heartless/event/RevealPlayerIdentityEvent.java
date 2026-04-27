package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.RoundObject;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reveals the identity (Traitor or Faithful) of the player who was just banished.
 * Runs at the end of every round, immediately after BanishRevealEvent.
 * A single player card is revealed with a 5-second lead-in for suspense.
 */
public class RevealPlayerIdentityEvent implements EventObjectInterface {

    private static final long LEAD_IN_MS = 5_000L;
    private static final long INTERVAL_MS = 4_000L;

    private final GameObject game;
    private Long startTime = null;
    /** The player who was banished this round, captured when the event starts. */
    private Player banishedPlayer = null;

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
        RoundObject round = game.getCurrentRound();
        banishedPlayer = (round != null) ? round.getPlayerBanished() : null;
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
        // Lead-in for suspense + one reveal interval + buffer after the card flips
        return LEAD_IN_MS + INTERVAL_MS + 5_000L;
    }

    @Override
    public long getEventEndTime() {
        if (startTime == null) return Long.MAX_VALUE;
        return startTime + getEventTime();
    }

    @Override
    public String getStartNotification() {
        return "The identity of the banished player is about to be revealed.";
    }

    @Override
    public String getInitialMessage() {
        return "Were they a Traitor? Find out now.";
    }

    @Override
    public boolean checkForNotifications() {
        return true;
    }

    /**
     * Returns a single timed reveal action for the banished player.
     * The reveal fires {@code LEAD_IN_MS} after the event starts, giving the
     * frontend time to build suspense before the card flips.
     */
    @Override
    public List<EventAction> getEvents() {
        if (startTime == null || banishedPlayer == null) return List.of();
        Map<String, Object> payload = new HashMap<>();
        payload.put("playerId", banishedPlayer.getId());
        payload.put("playerName", banishedPlayer.getName());
        payload.put("isTraitor", banishedPlayer.isTraitor());
        payload.put("role", banishedPlayer.isTraitor() ? "TRAITOR" : "FAITHFUL");
        payload.put("isDead", banishedPlayer.isDead());
        return List.of(new EventAction(payload, startTime + LEAD_IN_MS));
    }
}
