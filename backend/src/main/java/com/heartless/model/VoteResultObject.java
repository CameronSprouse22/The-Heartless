package com.heartless.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregation of all votes for a single voting round.
 * Provides derived properties: winner, tally, and tie detection.
 */
public class VoteResultObject {

    private final List<Vote> voteList;

    public VoteResultObject(List<Vote> voteList) {
        this.voteList = voteList != null ? List.copyOf(voteList) : List.of();
    }

    public List<Vote> getVoteList() {
        return voteList;
    }

    /**
     * Returns a tally of votes: player → vote count.
     */
    public Map<Player, Integer> getTally() {
        Map<Player, Integer> tally = new LinkedHashMap<>();
        for (Vote vote : voteList) {
            Player target = vote.getReceivingPlayer();
            tally.merge(target, 1, Integer::sum);
        }
        return Collections.unmodifiableMap(tally);
    }

    /**
     * Returns the player with the most votes, or null if tied.
     */
    public Player getWinner() {
        if (isTie()) return null;
        Map<Player, Integer> tally = getTally();
        return tally.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Returns true if the top two vote-getters have equal votes.
     */
    public boolean isTie() {
        Map<Player, Integer> tally = getTally();
        if (tally.size() < 2) return false;
        List<Integer> counts = tally.values().stream()
                .sorted(Collections.reverseOrder())
                .toList();
        return counts.get(0).equals(counts.get(1));
    }
}
