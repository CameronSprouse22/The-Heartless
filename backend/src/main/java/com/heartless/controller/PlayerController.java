package com.heartless.controller;

import com.heartless.service.PlayerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/join/{gameCode}")
    public ResponseEntity<Map<String, Object>> getJoinInfo(@PathVariable String gameCode) {
        try {
            return ResponseEntity.ok(playerService.getJoinInfo(gameCode));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/join/{gameCode}")
    public ResponseEntity<Map<String, Object>> joinGame(
            @PathVariable String gameCode,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String contact = body.get("contact");
        if (name == null || name.isBlank() || contact == null || contact.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name and contact are required"));
        }
        try {
            return ResponseEntity.ok(playerService.joinGame(gameCode, name, contact));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}/me")
    public ResponseEntity<Map<String, Object>> getPlayerInfo(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        try {
            return ResponseEntity.ok(playerService.getPlayerInfo(gameCode, playerCode));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
}
