package com.heartless.service;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.Vote;
import com.heartless.model.enums.PlayerStatusEnum;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages banish and murder votes per game per round.
 */
@Service
public class VotingService {

    private final GameStore gameStore;
    private final SimpMessagingTemplate messagingTemplate;

    // gameCode -> list of banish votes for current round
    private final ConcurrentHashMap<String, List<Vote>> banishVotes = new ConcurrentHashMap<>();
    // gameCode -> list of murder votes for current round
    private final ConcurrentHashMap<String, List<Map<String, Object>>> murderVotes = new ConcurrentHashMap<>();

    public VotingService(GameStore gameStore, SimpMessagingTemplate messagingTemplate) {
        this.gameStore = gameStore;
        this.messagingTemplate = messagingTemplate;
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

        // Track submit in per-player selection state
        UserSelectionsState selState = game.getSelectionState(voterId);
        if (selState != null) { selState.setSubmitPressed(true); }

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
        result.put("othersVotes", getOtherMurderVotes(gameCode, playerId, game));
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
        voteRecord.put("voterName", voter.getName());
        voteRecord.put("targetIds", targetIds);

        List<String> targetNames = targetIds.stream()
                .map(tid -> {
                    Player t = game.findPlayerById(tid);
                    return t != null ? t.getName() : tid;
                })
                .toList();
        voteRecord.put("targetNames", targetNames);

        murderVotes.computeIfAbsent(gameCode, k -> Collections.synchronizedList(new ArrayList<>())).add(voteRecord);

        // Track submit in per-player selection state
        UserSelectionsState selState = game.getSelectionState(voterId);
        if (selState != null) { selState.setSubmitPressed(true); }

        // Broadcast to other traitors via WebSocket
        if (messagingTemplate != null) {
            Map<String, Object> broadcast = new HashMap<>();
            broadcast.put("type", "MURDER_VOTE_UPDATE");
            broadcast.put("voterId", voterId);
            broadcast.put("voterName", voter.getName());
            broadcast.put("targetIds", targetIds);
            broadcast.put("targetNames", targetNames);
            messagingTemplate.convertAndSend("/topic/games/" + gameCode + "/murder-vote", broadcast);
        }

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

    private List<Map<String, Object>> getOtherMurderVotes(String gameCode, String playerId, GameObject game) {
        List<Map<String, Object>> votes = murderVotes.getOrDefault(gameCode, List.of());
        List<Map<String, Object>> others = new ArrayList<>();
        for (Map<String, Object> v : votes) {
            if (!playerId.equals(v.get("voterId"))) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("voterId", v.get("voterId"));
                entry.put("voterName", v.get("voterName"));
                entry.put("targetIds", v.get("targetIds"));
                entry.put("targetNames", v.get("targetNames"));
                others.add(entry);
            }
        }
        return others;
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
