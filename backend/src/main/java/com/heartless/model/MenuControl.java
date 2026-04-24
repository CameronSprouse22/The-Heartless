package com.heartless.model;

/**
 * Controls which menu items are enabled/visible during a game event.
 * Each event creates its own MenuControl to define what players can access.
 */
public class MenuControl {


    private boolean traitorChatEnabled;
    private boolean allChatEnabled;
    private boolean banishVoteEnabled;
    private boolean murderVoteEnabled;
    private boolean individualChatEnabled;
    private boolean actionsEnabled;
    private boolean gameLogsEnabled;
    private boolean gameOptionsEnabled;
    private boolean revealEnabled;

    public MenuControl() {
        // All disabled by default
        this.traitorChatEnabled = false;
        this.allChatEnabled = false;
        this.banishVoteEnabled = false;
        this.murderVoteEnabled = false;
        this.individualChatEnabled = false;
        this.actionsEnabled = false;
        this.gameLogsEnabled = false;
        this.gameOptionsEnabled = false;
        this.revealEnabled = false;
    }

    public boolean isTraitorChatEnabled() { return traitorChatEnabled; }
    public void setTraitorChatEnabled(boolean traitorChatEnabled) { this.traitorChatEnabled = traitorChatEnabled; }

    public boolean isAllChatEnabled() { return allChatEnabled; }
    public void setAllChatEnabled(boolean allChatEnabled) { this.allChatEnabled = allChatEnabled; }

    public boolean isBanishVoteEnabled() { return banishVoteEnabled; }
    public void setBanishVoteEnabled(boolean banishVoteEnabled) { this.banishVoteEnabled = banishVoteEnabled; }

    public boolean isMurderVoteEnabled() { return murderVoteEnabled; }
    public void setMurderVoteEnabled(boolean murderVoteEnabled) { this.murderVoteEnabled = murderVoteEnabled; }

    public boolean isIndividualChatEnabled() { return individualChatEnabled; }
    public void setIndividualChatEnabled(boolean individualChatEnabled) { this.individualChatEnabled = individualChatEnabled; }

    public boolean isActionsEnabled() { return actionsEnabled; }
    public void setActionsEnabled(boolean actionsEnabled) { this.actionsEnabled = actionsEnabled; }

    public boolean isGameLogsEnabled() { return gameLogsEnabled; }
    public void setGameLogsEnabled(boolean gameLogsEnabled) { this.gameLogsEnabled = gameLogsEnabled; }

    public boolean isGameOptionsEnabled() { return gameOptionsEnabled; }
    public void setGameOptionsEnabled(boolean gameOptionsEnabled) { this.gameOptionsEnabled = gameOptionsEnabled; }

    public boolean isRevealEnabled() { return revealEnabled; }
    public void setRevealEnabled(boolean revealEnabled) { this.revealEnabled = revealEnabled; }
}
