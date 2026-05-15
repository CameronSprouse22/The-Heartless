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
    private boolean identityRevealEnabled;
    private boolean closeEnabled;
    private boolean roleRevealEnabled;
    private boolean sitRepEnabled;
    private boolean miniGameEnabled;
    /**
     * The page/tab the player should currently be viewing.
     * When non-null the frontend navigates directly to this panel,
     * bypassing the manual menu. Matches the panel IDs used in MenuPage
     * (e.g. "mini-game", "murder-vote", "banish-vote", "all-chat", etc.).
     */
    private String currentPage;

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
        this.identityRevealEnabled = false;
        this.closeEnabled = false;
        this.roleRevealEnabled = false;
        this.sitRepEnabled = false;
        this.miniGameEnabled = false;
        this.currentPage = null;
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

    public boolean isIdentityRevealEnabled() { return identityRevealEnabled; }
    public void setIdentityRevealEnabled(boolean identityRevealEnabled) { this.identityRevealEnabled = identityRevealEnabled; }

    public boolean isCloseEnabled() { return closeEnabled; }
    public void setCloseEnabled(boolean closeEnabled) { this.closeEnabled = closeEnabled; }

    public boolean isRoleRevealEnabled() { return roleRevealEnabled; }
    public void setRoleRevealEnabled(boolean roleRevealEnabled) { this.roleRevealEnabled = roleRevealEnabled; }

    public boolean isSitRepEnabled() { return sitRepEnabled; }
    public void setSitRepEnabled(boolean sitRepEnabled) { this.sitRepEnabled = sitRepEnabled; }

    public boolean isMiniGameEnabled() { return miniGameEnabled; }
    public void setMiniGameEnabled(boolean miniGameEnabled) { this.miniGameEnabled = miniGameEnabled; }

    public String getCurrentPage() { return currentPage; }
    public void setCurrentPage(String currentPage) { this.currentPage = currentPage; }
}
