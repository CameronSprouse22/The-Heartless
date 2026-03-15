package com.heartless.service;

import com.heartless.model.Card;
import com.heartless.model.Player;
import com.heartless.model.enums.CardNumber;
import com.heartless.model.enums.CardSuit;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Generates a shuffled 52-card deck and assigns unique cards to players.
 */
@Service
public class CardAssignmentService {

    /**
     * Generate a full 52-card deck and shuffle it.
     */
    public List<Card> generateShuffledDeck() {
        List<Card> deck = new ArrayList<>(52);
        for (CardSuit suit : CardSuit.values()) {
            for (CardNumber number : CardNumber.values()) {
                deck.add(new Card(suit, number));
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    /**
     * Assign a unique card to each player from a shuffled deck.
     * Players list must not exceed 52.
     */
    public Map<Player, Card> assignCards(List<Player> players) {
        if (players.size() > 52) {
            throw new IllegalArgumentException("Cannot assign cards to more than 52 players");
        }
        List<Card> deck = generateShuffledDeck();
        Map<Player, Card> assignments = new LinkedHashMap<>();
        for (int i = 0; i < players.size(); i++) {
            assignments.put(players.get(i), deck.get(i));
        }
        return assignments;
    }
}
