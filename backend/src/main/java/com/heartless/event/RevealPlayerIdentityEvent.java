package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.PlayerLifeStatusEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reveals the identity (Traitor or Faithful) of dead players only.
 * Dead players appear immediately (statically); the newly-banished player
 * (or a "Banish Blocked" entry) is revealed via a slot-machine animation
 * after a short lead-in.
 */
public class RevealPlayerIdentityEvent implements EventObjectInterface {

    private static final long LEAD_IN_MS = 5_000L;

    private final GameObject game;
    private Long startTime = null;
    /** ID of the player who was MARKED_FOR_BANISHMENT when this event started (null = banish blocked). */
    private String newlyBanishedPlayerId = null;

    public RevealPlayerIdentityEvent(GameObject game) {
        this.game = game;
    }

    @Override
    public boolean isShowToDeadPlayers() { return true; }

    @Override
    public boolean checkStartConditions() {
        startTime = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onStart() {
        // Capture which player was MARKED_FOR_BANISHMENT before transitioning them.
        // null means no one was banished this round → "Banish Blocked" will be shown.
        game.getPlayerList().stream()
                .filter(p -> p.getLifeStatus() == PlayerLifeStatusEnum.MARKED_FOR_BANISHMENT)
                .findFirst()
                .ifPresent(p -> newlyBanishedPlayerId = p.getId());

        // Transition MARKED_FOR_BANISHMENT → BANISHED.
        game.getPlayerList().stream()
                .filter(p -> p.getLifeStatus() == PlayerLifeStatusEnum.MARKED_FOR_BANISHMENT)
                .forEach(p -> p.setLifeStatus(PlayerLifeStatusEnum.BANISHED));
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
        return GameState.fromEvent(mc, game, this);
    }

    @Override 
    public long getEventTime() {
        // Lead-in for the slot machine + time for animation + buffer
        return LEAD_IN_MS + 10_000L;
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
     * Returns timed reveal actions.
     * Dead players (except the newly-banished one) fire immediately at {@code startTime}.
     * The newly-banished player (or a "Banish Blocked" sentinel) fires after {@code LEAD_IN_MS}
     * to give the frontend time to start a slot-machine animation.
     */
    @Override
    public List<EventAction> getEvents() {
        if (startTime == null) return List.of();

        List<Player> players = game.getPlayerList();
        List<EventAction> actions = new ArrayList<>();

        // Immediately reveal all dead players except the newly-banished one.
        for (Player player : players) {
            if (!player.isDead()) continue;
            if (player.getId().equals(newlyBanishedPlayerId)) continue;

            Map<String, Object> payload = new HashMap<>();
            payload.put("playerId", player.getId());
            payload.put("playerName", player.getName());
            payload.put("isTraitor", player.isTraitor());
            payload.put("role", player.isTraitor() ? "TRAITOR" : "FAITHFUL");
            payload.put("deathType", player.getLifeStatus().name());
            payload.put("isNewlyBanished", false);
            actions.add(new EventAction(payload, startTime));
        }

        // Slot-machine reveal: newly banished player or "Banish Blocked" sentinel.
        if (newlyBanishedPlayerId != null) {
            Player banished = players.stream()
                    .filter(p -> p.getId().equals(newlyBanishedPlayerId))
                    .findFirst().orElse(null);
            if (banished != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("playerId", banished.getId());
                payload.put("playerName", banished.getName());
                payload.put("isTraitor", banished.isTraitor());
                payload.put("role", banished.isTraitor() ? "TRAITOR" : "FAITHFUL");
                payload.put("deathType", "BANISHED");
                payload.put("isNewlyBanished", true);
                payload.put("isBanishBlocked", false);
                actions.add(new EventAction(payload, startTime + LEAD_IN_MS));
            }
        } else {
            // No player was banished this round (e.g. blocked by an item).
            Map<String, Object> payload = new HashMap<>();
            payload.put("playerId", "banish-blocked");
            payload.put("playerName", "Banish Blocked");
            payload.put("isTraitor", false);
            payload.put("role", "BLOCKED");
            payload.put("deathType", "BLOCKED");
            payload.put("isNewlyBanished", true);
            payload.put("isBanishBlocked", true);
            actions.add(new EventAction(payload, startTime + LEAD_IN_MS));
        }

        return actions;
    }
}
