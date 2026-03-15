package com.heartless.operation.item;

import com.heartless.model.GameObject;
import com.heartless.model.Player;

/**
 * Contract for game items that affect gameplay.
 */
public interface ItemsInterface {

    String getName();

    void use(Player player, GameObject game);

    boolean isUsable(Player player, GameObject game);
}
