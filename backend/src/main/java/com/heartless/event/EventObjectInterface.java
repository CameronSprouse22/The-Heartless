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

    void execute();

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
}
