package com.heartless.gamethread;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.enums.GameStatusEnum;

/**
 * Evaluates whether the game should continue or end.
 * Pure Java — no Spring annotations.
 */
public class GameCriteriaObject {

    /**
     * Returns true if the game should continue (more rounds needed).
     * Game ends when: all traitors are dead (faithful win) or
     * traitors outnumber or equal faithful alive players (traitors win).
     */
    public boolean checkGameConditions(GameObject game) {
        long aliveTraitors = countAliveTraitors(game);
        long aliveFaithful = countAliveFaithful(game);

        // No traitors left -> faithful win -> end
        if (aliveTraitors == 0) {
            return false;
        }
        // Traitors >= faithful -> traitors win -> end
        if (aliveTraitors >= aliveFaithful) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if the game is in END state (final phase).
     */
    public boolean checkEndConditions(GameObject game) {
        return game.getGameStatus() == GameStatusEnum.END;
    }

    private long countAliveTraitors(GameObject game) {
        return game.getPlayerList().stream()
                .filter(Player::isTraitor)
                .filter(p -> !p.isDead())
                .count();
    }

    private long countAliveFaithful(GameObject game) {
        return game.getPlayerList().stream()
                .filter(p -> !p.isTraitor())
                .filter(p -> !p.isDead())
                .count();
    }
}
