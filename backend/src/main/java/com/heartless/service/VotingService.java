package com.heartless.service;

import com.heartless.gamethread.GameThread;
import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.UserSelectionsState;
import com.heartless.model.Vote;
import com.heartless.model.enums.PlayerLifeStatusEnum;
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

    // gameCode -> list of banish votes for current round (first vote)
    private final ConcurrentHashMap<String, List<Vote>> banishVotes = new ConcurrentHashMap<>();
    // gameCode -> list of banish votes for tiebreak (second vote)
    private final ConcurrentHashMap<String, List<Vote>> banishSecondVotes = new ConcurrentHashMap<>();
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

        boolean isTieBreak = !game.getTieBreakCandidateIds().isEmpty();
        List<Map<String, Object>> candidates = buildCandidateList(game, playerId);
        String existingVote = findExistingVote(gameCode, playerId, isTieBreak);

        Map<String, Object> result = new HashMap<>();
        result.put("voteType", "BANISH");
        result.put("votingEnabled", true);
        result.put("candidates", candidates);
        result.put("existingVote", existingVote);
        return result;
    }

    private List<Map<String, Object>> buildCandidateList(GameObject game, String excludePlayerId) {
        List<String> tieIds = game.getTieBreakCandidateIds();
        return game.getPlayerList().stream()
                .filter(p -> p.getStatus() == PlayerStatusEnum.ACTIVE && !p.isDead() && !p.getId().equals(excludePlayerId))
                .filter(p -> tieIds.isEmpty() || tieIds.contains(p.getId()))
                .map(p -> {
                    Map<String, Object> c = new HashMap<>();
                    c.put("id", p.getId());
                    c.put("name", p.getName());
                    return c;
                })
                .toList();
    }

    private String findExistingVote(String gameCode, String playerId, boolean isTieBreak) {
        List<Vote> votes = isTieBreak
                ? banishSecondVotes.getOrDefault(gameCode, List.of())
                : banishVotes.getOrDefault(gameCode, List.of());
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

        // Reject votes submitted after the event deadline
        GameThread thread = gameStore.getGameThread(gameCode);
        if (thread != null && thread.getCurrentEvent() != null
                && System.currentTimeMillis() > thread.getCurrentEvent().getEventEndTime()) {
            throw new IllegalStateException("Voting period has ended");
        }

        boolean isTieBreak = !game.getTieBreakCandidateIds().isEmpty();
        if (isTieBreak) {
            // Second (tiebreak) vote — use separate pool so first-round votes don't block it
            List<Vote> secondVotes = banishSecondVotes.computeIfAbsent(gameCode, k -> Collections.synchronizedList(new ArrayList<>()));
            boolean alreadyVoted = secondVotes.stream().anyMatch(v -> v.getCastingPlayer().getId().equals(voterId))
                    || game.getBanishSecondVotes().stream().anyMatch(v -> v.getCastingPlayer().getId().equals(voterId));
            if (alreadyVoted) {
                throw new IllegalStateException("Already voted this round");
            }
            Vote vote = new Vote(voter, target);
            secondVotes.add(vote);
            game.addBanishSecondVote(vote);
        } else {
            List<Vote> votes = banishVotes.computeIfAbsent(gameCode, k -> Collections.synchronizedList(new ArrayList<>()));
            // Check both the service-level list and the game-level list to catch races with resolveEvent
            boolean alreadyVoted = votes.stream().anyMatch(v -> v.getCastingPlayer().getId().equals(voterId))
                    || game.getBanishVotes().stream().anyMatch(v -> v.getCastingPlayer().getId().equals(voterId));
            if (alreadyVoted) {
                throw new IllegalStateException("Already voted this round");
            }
            Vote vote = new Vote(voter, target);
            votes.add(vote);
            game.addBanishVote(vote);
        }

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
                .filter(p -> p.getStatus() == PlayerStatusEnum.ACTIVE && !p.isDead() && !p.getId().equals(playerId) && !p.isTraitor())
                .map(p -> {
                    Map<String, Object> c = new HashMap<>();
                    c.put("id", p.getId());
                    c.put("name", p.getName());
                    return c;
                })
                .toList();

        // Other active traitors (for consensus check on the frontend)
        List<Map<String, Object>> coTraitors = game.getPlayerList().stream()
                .filter(p -> p.isTraitor() && !p.isDead()
                        && !p.getId().equals(playerId)
                        && p.getStatus() == PlayerStatusEnum.ACTIVE)
                .map(p -> {
                    Map<String, Object> t = new HashMap<>();
                    t.put("id", p.getId());
                    t.put("name", p.getName());
                    return t;
                })
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("voteType", "MURDER");
        result.put("votingEnabled", true);
        result.put("candidates", candidates);
        result.put("coTraitors", coTraitors);
        List<Map<String, Object>> existingVoteRecords = murderVotes.getOrDefault(gameCode, List.of());
        List<String> existingVotes = existingVoteRecords.stream()
                .filter(v -> playerId.equals(v.get("voterId")))
                .findFirst()
                .map(v -> (List<String>) v.get("targetIds"))
                .orElse(List.of());
        result.put("existingVotes", existingVotes);
        result.put("othersVotes", getOtherMurderVotes(gameCode, playerId, game));
        return result;
    }

    public Map<String, Object> castMurderVote(String gameCode, String voterId, List<String> targetIds) {
        GameObject game = getGameOrThrow(gameCode);
        Player voter = getPlayerOrThrow(game, voterId);

        if (voter.isDead()) {
            throw new IllegalStateException("Dead players cannot vote");
        }

        // Reject votes submitted after the event deadline
        GameThread murderThread = gameStore.getGameThread(gameCode);
        if (murderThread != null && murderThread.getCurrentEvent() != null
                && System.currentTimeMillis() > murderThread.getCurrentEvent().getEventEndTime()) {
            throw new IllegalStateException("Voting period has ended");
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

        List<Map<String, Object>> votes = murderVotes.computeIfAbsent(gameCode, k -> Collections.synchronizedList(new ArrayList<>()));
        boolean alreadyVoted = votes.stream().anyMatch(v -> voterId.equals(v.get("voterId")));
        if (alreadyVoted) {
            throw new IllegalStateException("Already voted this round");
        }
        votes.add(voteRecord);

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
        banishSecondVotes.remove(gameCode);
        murderVotes.remove(gameCode);
    }

    /**
     * Tallies murder votes for this game and marks the plurality winner as MARKED_FOR_MURDER.
     * Called by MurderVoteEvent when all traitors have submitted their vote.
     * If no votes have been cast this is a no-op (no murder).
     */
    public void applyMurderResult(String gameCode, GameObject game) {
        List<Map<String, Object>> votes = murderVotes.getOrDefault(gameCode, List.of());
        if (votes.isEmpty()) return;

        // Tally votes per target
        Map<String, Integer> tally = new HashMap<>();
        for (Map<String, Object> vote : votes) {
            @SuppressWarnings("unchecked")
            List<String> targetIds = (List<String>) vote.get("targetIds");
            if (targetIds != null) {
                for (String tid : targetIds) {
                    tally.merge(tid, 1, Integer::sum);
                }
            }
        }
        if (tally.isEmpty()) return;

        // Find plurality winner (random pick on tie)
        int maxVotes = tally.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> topIds = tally.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();
        String winnerId = topIds.size() == 1 ? topIds.get(0)
                : topIds.get(new Random().nextInt(topIds.size()));

        Player target = game.findPlayerById(winnerId);
        if (target != null && !target.isDead()) {
            target.setLifeStatus(PlayerLifeStatusEnum.MARKED_FOR_MURDER);
        }
    }

    private List<Map<String, Object>> getOtherMurderVotes(String gameCode, String playerId, GameObject game) {
        List<Map<String, Object>> votes = murderVotes.getOrDefault(gameCode, List.of());
        Set<String> submittedVoterIds = new HashSet<>();
        List<Map<String, Object>> others = new ArrayList<>();
        for (Map<String, Object> v : votes) {
            if (!playerId.equals(v.get("voterId"))) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("voterId", v.get("voterId"));
                entry.put("voterName", v.get("voterName"));
                entry.put("targetIds", v.get("targetIds"));
                entry.put("targetNames", v.get("targetNames"));
                entry.put("submitted", true);
                others.add(entry);
                submittedVoterIds.add((String) v.get("voterId"));
            }
        }
        // Include live (not yet submitted) selections from UserSelectionsState for other traitors
        for (Player p : game.getPlayerList()) {
            if (!p.isTraitor() || p.isDead() || p.getId().equals(playerId)
                    || p.getStatus() != PlayerStatusEnum.ACTIVE) continue;
            if (submittedVoterIds.contains(p.getId())) continue;
            UserSelectionsState sel = game.getSelectionState(p.getId());
            if (sel != null && !sel.getSelectedItems().isEmpty()) {
                List<String> targetIds = new ArrayList<>(sel.getSelectedItems());
                List<String> targetNames = targetIds.stream()
                        .map(tid -> {
                            Player t = game.findPlayerById(tid);
                            return t != null ? t.getName() : tid;
                        })
                        .toList();
                Map<String, Object> entry = new HashMap<>();
                entry.put("voterId", p.getId());
                entry.put("voterName", p.getName());
                entry.put("targetIds", targetIds);
                entry.put("targetNames", targetNames);
                entry.put("submitted", false);
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
