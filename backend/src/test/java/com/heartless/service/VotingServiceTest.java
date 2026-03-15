package com.heartless.service;

import com.heartless.model.GameObject;
import com.heartless.model.Player;
import com.heartless.model.Vote;
import com.heartless.model.enums.PlayerStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VotingServiceTest {

    private VotingService votingService;
    private GameStore gameStore;
    private GameObject game;
    private Player player1;
    private Player player2;
    private Player player3;
    private Player deadPlayer;

    @BeforeEach
    void setUp() {
        gameStore = new GameStore();
        votingService = new VotingService(gameStore);

        game = new GameObject("VOTE01");
        game.transitionToStart(); // game must be START to vote

        player1 = new Player("Alice", "alice@test.com", null);
        player1.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(player1);

        player2 = new Player("Bob", "bob@test.com", null);
        player2.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(player2);

        player3 = new Player("Charlie", "charlie@test.com", null);
        player3.setStatus(PlayerStatusEnum.ACTIVE);
        game.addPlayer(player3);

        deadPlayer = new Player("Dead", "dead@test.com", null);
        deadPlayer.setStatus(PlayerStatusEnum.ACTIVE);
        deadPlayer.setDead(true);
        game.addPlayer(deadPlayer);

        gameStore.putGame("VOTE01", game);
    }

    // Banish vote tests - T063
    @Test
    void getBanishCandidatesExcludesSelf() {
        var result = votingService.getBanishCandidates("VOTE01", player1.getId());
        List<?> candidates = (List<?>) result.get("candidates");
        // Should not include player1 (self) or deadPlayer
        assertEquals(2, candidates.size());
    }

    @Test
    void getBanishCandidatesExcludesDead() {
        var result = votingService.getBanishCandidates("VOTE01", player1.getId());
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
        boolean containsDead = candidates.stream()
                .anyMatch(c -> c.get("id").equals(deadPlayer.getId()));
        assertFalse(containsDead);
    }

    @Test
    void castBanishVoteSuccess() {
        var result = votingService.castBanishVote("VOTE01", player1.getId(), player2.getId());
        assertEquals(true, result.get("voteRecorded"));
        assertEquals(player1.getId(), result.get("castingPlayerId"));
        assertEquals(player2.getId(), result.get("targetPlayerId"));
    }

    @Test
    void castBanishVoteRejectsSelfVote() {
        assertThrows(IllegalArgumentException.class,
                () -> votingService.castBanishVote("VOTE01", player1.getId(), player1.getId()));
    }

    @Test
    void castBanishVoteRejectsDuplicateVote() {
        votingService.castBanishVote("VOTE01", player1.getId(), player2.getId());
        assertThrows(IllegalStateException.class,
                () -> votingService.castBanishVote("VOTE01", player1.getId(), player3.getId()));
    }

    @Test
    void castBanishVoteRejectsDeadVoter() {
        assertThrows(IllegalStateException.class,
                () -> votingService.castBanishVote("VOTE01", deadPlayer.getId(), player1.getId()));
    }

    @Test
    void castBanishVoteRejectsDeadTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> votingService.castBanishVote("VOTE01", player1.getId(), deadPlayer.getId()));
    }

    @Test
    void banishVoteTally() {
        votingService.castBanishVote("VOTE01", player1.getId(), player2.getId());
        votingService.castBanishVote("VOTE01", player2.getId(), player3.getId());
        votingService.castBanishVote("VOTE01", player3.getId(), player2.getId());

        var result = votingService.getBanishResult("VOTE01");
        // player2 has 2 votes, player3 has 1
        assertNotNull(result.get("tally"));
    }

    // Murder vote tests - T069
    @Test
    void getMurderCandidatesTraitorOnly() {
        player1.setTraitor(true);
        var result = votingService.getMurderCandidates("VOTE01", player1.getId());
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) result.get("candidates");
        // Should show non-traitor, alive players: player2, player3 (not deadPlayer, not player1)
        assertEquals(2, candidates.size());
        boolean containsTraitor = candidates.stream()
                .anyMatch(c -> c.get("id").equals(player1.getId()));
        assertFalse(containsTraitor);
    }

    @Test
    void getMurderCandidatesRejectsNonTraitor() {
        assertThrows(SecurityException.class,
                () -> votingService.getMurderCandidates("VOTE01", player2.getId()));
    }

    @Test
    void castMurderVoteSuccess() {
        player1.setTraitor(true);
        var result = votingService.castMurderVote("VOTE01", player1.getId(), List.of(player2.getId()));
        assertEquals(true, result.get("voteRecorded"));
    }

    @Test
    void castMurderVoteRejectsNonTraitor() {
        assertThrows(SecurityException.class,
                () -> votingService.castMurderVote("VOTE01", player2.getId(), List.of(player3.getId())));
    }

    @Test
    void castMurderVoteRejectsTraitorTarget() {
        player1.setTraitor(true);
        player2.setTraitor(true);
        assertThrows(IllegalArgumentException.class,
                () -> votingService.castMurderVote("VOTE01", player1.getId(), List.of(player2.getId())));
    }

    @Test
    void castMurderVoteRejectsDeadTarget() {
        player1.setTraitor(true);
        assertThrows(IllegalArgumentException.class,
                () -> votingService.castMurderVote("VOTE01", player1.getId(), List.of(deadPlayer.getId())));
    }

    @Test
    void clearVotesRemovesAll() {
        votingService.castBanishVote("VOTE01", player1.getId(), player2.getId());
        votingService.clearVotes("VOTE01");
        var result = votingService.getBanishResult("VOTE01");
        assertEquals(0, result.get("totalVotes"));
    }
}
