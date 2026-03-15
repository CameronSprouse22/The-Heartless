package com.heartless.operation.item;

import com.heartless.model.GameObject;
import com.heartless.model.Player;

/**
 * Duel item — initiates a duel between two players.
 */
public class Duel implements ItemsInterface {

    @Override
    public String getName() {
        return "Duel";
    }

    @Override
    public void use(Player player, GameObject game) {
        // Stub: duel execution
    }

    @Override
    public boolean isUsable(Player player, GameObject game) {
        return !player.isDead();
    }
}
