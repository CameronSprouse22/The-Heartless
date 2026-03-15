package com.heartless.operation.item;

import com.heartless.model.GameObject;
import com.heartless.model.Player;

/**
 * Tether item — links two players together.
 */
public class Tether implements ItemsInterface {

    @Override
    public String getName() {
        return "Tether";
    }

    @Override
    public void use(Player player, GameObject game) {
        // Stub: tether two players
    }

    @Override
    public boolean isUsable(Player player, GameObject game) {
        return !player.isDead();
    }
}
