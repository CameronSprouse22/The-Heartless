package com.heartless.config;

/**
 * Centralised configuration for game start requirements.
 */
public final class StartingGameSettings {

    private StartingGameSettings() {}

    /** Minimum number of players that must be invited (in the list) to enable the VIP start buttons. */
    public static final int MIN_PLAYERS_TO_START = 3;

    /** Maximum number of players that can join a game (including the VIP). */
    public static final int MAX_PLAYERS = 10;
}
