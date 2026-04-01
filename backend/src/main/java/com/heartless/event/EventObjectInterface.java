package com.heartless.event;

import com.heartless.gamethread.GameState;
import com.heartless.model.UserSelectionsState;

import java.util.ArrayList;

/**
 * Contract for all game events.
 */
public interface EventObjectInterface {

    boolean checkStartConditions();

    boolean checkEndConditions();

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
}
