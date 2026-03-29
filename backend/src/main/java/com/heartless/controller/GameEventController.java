package com.heartless.controller;

import com.heartless.model.GameEventConfig;
import com.heartless.model.GameEventState;
import com.heartless.model.PlayerSelection;
import com.heartless.service.GameEventService;
import com.heartless.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class GameEventController {

    private final GameEventService gameEventService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameEventController(GameEventService gameEventService, GameService gameService,
                               SimpMessagingTemplate messagingTemplate) {
        this.gameEventService = gameEventService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/games/{gameCode}/event")
    public ResponseEntity<Map<String, Object>> createEvent(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode,
            @RequestBody Map<String, Object> body) {

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }

        try {
            GameEventConfig config = buildConfig(gameCode, body);
            GameEventState state = gameEventService.createEvent(config);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("eventId", state.getConfig().getEventId());
            result.put("gameCode", gameCode);
            result.put("title", state.getConfig().getTitle());
            result.put("status", "ACTIVE");
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}/event")
    public ResponseEntity<Map<String, Object>> getEvent(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }

        GameEventState state = gameEventService.getEventState(gameCode);
        if (state == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No active event for this game"));
        }

        GameEventConfig cfg = state.getConfig();
        Map<String, Object> configMap = new LinkedHashMap<>();
        configMap.put("title", cfg.getTitle());
        configMap.put("prompt", cfg.getPrompt());
        configMap.put("listOfItems", cfg.getListOfItems());
        configMap.put("singleAnswer", cfg.isSingleAnswer());
        configMap.put("showOthersSelections", cfg.isShowOthersSelections());
        configMap.put("minNumberSelectedToSubmit", cfg.getMinNumberSelectedToSubmit());
        configMap.put("maxNumberSelectedToSubmit", cfg.getMaxNumberSelectedToSubmit());
        configMap.put("endTime", cfg.getEndTime());
        configMap.put("inputString", cfg.isInputString());
        configMap.put("playersMustAgree", cfg.isPlayersMustAgree());

        Map<String, Object> mySelection = null;
        PlayerSelection sel = state.getSelections().get(playerId);
        if (sel != null) {
            mySelection = new LinkedHashMap<>();
            mySelection.put("selectedItems", sel.getSelectedItems());
            mySelection.put("textInput", sel.getTextInput());
            mySelection.put("submissionStatus", sel.getSubmissionStatus().name());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", cfg.getEventId());
        result.put("config", configMap);
        result.put("mySelection", mySelection);
        result.put("resolved", state.isResolved());
        result.put("result", state.isResolved() ? buildResultMap(state) : null);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/games/{gameCode}/event/players")
    public ResponseEntity<Map<String, Object>> getPlayerSelections(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }

        GameEventState state = gameEventService.getEventState(gameCode);
        if (state == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "No active event"));
        }

        if (!state.getConfig().isShowOthersSelections()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "ShowOthersSelections is disabled"));
        }

        List<Map<String, Object>> players = new java.util.ArrayList<>();
        for (PlayerSelection sel : state.getSelections().values()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("playerId", sel.getPlayerId());
            p.put("playerName", sel.getPlayerName());
            p.put("selectedItems", sel.getSelectedItems());
            p.put("submissionStatus", sel.getSubmissionStatus().name());
            players.add(p);
        }

        return ResponseEntity.ok(Map.of("players", players));
    }

    // --- WebSocket handler (T018) ---

    @MessageMapping("/games/{gameCode}/event/select")
    public void handleSelect(@DestinationVariable String gameCode,
                             Map<String, Object> payload,
                             StompHeaderAccessor accessor) {
        String playerId = (String) accessor.getSessionAttributes().get("playerId");
        if (playerId == null) {
            sendError(accessor, "NOT_AUTHENTICATED", "Player not authenticated");
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> items = (List<String>) payload.get("selectedItems");
            String textInput = (String) payload.get("textInput");
            Set<String> selectedItems = items != null ? new LinkedHashSet<>(items) : Set.of();
            gameEventService.saveSelection(gameCode, playerId, selectedItems, textInput);
        } catch (IllegalArgumentException e) {
            sendError(accessor, "INVALID_SELECTION", e.getMessage());
        } catch (IllegalStateException e) {
            sendError(accessor, "EVENT_RESOLVED", e.getMessage());
        }
    }

    @MessageMapping("/games/{gameCode}/event/submit")
    public void handleSubmit(@DestinationVariable String gameCode,
                             StompHeaderAccessor accessor) {
        String playerId = (String) accessor.getSessionAttributes().get("playerId");
        if (playerId == null) {
            sendError(accessor, "NOT_AUTHENTICATED", "Player not authenticated");
            return;
        }

        try {
            gameEventService.submitSelections(gameCode, playerId);
        } catch (IllegalArgumentException e) {
            sendError(accessor, "SUBMIT_PRECONDITIONS_NOT_MET", e.getMessage());
        } catch (IllegalStateException e) {
            String code = e.getMessage() != null && e.getMessage().contains("resolved")
                    ? "EVENT_RESOLVED" : "SUBMIT_PRECONDITIONS_NOT_MET";
            sendError(accessor, code, e.getMessage());
        }
    }

    @MessageMapping("/games/{gameCode}/event/cancel-submit")
    public void handleCancelSubmit(@DestinationVariable String gameCode,
                                   StompHeaderAccessor accessor) {
        String playerId = (String) accessor.getSessionAttributes().get("playerId");
        if (playerId == null) {
            sendError(accessor, "NOT_AUTHENTICATED", "Player not authenticated");
            return;
        }

        try {
            gameEventService.cancelSubmit(gameCode, playerId);
        } catch (IllegalStateException e) {
            sendError(accessor, "CANCEL_NOT_ALLOWED", e.getMessage());
        }
    }

    private void sendError(StompHeaderAccessor accessor, String errorCode, String message) {
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("errorCode", errorCode);
        error.put("message", message);
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", error);
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private GameEventConfig buildConfig(String gameCode, Map<String, Object> body) {
        String title = (String) body.get("title");
        String prompt = (String) body.get("prompt");
        List<String> listOfItems = body.containsKey("listOfItems") ? (List<String>) body.get("listOfItems") : List.of();
        boolean singleAnswer = Boolean.TRUE.equals(body.get("singleAnswer"));
        boolean showOthersSelections = Boolean.TRUE.equals(body.get("showOthersSelections"));
        int min = body.containsKey("minNumberSelectedToSubmit") ? ((Number) body.get("minNumberSelectedToSubmit")).intValue() : 0;
        int max = body.containsKey("maxNumberSelectedToSubmit") ? ((Number) body.get("maxNumberSelectedToSubmit")).intValue() : 0;
        long endTime = body.containsKey("endTime") ? ((Number) body.get("endTime")).longValue() : 0;
        boolean inputString = Boolean.TRUE.equals(body.get("inputString"));
        boolean playersMustAgree = Boolean.TRUE.equals(body.get("playersMustAgree"));

        return new GameEventConfig(gameCode, title, prompt, listOfItems,
                singleAnswer, showOthersSelections, min, max, endTime, inputString, playersMustAgree);
    }

    private Map<String, Object> buildResultMap(GameEventState state) {
        var eventResult = state.getResult();
        if (eventResult == null) return null;

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("resolutionType", eventResult.getResolutionType().name());
        resultMap.put("resolvedAt", eventResult.getResolvedAt());

        Map<String, Object> finalSelections = new LinkedHashMap<>();
        for (var entry : eventResult.getFinalSelections().entrySet()) {
            PlayerSelection sel = entry.getValue();
            Map<String, Object> selMap = new LinkedHashMap<>();
            selMap.put("playerName", sel.getPlayerName());
            selMap.put("selectedItems", sel.getSelectedItems());
            selMap.put("submissionStatus", sel.getSubmissionStatus().name());
            finalSelections.put(entry.getKey(), selMap);
        }
        resultMap.put("finalSelections", finalSelections);
        return resultMap;
    }
}
