package com.heartless.model;

/**
 * Controls which overlay tabs are accessible to a player during an event.
 * The event itself is always served directly; these flags control whether
 * All Chat, Individual Chat, Traitor Chat, and Status tabs are accessible.
 */
public class MenuControl {

    private boolean allChatEnabled;
    private boolean individualChatEnabled;
    private boolean traitorChatEnabled;
    private boolean statusEnabled;

    public MenuControl() {
        this.allChatEnabled = false;
        this.individualChatEnabled = false;
        this.traitorChatEnabled = false;
        this.statusEnabled = false;
    }

    public boolean isAllChatEnabled() { return allChatEnabled; }
    public void setAllChatEnabled(boolean allChatEnabled) { this.allChatEnabled = allChatEnabled; }

    public boolean isIndividualChatEnabled() { return individualChatEnabled; }
    public void setIndividualChatEnabled(boolean individualChatEnabled) { this.individualChatEnabled = individualChatEnabled; }

    public boolean isTraitorChatEnabled() { return traitorChatEnabled; }
    public void setTraitorChatEnabled(boolean traitorChatEnabled) { this.traitorChatEnabled = traitorChatEnabled; }

    public boolean isStatusEnabled() { return statusEnabled; }
    public void setStatusEnabled(boolean statusEnabled) { this.statusEnabled = statusEnabled; }
}
