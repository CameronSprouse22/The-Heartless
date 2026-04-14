package com.heartless.gamethread;

import java.util.ArrayList;
import java.util.List;

/**
 * Records a single player's input/vote submission for one event.
 */
public class PlayerInput {

    public enum SubmissionStatus {
        /** Player pressed submit themselves. */
        SUBMITTED,
        /** Time ran out or event was skipped — game resolved the vote on their behalf. */
        RESOLVED
    }

    private final String playerId;
    private final String playerName;
    private List<String> selectedItems = new ArrayList<>();
    private String textInput = "";
    private SubmissionStatus submissionStatus;

    public PlayerInput(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }

    public List<String> getSelectedItems() { return new ArrayList<>(selectedItems); }
    public void setSelectedItems(List<String> items) {
        this.selectedItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public String getTextInput() { return textInput; }
    public void setTextInput(String textInput) { this.textInput = textInput != null ? textInput : ""; }

    public SubmissionStatus getSubmissionStatus() { return submissionStatus; }
    public void setSubmissionStatus(SubmissionStatus submissionStatus) {
        this.submissionStatus = submissionStatus;
    }
}
