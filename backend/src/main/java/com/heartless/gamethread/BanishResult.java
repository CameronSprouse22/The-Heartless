package com.heartless.gamethread;

import com.heartless.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Records all player votes for a banish vote event in a round.
 */
public class BanishResult {

    private final List<VoteObject> votes = new ArrayList<>();
    private GameRoundObject.RoundStatus status = GameRoundObject.RoundStatus.NOTSTARTED;
    private Player banishedPlayer;

    public List<VoteObject> getVotes() { return votes; }

    public void addVote(VoteObject vote) { votes.add(vote); }

    public GameRoundObject.RoundStatus getStatus() { return status; }
    public void setStatus(GameRoundObject.RoundStatus status) { this.status = status; }

    public Player getBanishedPlayer() { return banishedPlayer; }
    public void setBanishedPlayer(Player banishedPlayer) { this.banishedPlayer = banishedPlayer; }

}
