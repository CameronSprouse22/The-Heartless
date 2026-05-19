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
    private static final long TWO_MINUTES_MS  = 2L * 60L * 1_000L;
    private static final long ONE_MINUTE_MS = 1L * 60L * 1_000L;
    private static final long HALF_MINUTE_MS = 1L * 30L * 1_000L;
    private static final long TEN_SECONDS_MS = 10L * 1_000L;

    public static final long AFTER_LIFE_GAME_EVENT_DURATION_MS = FIVE_MINUTES_MS;
    public static final long BANISH_PRE_EVENT_DURATION_MS     = TEN_SECONDS_MS;
    public static final long BANISH_REVEAL_EVENT_DURATION_MS  = HALF_MINUTE_MS;
    public static final long BANISH_VOTE_EVENT_DURATION_MS    = HALF_MINUTE_MS;
    public static final long TIE_BREAK_EVENT_DURATION_MS      = HALF_MINUTE_MS;
    public static final long LOBBY_EVENT_DURATION_MS          = FIVE_MINUTES_MS;
    public static final long MINI_GAME_EVENT_DURATION_MS      = TWO_MINUTES_MS;
    public static final long MURDER_PRE_EVENT_DURATION_MS     = TEN_SECONDS_MS;
    public static final long MURDER_REVEAL_EVENT_DURATION_MS  = FIVE_MINUTES_MS;
    public static final long MURDER_VOTE_EVENT_DURATION_MS    = FIVE_MINUTES_MS;
    public static final long PRE_VOTE_EVENT_DURATION_MS       = FIVE_MINUTES_MS;
    public static final long RECRUIT_EVENT_DURATION_MS        = FIVE_MINUTES_MS;
    public static final long TESTING_EVENT_DURATION_MS        = TEN_SECONDS_MS;
    public static final long VOTE_REVEAL_EVENT_DURATION_MS    = FIVE_MINUTES_MS;
    public static final long REVEAL_PLAYER_IDENTITY_EVENT_DURATION_MS = HALF_MINUTE_MS;
    public static final long REVEAL_ROLE_EVENT_DURATION_MS = HALF_MINUTE_MS;
    /** Duration for the sit-rep event shown at the start of each round. */
    public static final long SITREP_EVENT_DURATION_MS = HALF_MINUTE_MS;
    /** Duration for the scuttlebutt (individual chat) event. */
    public static final long SCUTTLEBUTT_EVENT_DURATION_MS = FIVE_MINUTES_MS;

    // ── UI toggles ───────────────────────────────────────────────────────────

    /**
     * When {@code false}, the event intro dialog (initial message overlay)
     * shown to every player at the start of each event is suppressed.
     * Set to {@code true} to re-enable the overlays.
     */
    public static final boolean SHOW_EVENT_DIALOGS = false;
}
