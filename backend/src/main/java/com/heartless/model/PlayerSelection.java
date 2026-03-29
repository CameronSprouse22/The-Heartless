package com.heartless.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PlayerSelection {

    private final String playerId;
    private final String playerName;
    private Set<String> selectedItems;
    private String textInput;
    private SubmissionStatus submissionStatus;

    public PlayerSelection(String playerId, String playerName) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(playerName, "playerName must not be null");
        this.playerId = playerId;
        this.playerName = playerName;
        this.selectedItems = new HashSet<>();
        this.textInput = null;
        this.submissionStatus = SubmissionStatus.NONE;
    }

    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public Set<String> getSelectedItems() { return Set.copyOf(selectedItems); }
    public String getTextInput() { return textInput; }
    public SubmissionStatus getSubmissionStatus() { return submissionStatus; }

    public void select(Set<String> items, String textInput) {
        Objects.requireNonNull(items, "items must not be null");
        this.selectedItems = new HashSet<>(items);
        this.textInput = textInput;
        if (this.submissionStatus == SubmissionStatus.SUBMITTED) {
            this.submissionStatus = SubmissionStatus.SELECTED;
        } else if (!items.isEmpty()) {
            this.submissionStatus = SubmissionStatus.SELECTED;
        }
    }

    public void submit() {
        if (this.submissionStatus != SubmissionStatus.SELECTED) {
            throw new IllegalStateException("Cannot submit: must be in SELECTED state, currently " + submissionStatus);
        }
        this.submissionStatus = SubmissionStatus.SUBMITTED;
    }

    public void cancelSubmit() {
        if (this.submissionStatus != SubmissionStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot cancel submit: must be in SUBMITTED state, currently " + submissionStatus);
        }
        this.submissionStatus = SubmissionStatus.SELECTED;
    }

    public void resetSubmission() {
        this.submissionStatus = SubmissionStatus.SELECTED;
    }
}
