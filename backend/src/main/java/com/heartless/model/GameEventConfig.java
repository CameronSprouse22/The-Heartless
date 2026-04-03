package com.heartless.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GameEventConfig {

    private final String eventId;
    private final String gameCode;
    private final String title;
    private final String prompt;
    private final List<String> listOfItems;
    private final boolean singleAnswer;
    private final boolean showOthersSelections;
    private final int minNumberSelectedToSubmit;
    private final int maxNumberSelectedToSubmit;
    private final long endTime;
    private final boolean inputString;
    private final boolean playersMustAgree;

    public GameEventConfig(String gameCode, String title, String prompt, List<String> listOfItems,
                           boolean singleAnswer, boolean showOthersSelections,
                           int minNumberSelectedToSubmit, int maxNumberSelectedToSubmit,
                           long endTime, boolean inputString, boolean playersMustAgree) {
        Objects.requireNonNull(gameCode, "gameCode must not be null");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        Objects.requireNonNull(listOfItems, "listOfItems must not be null");
        if (!listOfItems.isEmpty() && minNumberSelectedToSubmit > listOfItems.size()) {
            throw new IllegalArgumentException(
                    "minNumberSelectedToSubmit (" + minNumberSelectedToSubmit + ") exceeds number of items (" + listOfItems.size() + ")");
        }
        if (maxNumberSelectedToSubmit > 0 && maxNumberSelectedToSubmit < minNumberSelectedToSubmit) {
            throw new IllegalArgumentException(
                    "maxNumberSelectedToSubmit (" + maxNumberSelectedToSubmit + ") is less than minNumberSelectedToSubmit (" + minNumberSelectedToSubmit + ")");
        }

        this.eventId = "evt-" + UUID.randomUUID().toString().substring(0, 8);
        this.gameCode = gameCode;
        this.title = title;
        this.prompt = prompt;
        this.listOfItems = List.copyOf(listOfItems);
        this.singleAnswer = singleAnswer;
        this.showOthersSelections = showOthersSelections;
        this.minNumberSelectedToSubmit = minNumberSelectedToSubmit;
        this.maxNumberSelectedToSubmit = maxNumberSelectedToSubmit;
        this.endTime = endTime;
        this.inputString = inputString;
        this.playersMustAgree = playersMustAgree;
    }
git 
    public String getEventId() { return eventId; }
    public String getGameCode() { return gameCode; }
    public String getTitle() { return title; }
    public String getPrompt() { return prompt; }
    public List<String> getListOfItems() { return listOfItems; }
    public boolean isSingleAnswer() { return singleAnswer; }
    public boolean isShowOthersSelections() { return showOthersSelections; }
    public int getMinNumberSelectedToSubmit() { return minNumberSelectedToSubmit; }
    public int getMaxNumberSelectedToSubmit() { return maxNumberSelectedToSubmit; }
    public long getEndTime() { return endTime; }
    public boolean isInputString() { return inputString; }
    public boolean isPlayersMustAgree() { return playersMustAgree; }
}
