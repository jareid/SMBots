/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.enums;

/**
 * The types of hands in poker.
 * 
 * @author Jamie Reid
 */
public enum HandValueType {
    /** Highest Card. */
    HIGH_CARD("High Card", 0),

    /** One Pair. */
    ONE_PAIR("One Pair", 1),

    /** Two Pairs. */
    TWO_PAIRS("Two Pairs", 2),

    /** Three of a Kind. */
    THREE_OF_A_KIND("Three of a Kind", 3),

    /** Straight. */
    STRAIGHT("Straight", 4),

    /** Flush. */
    FLUSH("Flush", 5),

    /** Full House. */
    FULL_HOUSE("Full House", 6),

    /** Four of a Kind. */
    FOUR_OF_A_KIND("Four of a Kind", 7),

    /** Straight Flush. */
    STRAIGHT_FLUSH("Straight Flush", 8),

    /** Royal flush. */
    ROYAL_FLUSH("Royal Flush", 9);

    /** The description. */
    private String description;

    /** The hand value. */
    private int    value;

    /**
     * Constructor.
     * 
     * @param descr The description.
     * @param val The hand value.
     */
    HandValueType(final String descr, final int val) {
        description = descr;
        value = val;
    }

    /**
     * Returns the description.
     * 
     * @return Description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the hand value.
     * 
     * @return Hand value.
     */
    public int getValue() {
        return value;
    }
}
