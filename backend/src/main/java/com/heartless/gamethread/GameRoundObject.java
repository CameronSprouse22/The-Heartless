package com.heartless.gamethread;

/**
 * Tracks the state and results of a single game round within the thread lifecycle.
 */
public class GameRoundObject {

    public enum RoundStatus {
        BYPASSED,
        NOTSTARTED,
        INPROGRESS,
        ENDED,
        RESOLVED
    }

    private final int roundId;

    private final GameEventResult gameResult = new GameEventResult();
    private final BanishResult banishResult = new BanishResult();
    private final MurderResult murderResult = new MurderResult();

    public GameRoundObject(int roundId) {
        this.roundId = roundId;
    }

    public int getRoundId() { return roundId; }

    public GameEventResult getGameResult() { return gameResult; }
    public BanishResult getBanishResult() { return banishResult; }
    public MurderResult getMurderResult() { return murderResult; }

    /**
     * Resolves any unfinished sub-events in this round.
     * Call when a round ends due to timeout or being skipped.
     */
    public void resolveEvent() {
        if (banishResult.getStatus() != RoundStatus.RESOLVED) {
            banishResult.setStatus(RoundStatus.RESOLVED);
        }
        if (murderResult.getStatus() != RoundStatus.RESOLVED) {
            murderResult.setStatus(RoundStatus.RESOLVED);
        }
        if (gameResult.getStatus() == RoundStatus.INPROGRESS
                || gameResult.getStatus() == RoundStatus.NOTSTARTED) {
            gameResult.setStatus(RoundStatus.RESOLVED);
        }
    }
}
