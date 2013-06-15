/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.game;

import org.smokinmils.games.casino.poker.enums.HandValueType;

/**
 * Represents the value of a poker hand.
 * 
 * @author Jamie Reid
 */
public class HandValue implements Comparable<HandValue> {

    /** The hand. */
    private final Hand          hand;

    /** The hand value type. */
    private final HandValueType type;

    /** The exact, numeric hand value. */
    private final int           value;

    /**
     * Constructor.
     * 
     * @param hnd The hand.
     */
    public HandValue(final Hand hnd) {
        hand = hnd;
        HandEvaluator evaluator = new HandEvaluator(hand);
        type = evaluator.getType();
        value = evaluator.getValue();
    }

    /**
     * Returns the hand.
     * 
     * @return The hand.
     */
    public final Hand getHand() {
        return hand;
    }

    /**
     * Returns the hand value type.
     * 
     * @return The hand value type.
     */
    public final HandValueType getType() {
        return type;
    }

    /**
     * Returns a description of the hand value type.
     * 
     * @return The description of the hand value type.
     */
    public final String getDescription() {
        return type.getDescription();
    }

    /**
     * Returns the exact, numeric hand value.
     * 
     * @return The exact, numeric hand value.
     */
    public final int getValue() {
        return value;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Object#hashCode()
     * @return the hash code.
     */
    @Override
    public final int hashCode() {
        return value;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Object#equals(java.lang.Object)
     * @param obj the object to compare.
     * @return true if they are equal.
     */
    @Override
    public final boolean equals(final Object obj) {
        if (obj instanceof HandValue) {
            return ((HandValue) obj).getValue() == value;
        } else {
            return false;
        }
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Object#toString()
     * @return the string
     */
    @Override
    public final String toString() {
        return String.format("%s", type.getDescription());
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * @param handValue the hand value.
     * @return an int
     */
    @Override
    public final int compareTo(final HandValue handValue) {
        if (value > handValue.getValue()) {
            return -1;
        } else if (value < handValue.getValue()) {
            return 1;
        } else {
            return 0;
        }
    }

}
