package com.heartless.operation.condition;

import com.heartless.model.GameObject;
import com.heartless.model.Player;

/**
 * Checks whether the game has a minimum number of alive players.
 */
public class NumberOfPlayerCondition implements ConditionInterface {

    private final int minimumPlayers;

    public NumberOfPlayerCondition(int minimumPlayers) {
        this.minimumPlayers = minimumPlayers;
    }

    @Override
    public boolean checkGameConditions(GameObject game) {
        long alivePlayers = game.getPlayerList().stream()
                .filter(p -> !p.isDead())
                .count();
        return alivePlayers >= minimumPlayers;
    }

    public int getMinimumPlayers() {
        return minimumPlayers;
    }
}
