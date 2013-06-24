/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.cards;

import org.smokinmils.settings.PokerStrs;

/**
 * Represents a card.
 * 
 * @author Jamie Reid
 */
public class Card implements Comparable<Card> {
    /** Number of Ranks. */
    public static final int       NO_OF_RANKS   = 13;

    /** Number of Suits. */
    public static final int       NO_OF_SUITS   = 4;

    /** The Ace rank. */
    public static final int       ACE           = 12;

    /** The King rank. */
    public static final int       KING          = 11;

    /** The Queen rank. */
    public static final int       QUEEN         = 10;

    /** The Jack rank. */
    public static final int       JACK          = 9;

    /** The 10 rank. */
    public static final int       TEN           = 8;

    /** The 9 rank. */
    public static final int       NINE          = 7;

    /** The 8 rank. */
    public static final int       EIGHT         = 6;

    /** The 7 rank. */
    public static final int       SEVEN         = 5;

    /** The 6 rank. */
    public static final int       SIX           = 4;

    /** The 5 rank. */
    public static final int       FIVE          = 3;

    /** The 4 rank. */
    public static final int       FOUR          = 2;

    /** The 3 rank. */
    public static final int       THREE         = 1;

    /** The 2 rank. */
    public static final int       DEUCE         = 0;

    /** The Spades suit. */
    public static final int       SPADES        = 3;

    /** The Hearts suit. */
    public static final int       HEARTS        = 2;

    /** The Clubs suit. */
    public static final int       CLUBS         = 1;

    /** The Diamonds suit. */
    public static final int       DIAMONDS      = 0;

    /** The string form of the ranks. */
    private static final String[] RANKS         = { " 2", " 3", " 4", " 5",
                                                " 6", " 7", " 8", " 9", "10",
                                                " J", " Q", " K", " A" };

    /** The long string form of the ranks. */
    private static final String[] RANKS_LONG    = { "Two", "Three", "Four",
                                                "Five", "Six", "Seven",
                                                "Eight", "Nine", "Ten", "Jack",
                                                "Queen",
                                                "King", "Ace" };

    /** The string form of the suits. */
    private static final String[] SUITS         = {
                                                PokerStrs.CardText_Diamonds,
                                                PokerStrs.CardText_Clubs,
                                                PokerStrs.CardText_Hearts,
                                                PokerStrs.CardText_Spades };

    /** The string form of the suit's colours. */
    private static final String[] SUITS_COLOURS = {
                                                PokerStrs.CardColours_Diamonds,
                                                PokerStrs.CardColours_Clubs,
                                                PokerStrs.CardColours_Hearts,
                                                PokerStrs.CardColours_Spades };

    /** The long string form of the suits. */
    private static final String[] SUITS_LONG    = { "Diamonds", "Clubs",
                                                "Hearts", "Spades" };

    /** The card's rank. */
    private int                   rankID;

    /** The card's suit. */
    private int                   suitID;

    /**
     * Constructor using rank and suit.
     * 
     * @param rank The rank.
     * @param suit The suit.
     */
    public Card(final int rank, final int suit) {
        if (rank < DEUCE || rank > ACE) {
            throw new IllegalArgumentException(
                    "Invalid rank given, should be 0 to 12. Received: " + rank);
        } else if (suit < DIAMONDS || suit > SPADES) {
            throw new IllegalArgumentException(
                    "Invalid suit given, should be 0 to 3. Received: " + suit);
        } else {
            rankID = rank;
            suitID = suit;
        }

    }

    /**
     * Returns the rank.
     * 
     * @return The rank.
     */
    public final int getRank() {
        return rankID;
    }

    /**
     * Returns the rank string.
     * 
     * @return The rank.
     */
    public final String getRankStr() {
        return RANKS[rankID - 1];
    }

    /**
     * Returns the long version of the rank.
     * 
     * @return The rank.
     */
    public final String getLongRank() {
        return RANKS_LONG[rankID - 1];
    }

    /**
     * Returns the suit.
     * 
     * @return The suit.
     */
    public final int getSuit() {
        return suitID;
    }

    /**
     * Returns the suit string.
     * 
     * @return The suit.
     */
    public final String getSuitStr() {
        return SUITS[suitID];
    }

    /**
     * Returns the long version of the suit.
     * 
     * @return The suit.
     */
    public final String getLongSuit() {
        return SUITS_LONG[suitID];
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Object#hashCode()
     * @return the hash code.
     */
    @Override
    public final int hashCode() {
        return (rankID + (suitID * NO_OF_SUITS));
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Object#equals(java.lang.Object)
     * @param obj an object to compare for equality against.
     * @return true if the objects are equal.
     */
    @Override
    public final boolean equals(final Object obj) {
        boolean ret = false;
        if (obj instanceof Card) {
            return ((Card) obj).hashCode() == this.hashCode();
        }
        return ret;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * @param card The card to compare this object to.
     * @return 0 if equal, -1 if this is lower than right, +1 otherwise.
     */
    @Override
    public final int compareTo(final Card card) {
        int value = hashCode();
        int other = card.hashCode();

        int ret = 0;
        if (value < other) {
            ret = -1;
        } else if (value > other) {
            ret = 1;
        }

        return ret;
    }

    /**
     * Converts the Card to a useful string for IRC.
     * 
     * @return the IRC formatted string.
     */
    public final String toIRCString() {
        String out = "%n"
                + PokerStrs.CardText.replaceAll("%rank", RANKS[rankID]);
        out = out.replaceAll("%suitC", "%c" + SUITS_COLOURS[suitID]);
        out = out.replaceAll("%suit", SUITS[suitID]);
        return out;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Object#toString()
     * @return the string.
     */
    @Override
    public final String toString() {
        return RANKS[rankID] + SUITS[suitID];
    }
}
