package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.GameObject;
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
     * Returns a list of scheduled reveal actions for this event.
     * Each {@link EventAction} has an actionObject (the data to reveal) and an
     * executeTime (epoch ms when it should be shown to the frontend).
     * Returns null for non-reveal events.
     */
    default java.util.List<EventAction> getEvents() { return null; }
}
