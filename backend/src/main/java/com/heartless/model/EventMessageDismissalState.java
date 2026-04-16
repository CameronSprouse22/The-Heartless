package com.heartless.model;

public class EventMessageDismissalState {

    private final Player player;
    private boolean dismissed;

    public EventMessageDismissalState(Player player) {
        this.player = player;
        this.dismissed = false;
    }

    public Player getPlayer() { return player; }

    public boolean isDismissed() { return dismissed; }

    public void dismiss() { this.dismissed = true; }
}
