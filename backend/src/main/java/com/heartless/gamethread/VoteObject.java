package com.heartless.gamethread;

import com.heartless.model.Player;

/**
 * Records a single player's vote for one vote event.
 * Tracks who voted, who they selected, their text input, and whether they submitted themselves.
 */
public class VoteObject {

    public enum SubmissionStatus {
        /** Player pressed submit themselves. */
        SUBMITTED,
        /** Time ran out or event was skipped — game resolved the vote on their behalf. */
        RESOLVED
    }

    private final Player playerVoting;
    private Player playerSelected;
    private String textInput = "";
    private SubmissionStatus submissionStatus;

    public VoteObject(Player playerVoting) {
        this.playerVoting = playerVoting;
    }

    public Player getPlayerVoting() { return playerVoting; }

    public Player getPlayerSelected() { return playerSelected; }
    public void setPlayerSelected(Player playerSelected) { this.playerSelected = playerSelected; }

    public String getTextInput() { return textInput; }
    public void setTextInput(String textInput) { this.textInput = textInput != null ? textInput : ""; }

    public SubmissionStatus getSubmissionStatus() { return submissionStatus; }
    public void setSubmissionStatus(SubmissionStatus submissionStatus) {
        this.submissionStatus = submissionStatus;
    }
}

