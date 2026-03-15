package com.heartless.model;

import java.util.Objects;

/**
 * A single vote cast by one player targeting another.
 */
public class Vote {

    private final Player castingPlayer;
    private final Player receivingPlayer;

    public Vote(Player castingPlayer, Player receivingPlayer) {
        Objects.requireNonNull(castingPlayer, "Casting player must not be null");
        Objects.requireNonNull(receivingPlayer, "Receiving player must not be null");
        if (castingPlayer.equals(receivingPlayer)) {
            throw new IllegalArgumentException("A player cannot vote for themselves");
        }
        this.castingPlayer = castingPlayer;
        this.receivingPlayer = receivingPlayer;
    }

    public Player getCastingPlayer() {
        return castingPlayer;
    }

    public Player getReceivingPlayer() {
        return receivingPlayer;
    }

    @Override
    public String toString() {
        return castingPlayer.getName() + " voted for " + receivingPlayer.getName();
    }
}
