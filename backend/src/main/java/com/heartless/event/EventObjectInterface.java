package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

/**
 * Contract for all game events.
 */
public interface EventObjectInterface {

    boolean checkStartConditions();

    boolean endConditonsMeet(GameObject gameObject);

    /**
     * Returns the current selection state for all players in this event.
     * Each entry reflects the player's real-time selections, text input,
     * and whether they have pressed Submit.
     *
     * @return list of per-player selection states; empty if no vote event is active
     */
    ArrayList<UserSelectionsState> getUsersSelections();

    /**
     * Called once when an event is started. Use this for one-time initialisation
     * (e.g. initialising player selection states, recording start time).
     * Default is a no-op — override only when start-up work is needed.
     */
    default void onStart() {}

    /**
     * Returns a GameState snapshot for this event,
     * custom to each event implementation.
     */
    GameState getGameState();

    /**
     * Returns a player-specific GameState snapshot for this event.
     * Override in events that need to differentiate between player roles.
     * Defaults to the role-agnostic {@link #getGameState()}.
     */
    default GameState getGameState(Player player) {
        return getGameState();
    }

    /**
     * Returns the total configured duration for this event in milliseconds,
     * sourced from {@link com.heartless.config.GameConfigurations}.
     */
    long getEventTime();

    /**
     * Returns the absolute epoch millisecond timestamp when this event expires.
     * Computed as {@code startTime + getEventTime()}.
     */
    long getEventEndTime();

    /**
     * Returns the notification message broadcast to players when this event starts.
     */
    String getStartNotification();

    /**
     * Returns true if there are pending notifications for this event.
     */
    boolean checkForNotifications();

    /**
     * Returns the message shown to each player once when they first open the event page.
     * The message is dismissed per-player and will not appear again after dismissal.
     * Default returns an empty string — override in events that require an intro message.
     */
    String getInitialMessage();

    /**
     * Called when the event ends without full completion (time ran out or skipped).
     * Implementations should resolve any unsubmitted player votes or pending state.
     * Default is a no-op — override in events that manage vote state.
     */
    default void resolveEvent() {}

    /**
     * Optionally override the computed end time so a nested event shares its parent's countdown.
     * Default is a no-op — override in events that support timer sharing.
     */
    default void setOverrideEndTime(Long endTime) {}

    /**
     * Returns a list of scheduled reveal actions for this event.
     * Each {@link EventAction} has an actionObject (the data to reveal) and an
     * executeTime (epoch ms when it should be shown to the frontend).
     * Returns null for non-reveal events.
     */
    default java.util.List<EventAction> getEvents() { return null; }

    /**
     * When a vote results in a random tiebreak pick, returns the name of the randomly chosen player.
     * Returns null when no random pick occurred.
     */
    default String getRandomPickedName() { return null; }

    /**
     * The candidate names that were eligible for the random tiebreak pick.
     * Returns an empty list when no random pick occurred.
     */
    default java.util.List<String> getRandomPickCandidates() { return java.util.List.of(); }

    /**
     * Returns true if this event should also be shown to dead players.
     * Default is false — override in reveal events visible to all players.
     */
    default boolean isShowToDeadPlayers() { return false; }
}
