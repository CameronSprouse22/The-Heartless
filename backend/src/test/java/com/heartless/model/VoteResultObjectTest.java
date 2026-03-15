package com.heartless.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VoteResultObjectTest {

    private Player player(String name) {
        return new Player(name, name.toLowerCase() + "@test.com", null);
    }

    @Test
    void tallyCountsVotes() {
        Player alice = player("Alice");
        Player bob = player("Bob");
        Player charlie = player("Charlie");

        List<Vote> votes = List.of(
                new Vote(alice, bob),
                new Vote(charlie, bob)
        );
        VoteResultObject result = new VoteResultObject(votes);

        Map<Player, Integer> tally = result.getTally();
        assertEquals(1, tally.size());
        assertEquals(2, tally.get(bob));
    }

    @Test
    void winnerIsPlayerWithMostVotes() {
        Player alice = player("Alice");
        Player bob = player("Bob");
        Player charlie = player("Charlie");

        List<Vote> votes = List.of(
                new Vote(alice, bob),
                new Vote(charlie, bob),
                new Vote(bob, alice)
        );
        VoteResultObject result = new VoteResultObject(votes);
        assertEquals(bob, result.getWinner());
        assertFalse(result.isTie());
    }

    @Test
    void tieDetected() {
        Player alice = player("Alice");
        Player bob = player("Bob");
        Player charlie = player("Charlie");
        Player dave = player("Dave");

        List<Vote> votes = List.of(
                new Vote(alice, bob),
                new Vote(charlie, dave),
                new Vote(bob, dave),
                new Vote(dave, bob)
        );
        VoteResultObject result = new VoteResultObject(votes);
        assertTrue(result.isTie());
        assertNull(result.getWinner());
    }

    @Test
    void emptyVoteListIsNotTie() {
        VoteResultObject result = new VoteResultObject(List.of());
        assertFalse(result.isTie());
        assertNull(result.getWinner());
    }

    @Test
    void singleVoteHasWinner() {
        Player alice = player("Alice");
        Player bob = player("Bob");
        VoteResultObject result = new VoteResultObject(List.of(new Vote(alice, bob)));
        assertEquals(bob, result.getWinner());
        assertFalse(result.isTie());
    }

    @Test
    void nullVoteListBecomesEmpty() {
        VoteResultObject result = new VoteResultObject(null);
        assertEquals(0, result.getVoteList().size());
    }
}
