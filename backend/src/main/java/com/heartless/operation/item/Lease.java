package com.heartless.operation.item;

import com.heartless.model.GameObject;
import com.heartless.model.Player;

/**
 * Lease item — grants temporary immunity.
 */
public class Lease implements ItemsInterface {

    @Override
    public String getName() {
        return "Lease";
    }

    @Override
    public void use(Player player, GameObject game) {
        // Stub: lease immunity
    }

    @Override
    public boolean isUsable(Player player, GameObject game) {
        return !player.isDead();
    }
}
