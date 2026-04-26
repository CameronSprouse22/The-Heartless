package com.heartless.service;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.enums.GameStatusEnum;
import com.heartless.model.enums.PlayerStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlayerServiceTest {

    private GameStore gameStore;
    private GameService gameService;
    private PlayerService playerService;
    private InvitationService invitationService;

    private String gameCode;
    private String vipPlayerCode;

    @BeforeEach
    void setUp() {
        gameStore = new GameStore();
        invitationService = org.mockito.Mockito.mock(InvitationService.class);
        gameService = new GameService(gameStore, invitationService, new TraitorSelectionService(), new CardAssignmentService(), null, null, null);
        playerService = new PlayerService(gameStore, gameService);

        Map<String, Object> created = gameService.createGame("VIP");
        gameCode = (String) created.get("gameCode");
        vipPlayerCode = (String) created.get("playerCode");
        gameService.invitePlayer(gameCode, vipPlayerCode, "Bob", "bob@test.com");
    }

    @Test
    void joinGameSuccessfully() {
        Map<String, Object> result = playerService.joinGame(gameCode, "Bob", "bob@test.com");
        assertNotNull(result.get("playerCode"));
        assertNotNull(result.get("player"));

        GameObject game = gameStore.getGame(gameCode);
        Player bob = game.getPlayerList().get(1);
        assertEquals(PlayerStatusEnum.ACTIVE, bob.getStatus());
    }

    @Test
    void joinGameReturnsPlayerCode() {
        Map<String, Object> result = playerService.joinGame(gameCode, "Bob", "bob@test.com");
        String playerCode = (String) result.get("playerCode");
        assertNotNull(playerCode);
        assertNotEquals(vipPlayerCode, playerCode);
    }

    @Test
    void duplicateJoinRejected() {
        playerService.joinGame(gameCode, "Bob", "bob@test.com");
        assertThrows(IllegalStateException.class,
                () -> playerService.joinGame(gameCode, "Bob", "bob@test.com"));
    }

    @Test
    void invalidGameCodeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> playerService.joinGame("INVALID", "Bob", "bob@test.com"));
    }

    @Test
    void alreadyStartedGameRejected() {
        // Add enough players to start
        gameService.invitePlayer(gameCode, vipPlayerCode, "C1", "c1@test.com");
        gameService.invitePlayer(gameCode, vipPlayerCode, "C2", "c2@test.com");
        gameService.invitePlayer(gameCode, vipPlayerCode, "C3", "c3@test.com");
        playerService.joinGame(gameCode, "Bob", "bob@test.com");
        playerService.joinGame(gameCode, "C1", "c1@test.com");
        playerService.joinGame(gameCode, "C2", "c2@test.com");
        playerService.joinGame(gameCode, "C3", "c3@test.com");

        // Start the game
        GameObject game = gameStore.getGame(gameCode);
        game.transitionToStart();

        // New invite + join should fail
        assertThrows(IllegalStateException.class,
                () -> playerService.joinGame(gameCode, "Late", "late@test.com"));
    }

    @Test
    void noMatchingInvitationRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> playerService.joinGame(gameCode, "Unknown", "unknown@test.com"));
    }

    @Test
    void getPlayerInfoReturnsPrivateData() {
        Map<String, Object> joinResult = playerService.joinGame(gameCode, "Bob", "bob@test.com");
        String playerCode = (String) joinResult.get("playerCode");

        Map<String, Object> info = playerService.getPlayerInfo(gameCode, playerCode);
        assertNotNull(info.get("id"));
        assertEquals("Bob", info.get("name"));
        assertEquals("ACTIVE", info.get("status"));
        assertFalse((Boolean) info.get("isDead"));
    }
}
