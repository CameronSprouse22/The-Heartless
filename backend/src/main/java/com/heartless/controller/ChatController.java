package com.heartless.controller;

import com.heartless.service.ChatService;
import com.heartless.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final GameService gameService;

    public ChatController(ChatService chatService, GameService gameService) {
        this.chatService = chatService;
        this.gameService = gameService;
    }

    @PostMapping("/games/{gameCode}/chat/{channel}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable String gameCode,
            @PathVariable String channel,
            @RequestHeader("X-Player-Code") String playerCode,
            @RequestBody Map<String, String> body) {

        String text = body.get("text");
        if (text == null || text.isBlank() || text.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of("error", "text is required (1-500 chars)"));
        }

        String recipientId = body.get("recipientId");
        if ("individual".equals(channel) && (recipientId == null || recipientId.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "recipientId required for individual channel"));
        }

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }

        try {
            Map<String, Object> result = chatService.sendMessage(gameCode, playerId, channel, text, recipientId);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}/chat/{channel}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable String gameCode,
            @PathVariable String channel,
            @RequestHeader("X-Player-Code") String playerCode,
            @RequestParam(required = false) Long since) {

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }

        try {
            Map<String, Object> result = chatService.getMessages(gameCode, playerId, channel, since);
            return ResponseEntity.ok(result);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}/chat/counts")
    public ResponseEntity<Map<String, Object>> getMessageCounts(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {

        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }

        try {
            Map<String, Integer> counts = chatService.getMessageCounts(gameCode);
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("counts", counts);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
