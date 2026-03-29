package com.heartless.model;

import java.util.Map;
import java.util.Objects;

public class GameEventResult {

    private final String eventId;
    private final String gameCode;
    private final Map<String, PlayerSelection> finalSelections;
    private final ResolutionType resolutionType;
    private final long resolvedAt;

    public GameEventResult(String eventId, String gameCode, Map<String, PlayerSelection> finalSelections,
                           ResolutionType resolutionType, long resolvedAt) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(gameCode, "gameCode must not be null");
        Objects.requireNonNull(finalSelections, "finalSelections must not be null");
        Objects.requireNonNull(resolutionType, "resolutionType must not be null");
        this.eventId = eventId;
        this.gameCode = gameCode;
        this.finalSelections = Map.copyOf(finalSelections);
        this.resolutionType = resolutionType;
        this.resolvedAt = resolvedAt;
    }

    public String getEventId() { return eventId; }
    public String getGameCode() { return gameCode; }
    public Map<String, PlayerSelection> getFinalSelections() { return finalSelections; }
    public ResolutionType getResolutionType() { return resolutionType; }
    public long getResolvedAt() { return resolvedAt; }
}
