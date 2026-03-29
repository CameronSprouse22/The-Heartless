package com.heartless.model;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class GameEventState {

    private final GameEventConfig config;
    private final Map<String, PlayerSelection> selections;
    private volatile boolean resolved;
    private GameEventResult result;
    private transient ScheduledFuture<?> timeoutFuture;

    public GameEventState(GameEventConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        this.config = config;
        this.selections = new ConcurrentHashMap<>();
        this.resolved = false;
        this.result = null;
        this.timeoutFuture = null;
    }

    public GameEventConfig getConfig() { return config; }
    public Map<String, PlayerSelection> getSelections() { return selections; }
    public boolean isResolved() { return resolved; }
    public GameEventResult getResult() { return result; }
    public ScheduledFuture<?> getTimeoutFuture() { return timeoutFuture; }

    public void setTimeoutFuture(ScheduledFuture<?> timeoutFuture) {
        this.timeoutFuture = timeoutFuture;
    }

    public synchronized void resolve(GameEventResult result) {
        if (this.resolved) {
            throw new IllegalStateException("Event is already resolved");
        }
        this.resolved = true;
        this.result = result;
        if (this.timeoutFuture != null) {
            this.timeoutFuture.cancel(false);
        }
    }

    public void addPlayer(String playerId, String playerName) {
        selections.put(playerId, new PlayerSelection(playerId, playerName));
    }
}
