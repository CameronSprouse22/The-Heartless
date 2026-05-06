package com.heartless.model.enums;

/**
 * Tracks how a player's participation in the game ended, or whether they are still alive.
 */
public enum PlayerLifeStatusEnum {
    ALIVE,
    MARKED_FOR_DEATH,
    MARKED_FOR_BANISHMENT,
    MURDERED,
    BANISHED,
    WON_AS_FAITHFUL,
    WON_AS_TRAITOR,
    WON_AS_SOLE_TRAITOR
}
