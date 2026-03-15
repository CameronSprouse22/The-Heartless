package com.heartless.gamethread;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.enums.PlayerStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameCriteriaObjectTest {

    private GameCriteriaObject criteria;
    private GameObject game;

    @BeforeEach
    void setUp() {
        game = new GameObject("CRIT01");
        game.transitionToStart();
        criteria = new GameCriteriaObject();
    }

    private Player createActive(String name, boolean isTraitor) {
        Player p = new Player(name, name.toLowerCase() + "@test.com", null);
        p.setStatus(PlayerStatusEnum.ACTIVE);
        p.setTraitor(isTraitor);
        game.addPlayer(p);
        return p;
    }

    @Test
    void gameContinuesWithMixedPlayers() {
        // 3 faithful, 1 traitor -> game continues
        createActive("A", false);
        createActive("B", false);
        createActive("C", false);
        createActive("T", true);
        assertTrue(criteria.checkGameConditions(game));
    }

    @Test
    void gameEndsWhenAllTraitorsBanished() {
        // All traitors are dead -> faithful win -> game ends
        Player t = createActive("T", true);
        t.setDead(true);
        createActive("A", false);
        createActive("B", false);
        assertFalse(criteria.checkGameConditions(game));
    }

    @Test
    void gameEndsWhenTraitorsOutnumberFaithful() {
        // 2 traitors, 1 faithful alive -> traitors win -> game ends
        createActive("T1", true);
        createActive("T2", true);
        createActive("A", false);
        assertFalse(criteria.checkGameConditions(game));
    }

    @Test
    void gameEndsWhenTraitorsEqualFaithful() {
        // 2 traitors, 2 faithful alive -> traitors have majority -> game ends
        createActive("T1", true);
        createActive("T2", true);
        createActive("A", false);
        createActive("B", false);
        assertFalse(criteria.checkGameConditions(game));
    }

    @Test
    void deadPlayersNotCounted() {
        // 1 traitor alive, 2 faithful alive, 1 faithful dead
        createActive("T", true);
        createActive("A", false);
        createActive("B", false);
        Player dead = createActive("C", false);
        dead.setDead(true);
        assertTrue(criteria.checkGameConditions(game));
    }

    @Test
    void checkEndConditionsReturnsTrueForEndState() {
        game.transitionToEnd();
        assertTrue(criteria.checkEndConditions(game));
    }

    @Test
    void checkEndConditionsReturnsFalseForStartState() {
        assertFalse(criteria.checkEndConditions(game));
    }

    @Test
    void noPlayersEndsGame() {
        // No players at all -> game should end
        assertFalse(criteria.checkGameConditions(game));
    }
}
