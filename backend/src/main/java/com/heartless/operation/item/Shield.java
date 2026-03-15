package com.heartless.operation.item;

import com.heartless.model.GameObject;
import com.heartless.model.Player;

/**
 * Shield item — protects a player from being murdered.
 */
public class Shield implements ItemsInterface {

    @Override
    public String getName() {
        return "Shield";
    }

    @Override
    public void use(Player player, GameObject game) {
        // Stub: protect player from murder
    }

    @Override
    public boolean isUsable(Player player, GameObject game) {
        return !player.isDead();
    }
}
