/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.database.types;

/**
 * An enumerate for the DB's credits transaction types.
 * 
 * @author Jamie Reid
 */
public enum GamesType {
    /** Admin game. */
    ADMIN("admin"),
    
    /** Auction game. */
    AUCTION("auction"),
    
    /** Blackjack game. */
    BLACKJACK("Blackjack"),
    
    /** Competitions. */
    COMPETITIONS("competitions"),
    
    /** DD game. */
    DICE_DUEL("Dice Duel"),
    
    /** Blackjack game. */
    DM("iDM"),
    
    /** Lottery game. */
    LOTTERY("Lottery"),
    
    /** OU game. */
    OVER_UNDER("Over Under"),
    
    /** Poker game. */
    POKER("Poker"),
    
    /** Roulette game. */
    ROULETTE("Roulette"),
    
    /** Rock Paper Scissors game. */
    ROCKPAPERSCISSORS("Rock Paper Scissors"),
    
    /** Timed Roll game. */
    TIMEDROLL("Timed Roll"),
    
    /**
     * Other games. Used for historical reasons
     */
    OTHER("other");

    /** The text. */
    private final String text;

    /**
     * Constructor.
     * @param txt textual representation.
     */
    GamesType(final String txt) {
        text = txt;
    }

    /**
     * Returns a long textual form of this action.
     * 
     * @return The textual representation.
     */
    public String getText() {
        return text;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Enum#toString()
     * @return the output
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Converts a String to the correct GamesType.
     * 
     * @param text the string to check
     * @return the correct enumerate object
     */
    public static GamesType fromString(final String text) {
        if (text != null) {
            for (GamesType gt : GamesType.values()) {
                if (gt.getText().equals(text)) {
                    return gt;
                }
            }
        }
        return OTHER;
    }
}
