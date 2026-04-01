package com.heartless.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captures one player's real-time interaction state on a vote page during
 * a single active vote event. Scoped to the current event — a new instance
 * is created per player per vote round via
 * {@link GameObject#initSelectionStates(List)}.
 *
 * <p>Thread-safe: all mutating and reading methods are {@code synchronized}.
 */
public class UserSelectionsState {

    private final List<String> selectedItems = Collections.synchronizedList(new ArrayList<>());
    private String textFieldInput = "";
    private boolean submitPressed = false;

    // --- Selected Items ---

    /**
     * Adds a candidate player ID to the selection list if not already present.
     *
     * @param playerId the ID of the candidate to select
     */
    public synchronized void addSelectedItem(String playerId) {
        if (playerId != null && !selectedItems.contains(playerId)) {
            selectedItems.add(playerId);
        }
    }

    /**
     * Removes a candidate player ID from the selection list if present.
     *
     * @param playerId the ID of the candidate to deselect
     */
    public synchronized void removeSelectedItem(String playerId) {
        selectedItems.remove(playerId);
    }

    /**
     * Replaces the entire selection list with the provided items.
     * A {@code null} argument is treated as an empty list.
     *
     * @param items the new list of selected candidate IDs
     */
    public synchronized void setSelectedItems(List<String> items) {
        selectedItems.clear();
        if (items != null) {
            selectedItems.addAll(items);
        }
    }

    /**
     * Returns a defensive copy of the current selection list.
     *
     * @return new {@link ArrayList} containing the currently selected IDs
     */
    public synchronized ArrayList<String> getSelectedItems() {
        return new ArrayList<>(selectedItems);
    }

    // --- Text Field Input ---

    /**
     * Replaces the stored text field value.
     * A {@code null} argument is stored as an empty string.
     *
     * @param text the current value of the vote page text field
     */
    public synchronized void setTextFieldInput(String text) {
        this.textFieldInput = (text != null) ? text : "";
    }

    /**
     * Returns the current text field value. Never {@code null}.
     *
     * @return the stored text field value
     */
    public synchronized String getTextFieldInput() {
        return textFieldInput;
    }

    // --- Submit Pressed ---

    /**
     * Records that the player has pressed Submit.
     * Once set to {@code true}, subsequent calls with {@code false} are no-ops.
     *
     * @param value {@code true} to mark submission; {@code false} is ignored if already submitted
     */
    public synchronized void setSubmitPressed(boolean value) {
        if (this.submitPressed) return; // irreversible — once true, never revert
        this.submitPressed = value;
    }

    /**
     * Returns whether the player has pressed Submit for this event.
     *
     * @return {@code true} if the player has submitted; {@code false} otherwise
     */
    public synchronized boolean isSubmitPressed() {
        return submitPressed;
    }
}
