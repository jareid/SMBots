/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.cards;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a deck of cards.
 * 
 * @author Jamie Reid
 */
public class Deck {
    /** Cards in the deck. */
    private static final int   NO_OF_CARDS   = Card.NO_OF_RANKS
                                                     * Card.NO_OF_SUITS;

    /** The deck's cards. */
    private final Card[]       cards;

    /** Index of next card to be dealt. */
    private int                nextCardIndex = 0;

    /** Random number generator. */
    private final SecureRandom secureRandom  = new SecureRandom();

    /**
     * Constructor.
     * 
     * Starts as a full, ordered deck.
     */
    public Deck() {
        cards = new Card[NO_OF_CARDS];
        int index = 0;
        for (int suit = Card.NO_OF_SUITS - 1; suit >= 0; suit--) {
            for (int rank = Card.NO_OF_RANKS - 1; rank >= 0; rank--) {
                cards[index++] = new Card(rank, suit);
            }
        }
    }

    /**
     * Resets the deck.
     * 
     * Does not re-order the cards.
     */
    public final void reset() {
        nextCardIndex = 0;
    }

    /**
     * Shuffles the deck.
     */
    public final void shuffle() {
        for (int oldIndex = 0; oldIndex < NO_OF_CARDS; oldIndex++) {
            int newIndex = secureRandom.nextInt(NO_OF_CARDS);
            Card tempCard = cards[oldIndex];
            cards[oldIndex] = cards[newIndex];
            cards[newIndex] = tempCard;
        }
        nextCardIndex = 0;
    }

    /**
     * Deals a single card.
     * 
     * @return the card dealt
     */
    public final Card deal() {
        if (nextCardIndex + 1 >= NO_OF_CARDS) {
            throw new IllegalStateException("No cards left in deck");
        }
        return cards[nextCardIndex++];
    }

    /**
     * Deals multiple cards at once.
     * 
     * @param numcards How many cards to deal
     * 
     * @return The cards of this hand
     */
    public final List<Card> deal(final int numcards) {
        if (numcards < 1) {
            throw new IllegalArgumentException("noOfCards < 1");
        }
        if (nextCardIndex + numcards >= NO_OF_CARDS) {
            throw new IllegalStateException("No cards left in deck");
        }
        List<Card> dealtCards = new ArrayList<Card>();
        for (int i = 0; i < numcards; i++) {
            dealtCards.add(cards[nextCardIndex++]);
        }
        return dealtCards;
    }
}
