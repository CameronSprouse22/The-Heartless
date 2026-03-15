package com.heartless.model;

import com.heartless.model.enums.CardNumber;
import com.heartless.model.enums.CardSuit;

import java.util.Objects;

/**
 * Immutable value object representing a playing card.
 * Two cards are equal if they share the same suit and number.
 */
public final class Card {

    private final CardSuit suit;
    private final CardNumber number;

    public Card(CardSuit suit, CardNumber number) {
        Objects.requireNonNull(suit, "Suit must not be null");
        Objects.requireNonNull(number, "Number must not be null");
        this.suit = suit;
        this.number = number;
    }

    public CardSuit getSuit() {
        return suit;
    }

    public CardNumber getNumber() {
        return number;
    }

    public String getImageUrl() {
        String suitName = suit.name().toLowerCase();
        String numberName = number.name().toLowerCase();
        return "/cards/" + suitName + "-" + numberName + ".png";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return suit == card.suit && number == card.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, number);
    }

    @Override
    public String toString() {
        return number + " of " + suit;
    }
}
