package com.heartless.model.enums;

/**
 * Tracks how a player's participation in the game ended, or whether they are still alive.
 * Terminal dead states (MURDERED, BANISHED, PLAYER_BOOTED, PLAYER_LEFT_GAME) return true
 * from {@link #isDead()}, which drives AfterLife routing in buildGameState().
 * Intermediate states (MARKED_FOR_MURDER, MARKED_FOR_BANISHMENT) are not dead — the player
 * still participates normally until the corresponding reveal event completes.
 */
public enum PlayerLifeStatusEnum {
    ALIVE,
    MARKED_FOR_DEATH,
    MARKED_FOR_MURDER,
    MURDERED,
    MARKED_FOR_BANISHMENT,
    BANISHED,
    WON_AS_FAITHFUL,
    WON_AS_TRAITOR,
    WON_AS_SOLE_TRAITOR,
    PLAYER_LEFT_GAME,
    PLAYER_BOOTED;

    /**
     * Returns true for terminal dead states — the player is fully removed from active play.
     */
    public boolean isDead() {
        return this == MURDERED || this == BANISHED || this == PLAYER_BOOTED || this == PLAYER_LEFT_GAME;
    }
}
