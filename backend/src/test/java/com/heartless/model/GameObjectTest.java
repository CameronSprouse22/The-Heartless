package com.heartless.model;

import com.heartless.model.enums.GameStageEnum;
import com.heartless.model.enums.GameStatusEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameObjectTest {

    @Test
    void newGameIsInInitState() {
        GameObject game = new GameObject("ABC123");
        assertEquals("ABC123", game.getGameIdCode());
        assertEquals(GameStatusEnum.INIT, game.getGameStatus());
        assertEquals(GameStageEnum.IN_START, game.getCurrentStage());
        assertEquals(-1, game.getRound());
        assertTrue(game.isGameActive());
        assertTrue(game.getPlayerList().isEmpty());
    }

    @Test
    void transitionToStart() {
        GameObject game = new GameObject("ABC123");
        game.transitionToStart();
        assertEquals(GameStatusEnum.START, game.getGameStatus());
        assertEquals(GameStageEnum.NORMAL_ROUNDS, game.getCurrentStage());
        assertEquals(1, game.getRound());
        assertNotNull(game.getStartGameTime());
    }

    @Test
    void transitionToEnd() {
        GameObject game = new GameObject("ABC123");
        game.transitionToStart();
        game.transitionToEnd();
        assertEquals(GameStatusEnum.END, game.getGameStatus());
        assertEquals(GameStageEnum.FINAL_ROUND, game.getCurrentStage());
        assertEquals(0, game.getRound());
    }

    @Test
    void transitionToOver() {
        GameObject game = new GameObject("ABC123");
        game.transitionToStart();
        game.transitionToEnd();
        game.transitionToOver();
        assertEquals(GameStatusEnum.OVER, game.getGameStatus());
        assertFalse(game.isGameActive());
        assertNotNull(game.getEndGameTime());
    }

    @Test
    void cannotStartFromStartState() {
        GameObject game = new GameObject("ABC123");
        game.transitionToStart();
        assertThrows(IllegalStateException.class, game::transitionToStart);
    }

    @Test
    void cannotEndFromInitState() {
        GameObject game = new GameObject("ABC123");
        assertThrows(IllegalStateException.class, game::transitionToEnd);
    }

    @Test
    void cannotFinishFromStartState() {
        GameObject game = new GameObject("ABC123");
        game.transitionToStart();
        assertThrows(IllegalStateException.class, game::transitionToOver);
    }

    @Test
    void addPlayer() {
        GameObject game = new GameObject("ABC123");
        Player player = new Player("Alice", "a@b.com", null);
        game.addPlayer(player);
        assertEquals(1, game.getPlayerList().size());
        assertEquals(player, game.getPlayerList().get(0));
    }

    @Test
    void findPlayerById() {
        GameObject game = new GameObject("ABC123");
        Player player = new Player("Alice", "a@b.com", null);
        game.addPlayer(player);
        assertEquals(player, game.findPlayerById(player.getId()));
        assertNull(game.findPlayerById("nonexistent"));
    }

    @Test
    void vipIsFirstPlayer() {
        GameObject game = new GameObject("ABC123");
        Player vip = new Player("VIP", "vip@test.com", null);
        Player other = new Player("Other", "other@test.com", null);
        game.addPlayer(vip);
        game.addPlayer(other);
        assertEquals(vip, game.getVip());
    }

    @Test
    void incrementRound() {
        GameObject game = new GameObject("ABC123");
        game.transitionToStart();
        assertEquals(1, game.getRound());
        game.incrementRound();
        assertEquals(2, game.getRound());
    }
}
