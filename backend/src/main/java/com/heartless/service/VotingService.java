package com.heartless.service;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.Vote;
import com.heartless.model.enums.PlayerStatusEnum;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages banish and murder votes per game per round.
 */
@Service
public class VotingService {

    private final GameStore gameStore;

    // gameCode -> list of banish votes for current round
    private final ConcurrentHashMap<String, List<Vote>> banishVotes = new ConcurrentHashMap<>();
    // gameCode -> list of murder votes for current round
    private final ConcurrentHashMap<String, List<Map<String, Object>>> murderVotes = new ConcurrentHashMap<>();

    public VotingService(GameStore gameStore) {
        this.gameStore = gameStore;
    }

    // --- Banish Vote ---

    public Map<String, Object> getBanishCandidates(String gameCode, String playerId) {
        GameObject game = getGameOrThrow(gameCode);
        Player voter = getPlayerOrThrow(game, playerId);

        if (voter.isDead()) {
            throw new IllegalStateException("Dead players cannot vote");
        }

        List<Map<String, Object>> candidates = buildCandidateList(game, playerId);
        String existingVote = findExistingVote(gameCode, playerId);

        Map<String, Object> result = new HashMap<>();
        result.put("voteType", "BANISH");
        result.put("votingEnabled", true);
        result.put("candidates", candidates);
        result.put("existingVote", existingVote);
        return result;
    }

    private List<Map<String, Object>> buildCandidateList(GameObject game, String excludePlayerId) {
        return game.getPlayerList().stream()
                .filter(p -> p.getStatus() == PlayerStatusEnum.ACTIVE && !p.isDead() && !p.getId().equals(excludePlayerId))
                .map(p -> {
                    Map<String, Object> c = new HashMap<>();
                    c.put("id", p.getId());
                    c.put("name", p.getName());
                    return c;
                })
                .toList();
    }

    private String findExistingVote(String gameCode, String playerId) {
        List<Vote> votes = banishVotes.getOrDefault(gameCode, List.of());
        return votes.stream()
                .filter(v -> v.getCastingPlayer().getId().equals(playerId))
                .findFirst()
                .map(v -> v.getReceivingPlayer().getId())
                .orElse(null);
    }

    public Map<String, Object> castBanishVote(String gameCode, String voterId, String targetId) {
        GameObject game = getGameOrThrow(gameCode);
        Player voter = getPlayerOrThrow(game, voterId);
        Player target = getPlayerOrThrow(game, targetId);

        if (voter.isDead()) {
            throw new IllegalStateException("Dead players cannot vote");
        }
        if (target.isDead()) {
            throw new IllegalArgumentException("Cannot vote for a dead player");
        }
        if (voterId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot vote for yourself");
        }

        List<Vote> votes = banishVotes.computeIfAbsent(gameCode, k -> Collections.synchronizedList(new ArrayList<>()));
        boolean alreadyVoted = votes.stream().anyMatch(v -> v.getCastingPlayer().getId().equals(voterId));
        if (alreadyVoted) {
            throw new IllegalStateException("Already voted this round");
        }

        Vote vote = new Vote(voter, target);
        votes.add(vote);

        Map<String, Object> result = new HashMap<>();
        result.put("voteRecorded", true);
        result.put("castingPlayerId", voterId);
        result.put("targetPlayerId", targetId);
        return result;
    }

    public Map<String, Object> getBanishResult(String gameCode) {
        List<Vote> votes = banishVotes.getOrDefault(gameCode, List.of());

        Map<String, Integer> tally = new LinkedHashMap<>();
        Map<String, String> nameMap = new HashMap<>();
        for (Vote v : votes) {
            String targetId = v.getReceivingPlayer().getId();
            tally.merge(targetId, 1, Integer::sum);
            nameMap.put(targetId, v.getReceivingPlayer().getName());
        }

        String[] winner = findWinner(tally);

        Map<String, Object> result = new HashMap<>();
        result.put("tally", tally);
        result.put("totalVotes", votes.size());
        boolean tie = "true".equals(winner[2]);
        result.put("winnerId", tie ? null : winner[0]);
        result.put("winnerName", tie ? null : nameMap.get(winner[0]));
        result.put("isTie", tie);
        return result;
    }

    private String[] findWinner(Map<String, Integer> tally) {
        String winnerId = null;
        int maxVotes = 0;
        boolean tie = false;
        for (var entry : tally.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winnerId = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true;
            }
        }
        return new String[]{winnerId, String.valueOf(maxVotes), String.valueOf(tie)};
    }

    // --- Murder Vote ---

    public Map<String, Object> getMurderCandidates(String gameCode, String playerId) {
        GameObject game = getGameOrThrow(gameCode);
        Player voter = getPlayerOrThrow(game, playerId);

        if (voter.isDead()) {
            throw new IllegalStateException("Dead players cannot vote");
        }

        List<Map<String, Object>> candidates = game.getPlayerList().stream()
                .filter(p -> p.getStatus() == PlayerStatusEnum.ACTIVE && !p.isDead() && !p.getId().equals(playerId))
                .map(p -> {
                    Map<String, Object> c = new HashMap<>();
                    c.put("id", p.getId());
                    c.put("name", p.getName());
                    return c;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("voteType", "MURDER");
        result.put("votingEnabled", true);
        result.put("candidates", candidates);
        result.put("existingVotes", List.of());
        return result;
    }

    public Map<String, Object> castMurderVote(String gameCode, String voterId, List<String> targetIds) {
        GameObject game = getGameOrThrow(gameCode);
        Player voter = getPlayerOrThrow(game, voterId);

        if (voter.isDead()) {
            throw new IllegalStateException("Dead players cannot vote");
        }

        for (String targetId : targetIds) {
            Player target = getPlayerOrThrow(game, targetId);
            if (voterId.equals(targetId)) {
                throw new IllegalArgumentException("Cannot vote for yourself");
            }
            if (target.isDead()) {
                throw new IllegalArgumentException("Cannot target a dead player");
            }
        }

        Map<String, Object> voteRecord = new HashMap<>();
        voteRecord.put("voterId", voterId);
        voteRecord.put("targetIds", targetIds);

        murderVotes.computeIfAbsent(gameCode, k -> Collections.synchronizedList(new ArrayList<>())).add(voteRecord);

        Map<String, Object> result = new HashMap<>();
        result.put("voteRecorded", true);
        result.put("castingPlayerId", voterId);
        result.put("targetPlayerIds", targetIds);
        return result;
    }

    public void clearVotes(String gameCode) {
        banishVotes.remove(gameCode);
        murderVotes.remove(gameCode);
    }

    private GameObject getGameOrThrow(String gameCode) {
        GameObject game = gameStore.getGame(gameCode);
        if (game == null) {
            throw new IllegalArgumentException("Game not found: " + gameCode);
        }
        return game;
    }

    private Player getPlayerOrThrow(GameObject game, String playerId) {
        Player player = game.findPlayerById(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found");
        }
        return player;
    }
}
