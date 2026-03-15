package com.heartless.service;

import com.heartless.model.Card;
import com.heartless.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CardAssignmentServiceTest {

    private final CardAssignmentService service = new CardAssignmentService();

    @Test
    void generateShuffledDeckHas52Cards() {
        List<Card> deck = service.generateShuffledDeck();
        assertEquals(52, deck.size());
    }

    @Test
    void generateShuffledDeckAllUnique() {
        List<Card> deck = service.generateShuffledDeck();
        Set<Card> uniqueCards = new HashSet<>(deck);
        assertEquals(52, uniqueCards.size());
    }

    @Test
    void generateShuffledDeckIsShuffled() {
        List<Card> deck1 = service.generateShuffledDeck();
        List<Card> deck2 = service.generateShuffledDeck();
        // Extremely unlikely to be equal after two shuffles
        assertNotEquals(deck1, deck2);
    }

    @Test
    void assignCardsMatchesPlayerCount() {
        List<Player> players = createPlayers(10);
        Map<Player, Card> assignments = service.assignCards(players);
        assertEquals(10, assignments.size());
    }

    @Test
    void assignCardsNoDuplicates() {
        List<Player> players = createPlayers(20);
        Map<Player, Card> assignments = service.assignCards(players);
        Set<Card> assignedCards = new HashSet<>(assignments.values());
        assertEquals(20, assignedCards.size());
    }

    @Test
    void assignCardsEveryPlayerGetsCard() {
        List<Player> players = createPlayers(5);
        Map<Player, Card> assignments = service.assignCards(players);
        for (Player p : players) {
            assertNotNull(assignments.get(p));
        }
    }

    @Test
    void assignCardsMax52Players() {
        List<Player> players = createPlayers(52);
        Map<Player, Card> assignments = service.assignCards(players);
        assertEquals(52, assignments.size());
    }

    @Test
    void assignCardsRejectsOver52Players() {
        List<Player> players = createPlayers(53);
        assertThrows(IllegalArgumentException.class,
                () -> service.assignCards(players));
    }

    private List<Player> createPlayers(int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            players.add(new Player("Player" + i, "p" + i + "@test.com", null));
        }
        return players;
    }
}
