package com.heartless.model;

import com.heartless.gamethread.BanishResult;
import com.heartless.model.enums.MurderResultEnum;

/**
 * Captures all events and outcomes for a single game round.
 */
public class RoundObject {

    private final int roundNumber;
    private boolean murderRevealed;
    private boolean miniGamePlayed;
    private Player miniGameWinner;
    private BanishResult banishVoteResult;
    private VoteResultObject murderVoteResult;
    private MurderResultEnum murderResult;
    private Player murderedPlayer;

    public RoundObject(int roundNumber) {
        if (roundNumber < 1) {
            throw new IllegalArgumentException("Round number must be >= 1");
        }
        this.roundNumber = roundNumber;
        this.murderRevealed = false;
        this.miniGamePlayed = false;
    }

    public int getRoundNumber() { return roundNumber; }
    public boolean isMurderRevealed() { return murderRevealed; }
    public void setMurderRevealed(boolean murderRevealed) { this.murderRevealed = murderRevealed; }
    public boolean isMiniGamePlayed() { return miniGamePlayed; }
    public void setMiniGamePlayed(boolean miniGamePlayed) { this.miniGamePlayed = miniGamePlayed; }
    public Player getMiniGameWinner() { return miniGameWinner; }
    public void setMiniGameWinner(Player miniGameWinner) { this.miniGameWinner = miniGameWinner; }
    public BanishResult getBanishVoteResult() { return banishVoteResult; }
    public void setBanishVoteResult(BanishResult banishVoteResult) { this.banishVoteResult = banishVoteResult; }
    public Player getPlayerBanished() { return banishVoteResult != null ? banishVoteResult.getBanishedPlayer() : null; }
    public void setPlayerBanished(Player playerBanished) {
        if (banishVoteResult == null) banishVoteResult = new BanishResult();
        banishVoteResult.setBanishedPlayer(playerBanished);
    }
    public VoteResultObject getMurderVoteResult() { return murderVoteResult; }
    public void setMurderVoteResult(VoteResultObject murderVoteResult) { this.murderVoteResult = murderVoteResult; }
    public MurderResultEnum getMurderResult() { return murderResult; }
    public void setMurderResult(MurderResultEnum murderResult) { this.murderResult = murderResult; }
    public Player getMurderedPlayer() { return murderedPlayer; }
    public void setMurderedPlayer(Player murderedPlayer) { this.murderedPlayer = murderedPlayer; }
}
