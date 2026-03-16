package com.heartless.controller;

import com.heartless.service.GameService;
import com.heartless.service.VotingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoteController {

    private final VotingService votingService;
    private final GameService gameService;

    public VoteController(VotingService votingService, GameService gameService) {
        this.votingService = votingService;
        this.gameService = gameService;
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
