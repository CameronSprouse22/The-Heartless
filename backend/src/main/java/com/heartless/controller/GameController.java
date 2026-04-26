package com.heartless.controller;

import com.heartless.event.BanishVoteEvent;
import com.heartless.event.EventAction;
import com.heartless.gamethread.GameState;
import com.heartless.gamethread.GameThread;
import com.heartless.model.GameObject;
import com.heartless.model.MenuControl;
import com.heartless.model.Player;
import com.heartless.model.RoundObject;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.PlayerStatusEnum;
import com.heartless.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/games/test-setup")
    public ResponseEntity<Map<String, Object>> createTestGame() {
        try {
            Map<String, Object> result = gameService.createTestGame();
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/games")
    public ResponseEntity<Map<String, Object>> createGame(@RequestBody Map<String, String> body) {
        String playerName = body.get("playerName");
        if (playerName == null || playerName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "playerName is required"));
        }
        try {
            Map<String, Object> result = gameService.createGame(playerName);
            return ResponseEntity.status(HttpStatus.CREATED).body(buildCreateGameResponse(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/games/{gameCode}/invite")
    public ResponseEntity<Map<String, Object>> invitePlayer(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String contact = body.get("contact");
        if (name == null || name.isBlank() || contact == null || contact.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name and contact are required"));
        }
        try {
            Map<String, Object> result = gameService.invitePlayer(gameCode, playerCode, name, contact);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}")
    public ResponseEntity<Map<String, Object>> getGameState(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        try {
            Map<String, Object> state = gameService.getGameState(gameCode, playerCode);
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/games/{gameCode}/start")
    public ResponseEntity<Map<String, Object>> startGame(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        try {
            Map<String, Object> result = gameService.startGame(gameCode, playerCode);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("VIP")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", msg));
            }
            if (msg != null && msg.contains("INIT")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Game already started"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    @GetMapping("/games/{gameCode}/resolve/{playerName}")
    public ResponseEntity<Map<String, Object>> resolvePlayer(
            @PathVariable String gameCode,
            @PathVariable String playerName) {
        try {
            Map<String, Object> result = gameService.resolvePlayerByName(gameCode, playerName);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}/menu")
    public ResponseEntity<Map<String, Object>> getMenu(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        try {
            GameObject game = gameService.getGameOrThrow(gameCode);
            Player player = game.findPlayerById(playerId);
            if (player == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Player not in this game"));
            }

            GameThread gameThread = gameService.getGameThread(gameCode);
            GameState gameState;
            if (gameThread != null) {
                gameState = gameThread.buildGameState(player);
            } else {
                gameState = GameState.fromMenuControl(new MenuControl(), game);
            }
            Map<String, Object> stateMap = gameState.toMap();
            stateMap.put("playersRemaining", game.getActivePlayerCount());
            if (gameThread != null && gameThread.getCurrentEvent() != null) {
                String initialMsg = gameThread.getCurrentEvent().getInitialMessage();
                boolean dismissed = game.hasPlayerDismissedInitialMessage(playerId);
                stateMap.put("initialMessage", initialMsg != null ? initialMsg : "");
                stateMap.put("hasDismissedInitialMessage", dismissed);
            }
            // Restore player's selection state so the frontend can re-populate UI after a refresh
            UserSelectionsState selState = game.getSelectionState(playerId);
            if (selState != null) {
                boolean isBanishVote = gameThread != null && gameThread.getCurrentEvent() instanceof BanishVoteEvent;
                stateMap.put("mySelection", Map.of(
                        "selectedItems", selState.getSelectedItems(),
                        "textFieldInput", isBanishVote ? "" : selState.getTextFieldInput(),
                        "submitPressed", selState.isSubmitPressed()
                ));
            }
            return ResponseEntity.ok(stateMap);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/games/{gameCode}/dismiss-message")
    public ResponseEntity<Map<String, Object>> dismissInitialMessage(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        try {
            GameObject game = gameService.getGameOrThrow(gameCode);
            if (game.findPlayerById(playerId) == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Player not in this game"));
            }
            game.dismissInitialMessage(playerId);
            return ResponseEntity.ok(Map.of("dismissed", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> buildCreateGameResponse(Map<String, Object> result) {
        Player player = (Player) result.get("player");
        Map<String, Object> response = new HashMap<>();
        response.put("gameId", result.get("gameId"));
        response.put("gameCode", result.get("gameCode"));
        response.put("playerCode", result.get("playerCode"));
        Map<String, Object> playerMap = new HashMap<>();
        playerMap.put("id", player.getId());
        playerMap.put("name", player.getName());
        playerMap.put("status", player.getStatus().name());
        response.put("player", playerMap);
        return response;
    }

    private String deriveStatusString(GameObject game) {
        switch (game.getGameStatus()) {
            case INIT: return "Lobby";
            case START: return "Round " + game.getRound();
            case END: return "Final Round";
            case OVER: return "Game Over";
            default: return game.getGameStatus().name();
        }
    }

    @GetMapping("/games/{gameCode}/round")
    public ResponseEntity<Map<String, Object>> getRoundInfo(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid player code"));
        }
        try {
            GameObject game = gameService.getGameOrThrow(gameCode);
            if (game.findPlayerById(playerId) == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Player not in this game"));
            }
            List<RoundObject> rounds = game.getRoundList();
            if (rounds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No active round"));
            }
            RoundObject current = rounds.get(rounds.size() - 1);
            Map<String, Object> result = new HashMap<>();
            result.put("roundNumber", current.getRoundNumber());
            result.put("phase", game.getCurrentTask());
            result.put("murderRevealed", current.isMurderRevealed());
            result.put("miniGamePlayed", current.isMiniGamePlayed());
            result.put("banishVoteResult", null);
            result.put("murderResult", null);
            if (current.getMiniGameWinner() != null) {
                result.put("miniGameWinner", Map.of(
                        "id", current.getMiniGameWinner().getId(),
                        "name", current.getMiniGameWinner().getName()));
            } else {
                result.put("miniGameWinner", null);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}/reveal")
    public ResponseEntity<Map<String, Object>> getRevealedVotes(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        try {
            GameObject game = gameService.getGameOrThrow(gameCode);
            if (game.findPlayerById(playerId) == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Player not in this game"));
            }
            GameThread thread = gameService.getGameThread(gameCode);
            if (thread == null || thread.getCurrentEvent() == null) {
                return ResponseEntity.ok(Map.of("revealedVotes", List.of(), "totalVotes", 0, "revealComplete", false));
            }
            List<EventAction> actions = thread.getCurrentEvent().getEvents();
            if (actions == null) {
                return ResponseEntity.ok(Map.of("revealedVotes", List.of(), "totalVotes", 0, "revealComplete", false));
            }
            long now = System.currentTimeMillis();
            List<Object> revealed = actions.stream()
                    .filter(a -> a.getExecuteTime() != null && a.getExecuteTime() <= now)
                    .map(EventAction::getActionObject)
                    .collect(java.util.stream.Collectors.toList());
            Map<String, Object> result = new HashMap<>();
            result.put("revealedVotes", revealed);
            result.put("totalVotes", actions.size());
            result.put("revealComplete", revealed.size() == actions.size());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
