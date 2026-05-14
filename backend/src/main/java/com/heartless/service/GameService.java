package com.heartless.service;

import com.heartless.config.StartingGameSettings;
import com.heartless.event.TestingEvent;
import com.heartless.gamethread.GameCriteriaObject;
import com.heartless.gamethread.GameThread;
import com.heartless.model.Card;
import com.heartless.push.PushNotificationService;
import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.enums.GameStatusEnum;
import com.heartless.model.enums.PlayerStatusEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core game management: create games, invite players, start games.
 */
@Service
public class GameService {

    private static final Logger log = LogManager.getLogger(GameService.class);

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GameStore gameStore;
    private final InvitationService invitationService;
    private final TraitorSelectionService traitorSelectionService;
    private final CardAssignmentService cardAssignmentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;
    private final VotingService votingService;

    // Maps playerCode -> { gameCode, playerId }
    private final ConcurrentHashMap<String, String[]> playerCodeMap = new ConcurrentHashMap<>();

    public GameService(GameStore gameStore, InvitationService invitationService,
                       TraitorSelectionService traitorSelectionService,
                       CardAssignmentService cardAssignmentService,
                       @Lazy SimpMessagingTemplate messagingTemplate,
                       PushNotificationService pushNotificationService,
                       @Lazy VotingService votingService) {
        this.gameStore = gameStore;
        this.invitationService = invitationService;
        this.traitorSelectionService = traitorSelectionService;
        this.cardAssignmentService = cardAssignmentService;
        this.messagingTemplate = messagingTemplate;
        this.pushNotificationService = pushNotificationService;
        this.votingService = votingService;
    }

    public Map<String, Object> createGame(String playerName) {
        String gameCode = generateGameCode();
        GameObject game = new GameObject(gameCode);

        Player vip = new Player(playerName, playerName.toLowerCase() + "@host.local", null);
        vip.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(vip);

        gameStore.putGame(gameCode, game);

        String playerCode = UUID.randomUUID().toString();
        playerCodeMap.put(playerCode, new String[]{gameCode, vip.getId()});

        Map<String, Object> result = new HashMap<>();
        result.put("gameId", game.getGameId());
        result.put("gameCode", gameCode);
        result.put("playerCode", playerCode);
        result.put("player", vip);
        return result;
    }

    public Map<String, Object> invitePlayer(String gameCode, String playerCode, String name, String contact) {
        GameObject game = getGameOrThrow(gameCode);
        validateVip(game, playerCode);
        validateNoDuplicateContact(game, contact);

        String email = contact.contains("@") ? contact : null;
        String phone = contact.contains("@") ? null : contact;
        Player player = new Player(name, email, phone);

        synchronized (game) {
            game.addPlayer(player);
        }

        invitationService.sendInvitation(contact, gameCode);

        Map<String, Object> result = new HashMap<>();
        result.put("playerId", player.getId());
        result.put("name", player.getName());
        result.put("contact", contact);
        result.put("contactType", contact.contains("@") ? "EMAIL" : "SMS");
        result.put("status", player.getStatus().name());
        result.put("invitationSent", true);
        return result;
    }

    public GameObject getGameOrThrow(String gameCode) {
        GameObject game = gameStore.getGame(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameCode);
        }
        return game;
    }

    public Map<String, Object> getGameState(String gameCode, String playerCode) {
        GameObject game = getGameOrThrow(gameCode);

        String playerId = getPlayerId(playerCode);
        if (playerId == null || game.findPlayerById(playerId) == null) {
            throw new SecurityException("Player not in this game");
        }

        boolean isVip = game.getVip() != null && game.getVip().getId().equals(playerId);
        long invitedCount = game.getPlayerList().stream()
                .filter(p -> p.getStatus() != PlayerStatusEnum.REMOVED)
                .count();
        long readyCount = game.getLobbyReadyPlayerIds().size();

        Map<String, Object> state = new HashMap<>();
        state.put("gameId", game.getGameId());
        state.put("gameCode", game.getGameIdCode());
        state.put("isGameActive", game.isGameActive());
        state.put("gameStatus", game.getGameStatus().name());
        state.put("currentStage", game.getCurrentStage().name());
        state.put("round", game.getRound());
        state.put("currentTask", game.getCurrentTask());
        state.put("playerCount", game.getActivePlayerCount());
        state.put("startGameTime", game.getStartGameTime());
        state.put("endGameTime", game.getEndGameTime());
        state.put("players", buildPublicPlayerList(game));
        state.put("isVip", isVip);
        state.put("invitedCount", invitedCount);
        state.put("readyCount", readyCount);
        state.put("myPlayerId", playerId);
        state.put("isReady", game.isLobbyPlayerReady(playerId));
        state.put("minPlayers", StartingGameSettings.MIN_PLAYERS_TO_START);
        state.put("maxPlayers", StartingGameSettings.MAX_PLAYERS);
        return state;
    }

    private List<Map<String, Object>> buildPublicPlayerList(GameObject game) {
        java.util.Set<String> readyIds = game.getLobbyReadyPlayerIds();
        return game.getPlayerList().stream()
                .filter(p -> p.getStatus() != PlayerStatusEnum.REMOVED)
                .map(p -> {
                    Map<String, Object> pm = new HashMap<>();
                    pm.put("id", p.getId());
                    pm.put("name", p.getName());
                    pm.put("status", p.getStatus().name());
                    pm.put("isDead", p.isDead());
                    pm.put("lifeStatus", p.getLifeStatus().name());
                    // Lobby-specific status label
                    String lobbyStatus;
                    if (p.getStatus() == PlayerStatusEnum.PENDING) {
                        lobbyStatus = "INVITE_PENDING";
                    } else if (p.getStatus() == PlayerStatusEnum.ACTIVE && readyIds.contains(p.getId())) {
                        lobbyStatus = "READY";
                    } else if (p.getStatus() == PlayerStatusEnum.ACTIVE) {
                        lobbyStatus = "NOT_READY";
                    } else {
                        lobbyStatus = p.getStatus().name();
                    }
                    pm.put("lobbyStatus", lobbyStatus);
                    return pm;
                })
                .toList();
    }

    /**
     * Marks the requesting player as ready in the lobby.
     */
    public Map<String, Object> markLobbyReady(String gameCode, String playerCode) {
        GameObject game = getGameOrThrow(gameCode);
        String playerId = getPlayerId(playerCode);
        if (playerId == null || game.findPlayerById(playerId) == null) {
            throw new SecurityException("Player not in this game");
        }
        game.markLobbyPlayerReady(playerId);
        log.info("Player {} marked ready in lobby — gameCode={}", playerId, gameCode);
        Map<String, Object> result = new HashMap<>();
        result.put("ready", true);
        result.put("playerId", playerId);
        return result;
    }

    /**
     * Removes a player's ready state in the lobby (they toggled back to Not Ready).
     */
    public Map<String, Object> markLobbyNotReady(String gameCode, String playerCode) {
        GameObject game = getGameOrThrow(gameCode);
        String playerId = getPlayerId(playerCode);
        if (playerId == null || game.findPlayerById(playerId) == null) {
            throw new SecurityException("Player not in this game");
        }
        game.markLobbyPlayerNotReady(playerId);
        log.info("Player {} marked NOT ready in lobby — gameCode={}", playerId, gameCode);
        Map<String, Object> result = new HashMap<>();
        result.put("ready", false);
        result.put("playerId", playerId);
        return result;
    }

    /**
     * Returns all sitrep data for the current game state:
     * round, full player list with life statuses, and pre-filtered categories
     * (murdered, banished, banished traitors).
     */
    public Map<String, Object> getSitRep(String gameCode, String playerCode) {
        GameObject game = getGameOrThrow(gameCode);
        String playerId = getPlayerId(playerCode);
        if (playerId == null || game.findPlayerById(playerId) == null) {
            throw new SecurityException("Player not in this game");
        }

        List<Map<String, Object>> allPlayers = new ArrayList<>();
        List<Map<String, Object>> murderedPlayers = new ArrayList<>();
        List<Map<String, Object>> banishedPlayers = new ArrayList<>();
        List<Map<String, Object>> banishedTraitors = new ArrayList<>();

        for (Player p : game.getPlayerList()) {
            String lifeStatus = p.getLifeStatus().name();
            boolean dead = p.isDead();

            Map<String, Object> entry = new HashMap<>();
            entry.put("id", p.getId());
            entry.put("name", p.getName());
            entry.put("lifeStatus", lifeStatus);
            entry.put("alive", !dead);
            allPlayers.add(entry);

            switch (p.getLifeStatus()) {
                case MURDERED -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    murderedPlayers.add(m);
                }
                case BANISHED -> {
                    Map<String, Object> b = new HashMap<>();
                    b.put("id", p.getId());
                    b.put("name", p.getName());
                    b.put("wasTraitor", p.isTraitor());
                    banishedPlayers.add(b);
                    if (p.isTraitor()) {
                        Map<String, Object> bt = new HashMap<>();
                        bt.put("id", p.getId());
                        bt.put("name", p.getName());
                        banishedTraitors.add(bt);
                    }
                }
                default -> { /* still in play */ }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("round", game.getRound());
        result.put("players", allPlayers);
        result.put("murderedPlayers", murderedPlayers);
        result.put("banishedPlayers", banishedPlayers);
        result.put("banishedTraitors", banishedTraitors);
        result.put("aliveCount", allPlayers.stream().filter(p -> Boolean.TRUE.equals(p.get("alive"))).count());
        result.put("totalCount", allPlayers.size());
        Map<String, UserSelectionsState> stateMap = game.getSelectionStateMap();
        result.put("confirmedCount", stateMap.values().stream().filter(UserSelectionsState::isSubmitPressed).count());
        result.put("requiredCount", (long) stateMap.size());
        UserSelectionsState myState = game.getSelectionState(playerId);
        result.put("myConfirmed", myState != null && myState.isSubmitPressed());
        return result;
    }

    /**
     * VIP starts the game with unconfirmed players — only active players participate.
     * Requires at least MIN_PLAYERS_TO_START in the invite list (any status).
     */
    public Map<String, Object> startGameWithUnconfirmed(String gameCode, String playerCode) {
        log.info("Starting game with unconfirmed — gameCode={}", gameCode);
        GameObject game = getGameOrThrow(gameCode);
        validateVip(game, playerCode);

        long invitedCount = game.getPlayerList().stream()
                .filter(p -> p.getStatus() != PlayerStatusEnum.REMOVED)
                .count();
        if (invitedCount < StartingGameSettings.MIN_PLAYERS_TO_START) {
            throw new IllegalStateException(
                "Need at least " + StartingGameSettings.MIN_PLAYERS_TO_START + " players invited to start");
        }

        List<Player> activePlayers = game.getPlayerList().stream()
                .filter(p -> p.getStatus() == PlayerStatusEnum.ACTIVE)
                .toList();
        if (activePlayers.isEmpty()) {
            throw new IllegalStateException("No active players available to start the game");
        }

        return doStartGame(game, gameCode, activePlayers, true);
    }

    public Map<String, Object> startGame(String gameCode, String playerCode) {
        log.info("Game starting — gameCode={}", gameCode);
        GameObject game = getGameOrThrow(gameCode);
        validateVip(game, playerCode);

        long invitedCount = game.getPlayerList().stream()
                .filter(p -> p.getStatus() != PlayerStatusEnum.REMOVED)
                .count();
        if (invitedCount < StartingGameSettings.MIN_PLAYERS_TO_START) {
            throw new IllegalStateException(
                "Need at least " + StartingGameSettings.MIN_PLAYERS_TO_START + " players invited to start");
        }

        // All non-VIP invited players must have accepted (ACTIVE)
        long pendingCount = game.getPlayerList().stream()
                .filter(p -> p.getStatus() == PlayerStatusEnum.PENDING)
                .count();
        if (pendingCount > 0) {
            throw new IllegalStateException(
                pendingCount + " invited player(s) have not yet accepted. Use \"Start With Unconfirmed\" to proceed anyway.");
        }

        List<Player> activePlayers = game.getPlayerList().stream()
                .filter(p -> p.getStatus() == PlayerStatusEnum.ACTIVE)
                .toList();

        // All active players must have pressed Ready
        java.util.Set<String> readyIds = game.getLobbyReadyPlayerIds();
        List<String> notReadyNames = activePlayers.stream()
                .filter(p -> !readyIds.contains(p.getId()))
                .map(Player::getName)
                .toList();
        if (!notReadyNames.isEmpty()) {
            throw new IllegalStateException(
                "The following players are not ready: " + String.join(", ", notReadyNames));
        }

        int activeCount = activePlayers.size();
        log.debug("Active player count={} for gameCode={}", activeCount, gameCode);

        return doStartGame(game, gameCode, activePlayers, false);
    }

    private Map<String, Object> doStartGame(GameObject game, String gameCode,
                                             List<Player> activePlayers, boolean withUnconfirmed) {
        int activeCount = activePlayers.size();

        synchronized (game) {
            game.setLobbyVipForcedStart(true);
            game.transitionToStart();
            assignRoles(activePlayers, activeCount);
        }

        // Create and store the GameThread
        GameCriteriaObject criteria = new GameCriteriaObject();
        GameThread gameThread = new GameThread(game, criteria);
        gameThread.setMessagingTemplate(messagingTemplate);
        gameThread.setPushNotificationService(pushNotificationService);
        gameThread.setVotingService(votingService);
        gameThread.gameInit();
        gameStore.putGameThread(gameCode, gameThread);

        log.info("Game started — gameCode={} players={}", gameCode, activeCount);

        Map<String, Object> result = new HashMap<>();
        result.put("gameStatus", game.getGameStatus().name());
        result.put("currentStage", game.getCurrentStage().name());
        result.put("playerCount", activeCount);
        result.put("message", "Game started! Roles have been assigned.");
        return result;
    }

    private void assignRoles(List<Player> activePlayers, int activeCount) {
        Set<Integer> traitorIndices = TraitorSelectionService.selectTraitorIndices(activeCount, new SecureRandom());
        for (int idx : traitorIndices) {
            activePlayers.get(idx).setTraitor(true);
        }
        Map<Player, Card> cardAssignments = cardAssignmentService.assignCards(activePlayers);
        for (Map.Entry<Player, Card> entry : cardAssignments.entrySet()) {
            entry.getKey().setCard(entry.getValue());
        }
    }

    public String getPlayerId(String playerCode) {
        String[] info = playerCodeMap.get(playerCode);
        return info != null ? info[1] : null;
    }

    public String getPlayerName(String playerCode) {
        String[] info = playerCodeMap.get(playerCode);
        if (info == null) return null;
        String gc = info[0];
        String pid = info[1];
        GameObject game = gameStore.getGame(gc);
        if (game == null) return null;
        return game.getPlayerList().stream()
                .filter(p -> p.getId().equals(pid))
                .map(Player::getName)
                .findFirst()
                .orElse(null);
    }

    public String getGameCodeForPlayer(String playerCode) {
        String[] info = playerCodeMap.get(playerCode);
        return info != null ? info[0] : null;
    }

    public GameThread getGameThread(String gameCode) {
        return gameStore.getGameThread(gameCode);
    }

    public void registerPlayerCode(String playerCode, String gameCode, String playerId) {
        playerCodeMap.put(playerCode, new String[]{gameCode, playerId});
    }

    /**
     * Resolve a player by name within a game. Returns existing playerCode or creates one.
     */
    public Map<String, Object> resolvePlayerByName(String gameCode, String playerName) {
        GameObject game = getGameOrThrow(gameCode);
        Player player = game.findPlayerByName(playerName);
        if (player == null) {
            throw new IllegalArgumentException("Player not found: " + playerName);
        }
        String existingCode = findPlayerCodeByPlayerId(player.getId());
        if (existingCode == null) {
            existingCode = UUID.randomUUID().toString();
            playerCodeMap.put(existingCode, new String[]{gameCode, player.getId()});
        }
        Map<String, Object> result = new HashMap<>();
        result.put("playerCode", existingCode);
        result.put("playerId", player.getId());
        result.put("playerName", player.getName());
        result.put("isTraitor", player.isTraitor());
        result.put("isDead", player.isDead());
        result.put("lifeStatus", player.getLifeStatus().name());
        return result;
    }

    private String findPlayerCodeByPlayerId(String playerId) {
        for (Map.Entry<String, String[]> entry : playerCodeMap.entrySet()) {
            if (entry.getValue()[1].equals(playerId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void validateVip(GameObject game, String playerCode) {
        String playerId = getPlayerId(playerCode);
        Player vip = game.getVip();
        if (vip == null || !vip.getId().equals(playerId)) {
            throw new IllegalStateException("Only the VIP can perform this action");
        }
    }

    private void validateNoDuplicateContact(GameObject game, String contact) {
        boolean exists = game.getPlayerList().stream().anyMatch(p -> {
            String playerContact = p.getContact();
            return playerContact != null && playerContact.equalsIgnoreCase(contact);
        });
        if (exists) {
            throw new IllegalStateException("Player with this contact already invited");
        }
    }

    private static final String[] TEST_PLAYER_NAMES = {
        //"Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel" 
        "A", "B", "C", "D" 
    };

    /**
     * Creates a test game with hardcoded players all set to ACTIVE + ready,
     * and pre-sets the forced-start flag so the lobby event is bypassed immediately
     * when startGame is called from the test dashboard.
     */
    public Map<String, Object> createTestGame() {
        String gameCode = generateGameCode();
        GameObject game = new GameObject(gameCode);

        List<Map<String, Object>> playerInfoList = new ArrayList<>();

        for (int i = 0; i < TEST_PLAYER_NAMES.length; i++) {
            String name = TEST_PLAYER_NAMES[i];
            String email = name.toLowerCase() + "@gmail.com";
            Player player = new Player(name, email, null);
            player.setStatus(PlayerStatusEnum.ACTIVE);
            if (i < 2) player.setTraitor(true); // first 2 players are traitors
            game.addPlayer(player);
            // Pre-mark all test players as ready so the lobby is skipped
            game.markLobbyPlayerReady(player.getId());

            String playerCode = UUID.randomUUID().toString();
            playerCodeMap.put(playerCode, new String[]{gameCode, player.getId()});

            Map<String, Object> info = new HashMap<>();
            info.put("playerCode", playerCode);
            info.put("playerId", player.getId());
            info.put("name", name);
            info.put("email", email);
            playerInfoList.add(info);
        }

        // Pre-flag forced start so the lobby event exits immediately
        game.setLobbyVipForcedStart(true);

        gameStore.putGame(gameCode, game);

        Map<String, Object> result = new HashMap<>();
        result.put("gameId", game.getGameId());
        result.put("gameCode", gameCode);
        result.put("players", playerInfoList);
        result.put("vipPlayerCode", playerInfoList.get(0).get("playerCode"));
        return result;
    }

    private String generateGameCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        String code = sb.toString();
        if (gameStore.containsGame(code)) {
            return generateGameCode();
        }
        return code;
    }
}
