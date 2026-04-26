package com.heartless.controller;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.push.PushNotificationService;
import com.heartless.service.GameService;
import com.heartless.service.GameStore;
import com.heartless.service.VotingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class VoteController {

    private static final Logger log = LogManager.getLogger(VoteController.class);

    private final VotingService votingService;
    private final GameService gameService;
    private final GameStore gameStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    /** Tracks "gameCode:round" keys where a banish-vote push has already been sent this round. */
    private final Set<String> banishNotifiedRounds = ConcurrentHashMap.newKeySet();
    /** Tracks "gameCode:round" keys where a murder-vote push has already been sent this round. */
    private final Set<String> murderNotifiedRounds = ConcurrentHashMap.newKeySet();

    public VoteController(VotingService votingService, GameService gameService,
                          GameStore gameStore, SimpMessagingTemplate messagingTemplate,
                          PushNotificationService pushNotificationService) {
        this.votingService = votingService;
        this.gameService = gameService;
        this.gameStore = gameStore;
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
    }

    /** Broadcast live (pre-submit) selection to other players on the banish vote page and persist to backend state. */
    @MessageMapping("/games/{gameCode}/banish-selection")
    public void handleBanishSelection(@DestinationVariable String gameCode,
                                      Map<String, Object> payload,
                                      StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;
        String playerId = (String) attrs.get("playerId");
        if (playerId == null) return;

        log.debug("Banish selection received — gameCode={} playerId={} payload={}", gameCode, playerId, payload);

        GameObject game = gameStore.getGame(gameCode);
        if (game == null) return;
        Player player = game.findPlayerById(playerId);
        if (player == null) return;

        // Persist selection state
        String targetId = payload.get("targetId") instanceof String s ? s : null;
        UserSelectionsState selState = game.getSelectionState(playerId);
        if (selState != null) {
            List<String> prev = selState.getSelectedItems();
            selState.setSelectedItems(targetId != null ? List.of(targetId) : List.of());
            log.info("Banish selection changed — gameCode={} player={} ({}) prev={} new={}",
                    gameCode, player.getName(), playerId, prev, targetId);
        } else {
            log.warn("Banish selection ignored — selState is null (event not started yet?) gameCode={} player={}",
                    gameCode, playerId);
        }

        // Do not broadcast selections to other clients during banish vote
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

        // Persist selection state
        @SuppressWarnings("unchecked")
        List<String> targetIds = payload.get("targetIds") instanceof List<?> l
                ? (List<String>) l : List.of();
        UserSelectionsState selState = game.getSelectionState(playerId);
        if (selState != null) {
            selState.setSelectedItems(targetIds);
        }

        Map<String, Object> broadcast = new HashMap<>();
        broadcast.put("type", "MURDER_SELECTION_UPDATE");
        broadcast.put("voterId", playerId);
        broadcast.put("voterName", player.getName());
        broadcast.put("targetIds", payload.getOrDefault("targetIds", List.of()));
        broadcast.put("targetNames", payload.getOrDefault("targetNames", List.of()));
        messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/murder-vote", broadcast);
    }

    /** Persist a player's vote page text field value to backend state. */
    @MessageMapping("/games/{gameCode}/vote-text")
    public void handleVoteText(@DestinationVariable String gameCode,
                               Map<String, Object> payload,
                               StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) return;
        String playerId = (String) attrs.get("playerId");
        if (playerId == null) return;

        GameObject game = gameStore.getGame(gameCode);
        if (game == null) return;

        String text = payload.get("text") instanceof String s ? s : "";
        UserSelectionsState state = game.getSelectionState(playerId);
        if (state != null) {
            state.setTextFieldInput(text);
        }
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

            // Fire one push per game per round the first time any player votes
            GameObject game = gameStore.getGame(gameCode);
            if (game != null) {
                String key = gameCode + ":" + game.getRound() + ":banish";
                if (banishNotifiedRounds.add(key)) {
                    pushNotificationService.notifyGame(gameCode, "Vote-off",
                            "Banish voting is underway — cast your vote now!");
                }
            }

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

    @PostMapping("/games/{gameCode}/reveal/close")
    public ResponseEntity<Map<String, Object>> closeReveal(
            @PathVariable String gameCode,
            @RequestHeader("X-Player-Code") String playerCode) {
        String playerId = gameService.getPlayerId(playerCode);
        if (playerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Invalid player code"));
        }
        GameObject game = gameStore.getGame(gameCode);
        if (game == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Game not found"));
        }
        UserSelectionsState selState = game.getSelectionState(playerId);
        if (selState == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No active reveal event"));
        }
        selState.setSubmitPressed(true);
        return ResponseEntity.ok(Map.of("closed", true));
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

            // Fire one push per game per round the first time any traitor votes
            GameObject game = gameStore.getGame(gameCode);
            if (game != null) {
                String key = gameCode + ":" + game.getRound() + ":murder";
                if (murderNotifiedRounds.add(key)) {
                    pushNotificationService.notifyGame(gameCode, "Murder Vote",
                            "The traitors are voting — check your menu!");
                }
            }

            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
