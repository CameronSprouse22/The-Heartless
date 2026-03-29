package com.heartless.controller;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.service.GameService;
import com.heartless.service.GameStore;
import com.heartless.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoteController {

    private final VotingService votingService;
    private final GameService gameService;
    private final GameStore gameStore;
    private final SimpMessagingTemplate messagingTemplate;

    public VoteController(VotingService votingService, GameService gameService,
                          GameStore gameStore, SimpMessagingTemplate messagingTemplate) {
        this.votingService = votingService;
        this.gameService = gameService;
        this.gameStore = gameStore;
        this.messagingTemplate = messagingTemplate;
    }

    /** Broadcast live (pre-submit) selection to other traitors on the murder vote page. */
    @MessageMapping("/games/{gameCode}/murder-selection")
    public void handleMurderSelection(@DestinationVariable String gameCode,
                                      Map<String, Object> payload,
                                      StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;
        String playerId = (String) attrs.get("playerId");
        if (playerId == null) return;

        GameObject game = gameStore.getGame(gameCode);
        if (game == null) return;
        Player player = game.findPlayerById(playerId);
        if (player == null) return;

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "MURDER_SELECTION_UPDATE");
        broadcast.put("voterId", playerId);
        broadcast.put("voterName", player.getName());
        broadcast.put("targetIds", payload.getOrDefault("targetIds", List.of()));
        broadcast.put("targetNames", payload.getOrDefault("targetNames", List.of()));
        messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/murder-vote", broadcast);
    }

    @GetMapping("/games/{gameCode}/vote/banish")
    public ResponseEntity<Map<String, Object>> getBanishCandidates(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        try {
            var result = votingService.getBanishCandidates(gameCode, playerId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/games/{gameCode}/vote/banish")
    public ResponseEntity<Map<String, Object>> castBanishVote(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode,
            @RequestBody Map<String, String> body) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        String targetPlayerId = body.get("targetPlayerId");
        if (targetPlayerId == null || targetPlayerId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetPlayerId is required"));
        }
        try {
            var result = votingService.castBanishVote(gameCode, playerId, targetPlayerId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}/vote/murder")
    public ResponseEntity<Map<String, Object>> getMurderCandidates(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        try {
            var result = votingService.getMurderCandidates(gameCode, playerId);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/games/{gameCode}/vote/murder")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> castMurderVote(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode,
            @RequestBody Map<String, Object> body) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        Object targetIds = body.get("targetPlayerIds");
        if (!(targetIds instanceof List)) {
            return ResponseEntity.badRequest().body(Map.of("error", "targetPlayerIds array is required"));
        }
        try {
            var result = votingService.castMurderVote(gameCode, playerId, (List<String>) targetIds);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
