package com.heartless.operation.item;

import com.heartless.model.GameObject;
import com.heartless.model.Player;

/**
 * Knife item — allows a player to attack.
 */
public class Knife implements ItemsInterface {

    @Override
    public String getName() {
        return "Knife";
    }

    @Override
    public void use(Player player, GameObject game) {
        // Stub: knife attack logic
    }

    @Override
    public boolean isUsable(Player player, GameObject game) {
        return !player.isDead();
    }
}
