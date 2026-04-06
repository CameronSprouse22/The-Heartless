package com.heartless.service;

import com.heartless.model.*;
import com.heartless.push.PushNotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class GameEventService {

    private final GameStore gameStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;
    private final ConcurrentHashMap<String, GameEventState> activeEvents = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public GameEventService(GameStore gameStore, SimpMessagingTemplate messagingTemplate,
                            PushNotificationService pushNotificationService) {
        this.gameStore = gameStore;
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
    }

    public GameEventState createEvent(GameEventConfig config) {
        GameObject game = getGameOrThrow(config.getGameCode());

        if (activeEvents.containsKey(config.getGameCode())) {
            throw new IllegalStateException("An event is already active for game: " + config.getGameCode());
        }

        GameEventState state = new GameEventState(config);
        for (Player player : game.getPlayerList()) {
            state.addPlayer(player.getId(), player.getName());
        }

        activeEvents.put(config.getGameCode(), state);

        // Notify all subscribed players via Web Push
        if (pushNotificationService != null) {
            pushNotificationService.notifyGame(
                    config.getGameCode(),
                    config.getTitle(),
                    config.getPrompt() != null && !config.getPrompt().isBlank()
                            ? config.getPrompt() : config.getTitle() + " has started."
            );
        }

        // Schedule timeout if endTime is set
        if (config.getEndTime() > 0) {
            long delay = config.getEndTime() - System.currentTimeMillis();
            if (delay <= 0) {
                resolveOnTimeout(config.getGameCode());
            } else {
                ScheduledFuture<?> future = scheduler.schedule(
                        () -> resolveOnTimeout(config.getGameCode()),
                        delay, TimeUnit.MILLISECONDS);
                state.setTimeoutFuture(future);
            }
        }

        return state;
    }

    public void saveSelection(String gameCode, String playerId, Set<String> selectedItems, String textInput) {
        GameEventState state = getActiveEventOrThrow(gameCode);

        if (state.isResolved()) {
            throw new IllegalStateException("Event is already resolved");
        }

        GameEventConfig config = state.getConfig();

        // Validate items against listOfItems
        if (!config.getListOfItems().isEmpty()) {
            for (String item : selectedItems) {
                if (!config.getListOfItems().contains(item)) {
                    throw new IllegalArgumentException("Invalid selection: '" + item + "' is not in the list of items");
                }
            }
        }

        // Enforce singleAnswer
        if (config.isSingleAnswer() && selectedItems.size() > 1) {
            throw new IllegalArgumentException("Only one item can be selected when singleAnswer is true");
        }

        PlayerSelection selection = state.getSelections().get(playerId);
        if (selection == null) {
            throw new IllegalArgumentException("Player not found in event: " + playerId);
        }

        selection.select(selectedItems, textInput);

        // Broadcast if showOthersSelections is enabled
        if (config.isShowOthersSelections() && messagingTemplate != null) {
            Map<String, Object> update = new LinkedHashMap<>();
            update.put("type", "SELECTION_UPDATE");
            update.put("playerId", playerId);
            update.put("playerName", selection.getPlayerName());
            update.put("selectedItems", selection.getSelectedItems());
            update.put("submissionStatus", selection.getSubmissionStatus().name());
            messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/event", update);
        }
    }

    public void submitSelections(String gameCode, String playerId) {
        GameEventState state = getActiveEventOrThrow(gameCode);

        if (state.isResolved()) {
            throw new IllegalStateException("Event is already resolved");
        }

        PlayerSelection selection = state.getSelections().get(playerId);
        if (selection == null) {
            throw new IllegalArgumentException("Player not found in event: " + playerId);
        }

        GameEventConfig config = state.getConfig();

        // Validate selection count
        int count = selection.getSelectedItems().size();
        if (count < config.getMinNumberSelectedToSubmit()) {
            throw new IllegalStateException("At least " + config.getMinNumberSelectedToSubmit() + " item(s) must be selected");
        }
        if (config.getMaxNumberSelectedToSubmit() > 0 && count > config.getMaxNumberSelectedToSubmit()) {
            throw new IllegalStateException("At most " + config.getMaxNumberSelectedToSubmit() + " item(s) can be selected");
        }

        // Validate text input if required
        if (config.isInputString()) {
            String text = selection.getTextInput();
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Text input is required");
            }
        }

        selection.submit();

        // Broadcast SUBMISSION_UPDATE
        if (messagingTemplate != null) {
            Map<String, Object> update = new LinkedHashMap<>();
            update.put("type", "SUBMISSION_UPDATE");
            update.put("playerId", playerId);
            update.put("playerName", selection.getPlayerName());
            update.put("selectedItems", selection.getSelectedItems());
            update.put("submissionStatus", selection.getSubmissionStatus().name());
            messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/event", update);
        }

        // Check agreement if playersMustAgree and all submitted
        if (config.isPlayersMustAgree()) {
            checkAgreement(gameCode, state);
        }
    }

    public void cancelSubmit(String gameCode, String playerId) {
        GameEventState state = getActiveEventOrThrow(gameCode);

        if (state.isResolved()) {
            throw new IllegalStateException("Event is already resolved");
        }

        if (!state.getConfig().isPlayersMustAgree()) {
            throw new IllegalStateException("Cancel submit is only allowed when playersMustAgree is true");
        }

        PlayerSelection selection = state.getSelections().get(playerId);
        if (selection == null) {
            throw new IllegalArgumentException("Player not found in event: " + playerId);
        }

        selection.cancelSubmit();

        if (messagingTemplate != null) {
            Map<String, Object> update = new LinkedHashMap<>();
            update.put("type", "SUBMISSION_UPDATE");
            update.put("playerId", playerId);
            update.put("playerName", selection.getPlayerName());
            update.put("selectedItems", selection.getSelectedItems());
            update.put("submissionStatus", selection.getSubmissionStatus().name());
            messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/event", update);
        }
    }

    private void checkAgreement(String gameCode, GameEventState state) {
        Collection<PlayerSelection> allSelections = state.getSelections().values();

        // Check if all players have submitted
        boolean allSubmitted = allSelections.stream()
                .allMatch(s -> s.getSubmissionStatus() == SubmissionStatus.SUBMITTED);
        if (!allSubmitted) return;

        // Compare all selections
        Set<String> referenceSet = null;
        boolean agree = true;
        for (PlayerSelection sel : allSelections) {
            if (referenceSet == null) {
                referenceSet = sel.getSelectedItems();
            } else if (!sel.getSelectedItems().equals(referenceSet)) {
                agree = false;
                break;
            }
        }

        if (agree) {
            // Agreement reached — resolve the event
            Map<String, PlayerSelection> finalSelections = new LinkedHashMap<>(state.getSelections());
            GameEventResult result = new GameEventResult(
                    state.getConfig().getEventId(), gameCode, finalSelections,
                    ResolutionType.AGREEMENT_REACHED, System.currentTimeMillis());
            state.resolve(result);

            if (messagingTemplate != null) {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("type", "EVENT_RESOLVED");
                msg.put("resolutionType", "AGREEMENT_REACHED");
                msg.put("resolvedAt", result.getResolvedAt());
                msg.put("finalSelections", buildFinalSelectionsMap(finalSelections));
                messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/event", msg);
            }
        } else {
            // Disagreement — reset all submissions
            for (PlayerSelection sel : allSelections) {
                sel.resetSubmission();
            }

            if (messagingTemplate != null) {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("type", "DISAGREEMENT");
                msg.put("message", "Players submitted different selections. Please re-select and try again.");
                messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/event", msg);
            }
        }
    }

    private Map<String, Object> buildFinalSelectionsMap(Map<String, PlayerSelection> selections) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (var entry : selections.entrySet()) {
            Map<String, Object> selMap = new LinkedHashMap<>();
            selMap.put("playerName", entry.getValue().getPlayerName());
            selMap.put("selectedItems", entry.getValue().getSelectedItems());
            selMap.put("submissionStatus", entry.getValue().getSubmissionStatus().name());
            map.put(entry.getKey(), selMap);
        }
        return map;
    }

    public GameEventState getEventState(String gameCode) {
        return activeEvents.get(gameCode);
    }

    public Map<String, PlayerSelection> getPlayerSelections(String gameCode) {
        GameEventState state = getActiveEventOrThrow(gameCode);
        return Map.copyOf(state.getSelections());
    }

    public void resolveOnTimeout(String gameCode) {
        GameEventState state = getActiveEventOrThrow(gameCode);

        if (state.isResolved()) {
            return; // already resolved normally
        }

        Map<String, PlayerSelection> finalSelections = new LinkedHashMap<>(state.getSelections());
        GameEventResult result = new GameEventResult(
                state.getConfig().getEventId(), gameCode, finalSelections,
                ResolutionType.TIMEOUT, System.currentTimeMillis());
        state.resolve(result);

        if (messagingTemplate != null) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "EVENT_RESOLVED");
            msg.put("resolutionType", "TIMEOUT");
            msg.put("resolvedAt", result.getResolvedAt());
            msg.put("finalSelections", buildFinalSelectionsMap(finalSelections));
            messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/event", msg);
        }
    }

    private GameObject getGameOrThrow(String gameCode) {
        GameObject game = gameStore.getGame(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameCode);
        }
        return game;
    }

    private GameEventState getActiveEventOrThrow(String gameCode) {
        GameEventState state = activeEvents.get(gameCode);
        if (state == null) {
            throw new IllegalArgumentException("No active event for game: " + gameCode);
        }
        return state;
    }
}
