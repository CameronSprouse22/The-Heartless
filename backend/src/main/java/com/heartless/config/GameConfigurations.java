package com.heartless.config;

/**
 * Centralised configuration for per-event durations.
 * All times are in milliseconds. Change a single constant here to
 * adjust that event's timer everywhere.
 */
public final class GameConfigurations {

    private GameConfigurations() {}

    /** 5 minutes in milliseconds — shared baseline. */
    private static final long FIVE_MINUTES_MS = 5L * 60L * 1_000L;
    private static final long ONE_MINUTE_MS = 1L * 60L * 1_000L;

    public static final long LOBBY_EVENT_DURATION_MS         = FIVE_MINUTES_MS;
    public static final long PRE_VOTE_EVENT_DURATION_MS      = FIVE_MINUTES_MS;
    public static final long VOTE_EVENT_DURATION_MS          = FIVE_MINUTES_MS;
    public static final long VOTE_REVEAL_EVENT_DURATION_MS   = FIVE_MINUTES_MS;
    public static final long MURDER_EVENT_DURATION_MS        = FIVE_MINUTES_MS;
    public static final long REVEL_MURDER_EVENT_DURATION_MS  = FIVE_MINUTES_MS;
    public static final long TIE_BREAK_EVENT_DURATION_MS     = FIVE_MINUTES_MS;
    public static final long RECRUIT_EVENT_DURATION_MS       = FIVE_MINUTES_MS;
    public static final long MINI_GAME_EVENT_DURATION_MS     = FIVE_MINUTES_MS;
    public static final long TESTING_EVENT_DURATION_MS       = ONE_MINUTE_MS;
    public static final long AFTER_LIFE_EVENT_DURATION_MS    = FIVE_MINUTES_MS;
}
