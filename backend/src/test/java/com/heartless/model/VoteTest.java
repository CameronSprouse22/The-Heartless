package com.heartless.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoteTest {

    @Test
    void createValidVote() {
        Player alice = new Player("Alice", "a@b.com", null);
        Player bob = new Player("Bob", "b@b.com", null);
        Vote vote = new Vote(alice, bob);
        assertEquals(alice, vote.getCastingPlayer());
        assertEquals(bob, vote.getReceivingPlayer());
    }

    @Test
    void selfVoteThrows() {
        Player alice = new Player("Alice", "a@b.com", null);
        assertThrows(IllegalArgumentException.class, () -> new Vote(alice, alice));
    }

    @Test
    void nullCastingPlayerThrows() {
        Player bob = new Player("Bob", "b@b.com", null);
        assertThrows(NullPointerException.class, () -> new Vote(null, bob));
    }

    @Test
    void nullReceivingPlayerThrows() {
        Player alice = new Player("Alice", "a@b.com", null);
        assertThrows(NullPointerException.class, () -> new Vote(alice, null));
    }
}
