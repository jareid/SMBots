/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.game;

import java.util.Collection;

import org.smokinmils.games.casino.carddeck.Card;
import org.smokinmils.logging.EventLog;

/**
 * A class to represent a poker hand.
 * 
 * @author Jamie
 */
public class Hand {
    /** The maximum number of cards in a hand. */
    public static final int MAX_CARDS = 7;

    /** The cards in this hand. */
    private final Card[]    cards     = new Card[MAX_CARDS];

    /** The current number of cards in this hand. */
    private int             noOfCards = 0;

    /**
     * Constructor for an empty hand.
     */
    public Hand() {

    }

    /**
     * Constructor with an array of initial cards.
     * 
     * @param crds The initial cards.
     */
    public Hand(final Card[] crds) {
        try {
            addCards(crds);
        } catch (IllegalArgumentException e) {
            EventLog.fatal(e, "Hand", "Constructor");
            System.exit(1);
        }
    }

    /**
     * Constructor with a collection of initial cards.
     * 
     * @param crds The initial cards.
     */
    public Hand(final Collection<Card> crds) {
        if (cards == null) {
            throw new IllegalArgumentException("Null array");
        }

        for (Card card : crds) {
            try {
                addCard(card);
            } catch (IllegalArgumentException e) {
                EventLog.fatal(e, "Hand", "Constructor");
                System.exit(1);
            }
        }
    }

    /**
     * This method returns the number of cards.
     * 
     * @return The number of cards.
     */
    public final int size() {
        return noOfCards;
    }

    /**
     * Adds a single card.
     * 
     * The card is inserted at such a position that the hand remains sorted
     * (highest ranking cards first).
     * 
     * @param card The card to add.
     */
    public final void addCard(final Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Null card");
        }

        int insertIndex = -1;
        for (int i = 0; i < noOfCards; i++) {
            if (card.compareTo(cards[i]) > 0) {
                insertIndex = i;
                break;
            }
        }
        if (insertIndex == -1) {
            // Could not insert anywhere, so append at the end.
            cards[noOfCards++] = card;
        } else {
            for (int i = noOfCards; i > insertIndex; i--) {
                cards[i] = cards[i - 1];
            }
            cards[insertIndex] = card;
            noOfCards++;
        }
    }

    /**
     * Adds multiple cards, sorting as cards are added.
     * 
     * @param crds The cards to add.
     */
    public final void addCards(final Card[] crds) {
        if (crds == null) {
            throw new IllegalArgumentException("Null array");
        }

        if (crds.length > MAX_CARDS) {
            throw new IllegalArgumentException("Too many cards");
        }

        for (Card card : crds) {
            try {
                addCard(card);
            } catch (IllegalArgumentException e) {
                EventLog.fatal(e, "Hand", "addCards");
                System.exit(1);
            }
        }
    }

    /**
     * Adds multiple cards, sorting as cards are added.
     * 
     * @param crds The cards to add.
     */
    public final void addCards(final Collection<Card> crds) {
        if (crds == null) {
            throw new IllegalArgumentException("Null collection");
        }
        if (crds.size() > MAX_CARDS) {
            throw new IllegalArgumentException("Too many cards");
        }
        for (Card card : crds) {
            try {
                addCard(card);
            } catch (IllegalArgumentException e) {
                EventLog.fatal(e, "Hand", "addCards");
                System.exit(1);
            }
        }
    }

    /**
     * This method returns the cards.
     * 
     * @return The cards.
     */
    public final Card[] getCards() {
        Card[] dest = new Card[noOfCards];
        System.arraycopy(cards, 0, dest, 0, noOfCards);
        return dest;
    }

    /**
     * This method removes all cards.
     */
    public final void removeAllCards() {
        noOfCards = 0;
    }

}
