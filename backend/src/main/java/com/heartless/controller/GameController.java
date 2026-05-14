package com.heartless.controller;

import com.heartless.config.GameConfigurations;
import com.heartless.event.BanishVoteEvent;
import com.heartless.event.EventAction;
import com.heartless.event.MiniGameEvent;
import com.heartless.event.MurderVoteEvent;
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
                String initialMsg = GameConfigurations.SHOW_EVENT_DIALOGS
                        ? gameThread.getCurrentEvent().getInitialMessage() : "";
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

    /**
     * Mark the requesting player as ready in the lobby.
     * All players (including VIP) must press Ready before "Start Game" is allowed.
     */
    @PostMapping("/games/{gameCode}/lobby/ready")
    public ResponseEntity<Map<String, Object>> markLobbyReady(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        try {
            Map<String, Object> result = gameService.markLobbyReady(gameCode, playerCode);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Toggle a player back to Not Ready in the lobby.
     */
    @PostMapping("/games/{gameCode}/lobby/unready")
    public ResponseEntity<Map<String, Object>> markLobbyNotReady(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        try {
            Map<String, Object> result = gameService.markLobbyNotReady(gameCode, playerCode);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * VIP starts the game even if some invited players have not responded.
     * Starts with however many players are currently ACTIVE.
     * Requires at least MIN_PLAYERS_TO_START players to be in the invite list.
     */
    @PostMapping("/games/{gameCode}/start-with-unconfirmed")
    public ResponseEntity<Map<String, Object>> startGameWithUnconfirmed(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        try {
            Map<String, Object> result = gameService.startGameWithUnconfirmed(gameCode, playerCode);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("VIP")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    /**
     * Returns the current situation report for a game:
     * round, all players with life statuses, murdered/banished/banished-traitor lists.
     */
    @GetMapping("/games/{gameCode}/sitrep")
    public ResponseEntity<Map<String, Object>> getSitRep(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        try {
            Map<String, Object> result = gameService.getSitRep(gameCode, playerCode);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Records the calling player's acknowledgement of the situation report.
     * When every active player has confirmed, the event ends early.
     */
    @PostMapping("/games/{gameCode}/sitrep/confirm")
    public ResponseEntity<Map<String, Object>> confirmSitRep(
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
            UserSelectionsState state = game.getSelectionState(playerId);
            if (state == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No sitrep active"));
            }
            state.setSubmitPressed(true);
            long confirmedCount = game.getSelectionStateMap().values().stream()
                    .filter(UserSelectionsState::isSubmitPressed).count();
            long totalCount = game.getSelectionStateMap().size();
            return ResponseEntity.ok(Map.of("confirmed", true, "confirmedCount", confirmedCount, "totalCount", totalCount));
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
            boolean complete = revealed.size() == actions.size();
            result.put("revealComplete", complete);
            if (complete) {
                String randomPickedName = thread.getCurrentEvent().getRandomPickedName();
                if (randomPickedName != null) {
                    result.put("randomPickedName", randomPickedName);
                    result.put("randomPickCandidates", thread.getCurrentEvent().getRandomPickCandidates());
                }
            }
            // Confirmation progress (mirrors SitRep behaviour)
            List<Player> activePlayers = game.getPlayerList().stream()
                    .filter(p -> !p.isDead() && p.getStatus() == com.heartless.model.enums.PlayerStatusEnum.ACTIVE)
                    .collect(java.util.stream.Collectors.toList());
            long confirmedCount = activePlayers.stream()
                    .filter(p -> {
                        UserSelectionsState st = game.getSelectionState(p.getId());
                        return st != null && st.isSubmitPressed();
                    }).count();
            UserSelectionsState myState = game.getSelectionState(playerId);
            result.put("myConfirmed", myState != null && myState.isSubmitPressed());
            result.put("confirmedCount", (int) confirmedCount);
            result.put("requiredCount", activePlayers.size());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/games/{gameCode}/role-reveal")
    public ResponseEntity<Map<String, Object>> getRoleReveal(
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
            List<Player> activePlayers = game.getPlayerList().stream()
                    .filter(p -> !p.isDead() && p.getStatus() == com.heartless.model.enums.PlayerStatusEnum.ACTIVE)
                    .collect(java.util.stream.Collectors.toList());
            long confirmedCount = activePlayers.stream()
                    .filter(p -> {
                        UserSelectionsState state = game.getSelectionState(p.getId());
                        return state != null && state.isSubmitPressed();
                    })
                    .count();
            UserSelectionsState myState = game.getSelectionState(playerId);
            boolean myConfirmed = myState != null && myState.isSubmitPressed();
            Map<String, Object> result = new HashMap<>();
            result.put("isTraitor", player.isTraitor());
            result.put("myRole", player.isTraitor() ? "TRAITOR" : "FAITHFUL");
            result.put("confirmedCount", (int) confirmedCount);
            result.put("totalPlayers", activePlayers.size());
            result.put("allConfirmed", !activePlayers.isEmpty() && confirmedCount == activePlayers.size());
            result.put("myConfirmed", myConfirmed);
            GameThread thread = gameService.getGameThread(gameCode);
            if (thread != null && thread.getCurrentEvent() != null) {
                result.put("eventEndTime", thread.getCurrentEvent().getEventEndTime());
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/games/{gameCode}/role-reveal/confirm")
    public ResponseEntity<Map<String, Object>> confirmRoleReveal(
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
            UserSelectionsState state = game.getSelectionState(playerId);
            if (state == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No role reveal active"));
            }
            state.setSubmitPressed(true);
            return ResponseEntity.ok(Map.of("confirmed", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Mini Game ────────────────────────────────────────────────────────────

    /**
     * Returns the current question for this player (derived from how many they
     * have already answered), plus aggregate progress and role flag.
     */
    @GetMapping("/games/{gameCode}/mini-game")
    public ResponseEntity<Map<String, Object>> getMiniGame(
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
            MiniGameEvent miniGame = resolveMiniGameEvent(gameCode);
            if (miniGame == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No mini game active"));
            }
            Map<String, Object> status = new java.util.LinkedHashMap<>(miniGame.buildStatusMap(playerId));
            status.put("isTraitor", player.isTraitor());
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Submit the calling player's answer to their current question.
     * Body: { "selectedOption": "E" }
     * Returns: { accepted, correct, questionIndex, done, nextQuestionIndex }
     */
    @PostMapping("/games/{gameCode}/mini-game/answer")
    public ResponseEntity<Map<String, Object>> submitMiniGameAnswer(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode,
            @RequestBody Map<String, String> body) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        String selectedOption = body.get("selectedOption");
        if (selectedOption == null || selectedOption.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "selectedOption is required"));
        }
        try {
            GameObject game = gameService.getGameOrThrow(gameCode);
            if (game.findPlayerById(playerId) == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Player not in this game"));
            }
            MiniGameEvent miniGame = resolveMiniGameEvent(gameCode);
            if (miniGame == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No mini game active"));
            }
            Map<String, Object> result = miniGame.submitAnswer(playerId, selectedOption);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Answer not accepted"));
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Resolves the active {@link MiniGameEvent} from the current game event chain.
     * Returns {@code null} if there is no active mini game.
     */
    private MiniGameEvent resolveMiniGameEvent(String gameCode) {
        GameThread thread = gameService.getGameThread(gameCode);
        if (thread == null) return null;
        var evt = thread.getCurrentEvent();
        if (evt instanceof MiniGameEvent mg) return mg;
        if (evt instanceof MurderVoteEvent mv && !mv.isMiniGameDone()) return mv.getMiniGameEvent();
        return null;
    }

    @GetMapping("/games/{gameCode}/identity-reveal")
    public ResponseEntity<Map<String, Object>> getIdentityReveal(
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
                return ResponseEntity.ok(Map.of("revealedPlayers", List.of(), "totalPlayers", 0, "revealComplete", false));
            }
            List<EventAction> actions = thread.getCurrentEvent().getEvents();
            if (actions == null) {
                return ResponseEntity.ok(Map.of("revealedPlayers", List.of(), "totalPlayers", 0, "revealComplete", false));
            }
            long now = System.currentTimeMillis();
            List<Object> revealed = actions.stream()
                    .filter(a -> a.getExecuteTime() != null && a.getExecuteTime() <= now)
                    .map(EventAction::getActionObject)
                    .collect(java.util.stream.Collectors.toList());
            // Mark each revealed player's hasBeenRevealed flag
            for (Object obj : revealed) {
                if (obj instanceof java.util.Map<?, ?> payload) {
                    Object pid = payload.get("playerId");
                    if (pid instanceof String playerIdStr) {
                        Player revealedPlayer = game.findPlayerById(playerIdStr);
                        if (revealedPlayer != null && !revealedPlayer.isHasBeenRevealed()) {
                            revealedPlayer.setHasBeenRevealed(true);
                        }
                    }
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("revealedPlayers", revealed);
            result.put("totalPlayers", actions.size());
            result.put("revealComplete", revealed.size() == actions.size());
            // All player names for the slot-machine animation on the frontend
            List<String> slotCandidates = game.getPlayerList().stream()
                    .map(Player::getName)
                    .collect(java.util.stream.Collectors.toList());
            result.put("slotCandidates", slotCandidates);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
