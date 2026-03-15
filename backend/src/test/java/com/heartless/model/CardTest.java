package com.heartless.model;

import com.heartless.model.enums.CardNumber;
import com.heartless.model.enums.CardSuit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void equalCardsAreEqual() {
        Card card1 = new Card(CardSuit.HEART, CardNumber.QUEEN);
        Card card2 = new Card(CardSuit.HEART, CardNumber.QUEEN);
        assertEquals(card1, card2);
        assertEquals(card1.hashCode(), card2.hashCode());
    }

    @Test
    void differentCardsAreNotEqual() {
        Card card1 = new Card(CardSuit.HEART, CardNumber.QUEEN);
        Card card2 = new Card(CardSuit.SPADE, CardNumber.QUEEN);
        assertNotEquals(card1, card2);
    }

    @Test
    void differentNumbersAreNotEqual() {
        Card card1 = new Card(CardSuit.HEART, CardNumber.QUEEN);
        Card card2 = new Card(CardSuit.HEART, CardNumber.KING);
        assertNotEquals(card1, card2);
    }

    @Test
    void cardIsImmutable() {
        Card card = new Card(CardSuit.DIAMOND, CardNumber.ACE);
        assertEquals(CardSuit.DIAMOND, card.getSuit());
        assertEquals(CardNumber.ACE, card.getNumber());
    }

    @Test
    void nullSuitThrows() {
        assertThrows(NullPointerException.class, () -> new Card(null, CardNumber.ACE));
    }

    @Test
    void nullNumberThrows() {
        assertThrows(NullPointerException.class, () -> new Card(CardSuit.HEART, null));
    }

    @Test
    void imageUrlIsCorrect() {
        Card card = new Card(CardSuit.HEART, CardNumber.QUEEN);
        assertEquals("/cards/heart-queen.png", card.getImageUrl());
    }
}
