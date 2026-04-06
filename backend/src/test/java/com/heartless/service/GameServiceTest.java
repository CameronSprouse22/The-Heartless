package com.heartless.service;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.enums.GameStatusEnum;
import com.heartless.model.enums.PlayerStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameServiceTest {

    private GameStore gameStore;
    private InvitationService invitationService;
    private TraitorSelectionService traitorSelectionService;
    private CardAssignmentService cardAssignmentService;
    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameStore = new GameStore();
        invitationService = mock(InvitationService.class);
        traitorSelectionService = new TraitorSelectionService();
        cardAssignmentService = new CardAssignmentService();
        gameService = new GameService(gameStore, invitationService, traitorSelectionService, cardAssignmentService, null, null);
    }

    // US1 tests - T021

    @Test
    void createGameGeneratesCode() {
        var result = gameService.createGame("Host");
        assertNotNull(result.get("gameCode"));
        String gameCode = (String) result.get("gameCode");
        assertEquals(6, gameCode.length());
        assertTrue(gameCode.matches("[A-Z0-9]{6}"));
    }

    @Test
    void createGameReturnsPlayerCode() {
        var result = gameService.createGame("Host");
        assertNotNull(result.get("playerCode"));
    }

    @Test
    void createGameMakesVipFirstPlayer() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");
        GameObject game = gameStore.getGame(gameCode);
        assertNotNull(game);
        assertEquals(1, game.getPlayerList().size());
        assertEquals("Host", game.getVip().getName());
        assertEquals(PlayerStatusEnum.ACTIVE, game.getVip().getStatus());
    }

    @Test
    void createGameStoresInGameStore() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");
        assertTrue(gameStore.containsGame(gameCode));
    }

    @Test
    void createGameSetsInitStatus() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");
        GameObject game = gameStore.getGame(gameCode);
        assertEquals(GameStatusEnum.INIT, game.getGameStatus());
    }

    @Test
    void invitePlayerAddsWithPendingStatus() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");
        String playerCode = (String) result.get("playerCode");

        var inviteResult = gameService.invitePlayer(gameCode, playerCode, "Bob", "bob@test.com");
        assertNotNull(inviteResult);

        GameObject game = gameStore.getGame(gameCode);
        assertEquals(2, game.getPlayerList().size());
        Player invited = game.getPlayerList().get(1);
        assertEquals("Bob", invited.getName());
        assertEquals(PlayerStatusEnum.PENDING, invited.getStatus());
    }

    @Test
    void invitePlayerSendsInvitation() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");
        String playerCode = (String) result.get("playerCode");

        gameService.invitePlayer(gameCode, playerCode, "Bob", "bob@test.com");
        verify(invitationService).sendInvitation("bob@test.com", gameCode);
    }

    @Test
    void invitePlayerOnlyByVip() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");

        assertThrows(IllegalStateException.class,
                () -> gameService.invitePlayer(gameCode, "wrong-code", "Bob", "bob@test.com"));
    }

    @Test
    void invitePlayerRejectsDuplicate() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");
        String playerCode = (String) result.get("playerCode");

        gameService.invitePlayer(gameCode, playerCode, "Bob", "bob@test.com");
        assertThrows(IllegalStateException.class,
                () -> gameService.invitePlayer(gameCode, playerCode, "Bob2", "bob@test.com"));
    }

    // US3 tests - T035

    @Test
    void startGameTransitionsToStart() {
        var result = createGameWith4Players();
        String gameCode = (String) result.get("gameCode");
        String playerCode = (String) result.get("playerCode");

        var startResult = gameService.startGame(gameCode, playerCode);
        assertEquals("START", startResult.get("gameStatus"));
        assertEquals("NORMAL_ROUNDS", startResult.get("currentStage"));
        assertEquals(4, startResult.get("playerCount"));
    }

    @Test
    void startGameRequiresMinimum4Players() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");
        String playerCode = (String) result.get("playerCode");

        // Only 1 player (the VIP)
        assertThrows(IllegalStateException.class,
                () -> gameService.startGame(gameCode, playerCode));
    }

    @Test
    void startGameOnlyVipCanStart() {
        var result = createGameWith4Players();
        String gameCode = (String) result.get("gameCode");

        assertThrows(IllegalStateException.class,
                () -> gameService.startGame(gameCode, "wrong-code"));
    }

    @Test
    void startGameRejectsAlreadyStarted() {
        var result = createGameWith4Players();
        String gameCode = (String) result.get("gameCode");
        String playerCode = (String) result.get("playerCode");

        gameService.startGame(gameCode, playerCode);
        assertThrows(IllegalStateException.class,
                () -> gameService.startGame(gameCode, playerCode));
    }

    @Test
    void getGameStateReturnsFullState() {
        var result = createGameWith4Players();
        String gameCode = (String) result.get("gameCode");
        String playerCode = (String) result.get("playerCode");

        var state = gameService.getGameState(gameCode, playerCode);
        assertNotNull(state);
        assertEquals(gameCode, state.get("gameCode"));
        assertEquals("INIT", state.get("gameStatus"));
        assertEquals(4, state.get("playerCount"));
        assertNotNull(state.get("players"));
    }

    /**
     * Helper: creates a game with VIP + 3 invited-and-joined players (4 total ACTIVE).
     */
    private Map<String, Object> createGameWith4Players() {
        var result = gameService.createGame("Host");
        String gameCode = (String) result.get("gameCode");
        String playerCode = (String) result.get("playerCode");

        for (int i = 1; i <= 3; i++) {
            gameService.invitePlayer(gameCode, playerCode, "Player" + i, "p" + i + "@test.com");
            // Activate the invited player (simulate join)
            GameObject game = gameStore.getGame(gameCode);
            Player invited = game.getPlayerList().get(i);
            invited.setStatus(PlayerStatusEnum.ACTIVE);
        }
        return result;
    }
}
