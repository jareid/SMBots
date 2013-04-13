/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.game;

import org.smokinmils.pokerbot.settings.Strings;

/**
 * Represents a card
 * 
 * @author Jamie Reid
 */
public class Card implements Comparable<Card> {
	/** Constants */
	public static final int NO_OF_RANKS = 13;
	public static final int NO_OF_SUITS = 4;

    /** The ranks. */
    public static final int ACE      = 12;
    public static final int KING     = 11;
    public static final int QUEEN    = 10;
    public static final int JACK     = 9;
    public static final int TEN      = 8;
    public static final int NINE     = 7;
    public static final int EIGHT    = 6;
    public static final int SEVEN    = 5;
    public static final int SIX      = 4;
    public static final int FIVE     = 3;
    public static final int FOUR     = 2;
    public static final int THREE    = 1;
    public static final int DEUCE    = 0;
    
    /** The suits. */
    public static final int SPADES   = 3;
    public static final int HEARTS   = 2;
    public static final int CLUBS    = 1;
    public static final int DIAMONDS = 0;
	
    /** The string form of the ranks. */
	private static final String[] RANKS = {" 2", " 3", " 4", " 5",
										    " 6", " 7", " 8", " 9",
										    "10", " J", " Q", " K", " A"};
	
    /** The long string form of the ranks. */
	private static final String[] RANKS_LONG = {"Two", "Three", "Four", "Five",
												 "Six", "Seven", "Eight", "Nine",
												 "Ten", "Jack", "Queen", "King", "Ace"};
	
    /** The string form of the suits. */
	private static final String[] SUITS = {Strings.CardText_Diamonds, Strings.CardText_Clubs,
										   Strings.CardText_Hearts, Strings.CardText_Spades};
	private static final String[] SUITS_COLOURS = {Strings.CardColours_Diamonds, Strings.CardColours_Clubs,
		   											Strings.CardColours_Hearts, Strings.CardColours_Spades};

    /** The long string form of the suits. */
	private static final String[] SUITS_LONG = { "Diamonds", "Clubs", "Hearts", "Spades"};
	
	/** The card's rank */
	private int rankID;
	
	/** The card's suit */
	private int suitID;
	
    /**
     * Constructor using rank and suit.
     * 
     * @param rank The rank.
     * @param suit The suit.
     * 
     * @throws IllegalArgumentException
     *         If the rank or suit is invalid.
     */
	public Card(int rank, int suit) throws IllegalArgumentException {
		if (rank < 0 || rank > 12)
			throw new IllegalArgumentException("Invalid rank given, should be 0 to 12. Received: " + rank);
		else if (suit < 0 || suit > 3)
			throw new IllegalArgumentException("Invalid suit given, should be 0 to 3. Received: " + suit);
		else {
			rankID = rank;
			suitID = suit;
		}
		
	}
	
    /**
     * Returns the rank.
     * 
     * @return The rank.
     */
	public int getRank() { return rankID; }
	
    /**
     * Returns the rank string
     * 
     * @return The rank.
     */
	public String getRankStr() { return RANKS[rankID-1]; }
	
    /**
     *  Returns the long version of the rank.
     * 
     * @return The rank.
     */
	public String getLongRank() { return RANKS_LONG[rankID-1]; }
	
    /**
     * Returns the suit 
     * 
     * @return The suit.
     */
	public int getSuit() { return suitID; }
	
    /**
     * Returns the suit string
     * 
     * @return The suit.
     */
	public String getSuitStr() { return SUITS[suitID]; }
	
    /**
     * Returns the long version of the suit.
     * 
     * @return The suit.
     */
	public String getLongSuit() { return SUITS_LONG[suitID]; }
	
	/**
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (rankID + (suitID * NO_OF_SUITS));
    }

    /**
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
    	boolean ret = false;
        if (obj instanceof Card) {
            return ((Card) obj).hashCode() == this.hashCode();
        }
    	return ret;
    }

    /**
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Card card) {
        int value = hashCode();
        int other = card.hashCode();
        if (value < other) {
            return -1;
        } else if (value > other) {
            return 1;
        } else {
            return 0;
        }
    }
    
	/**
	 * Converts the Card to a useful string for IRC
	 */	
	public String toIRCString() {
    	String out = "%n" + Strings.CardText.replaceAll("%rank", RANKS[rankID]);
    	out = out.replaceAll("%suitC", "%c" + SUITS_COLOURS[suitID]);
    	out = out.replaceAll("%suit", SUITS[suitID]);
		return out;
	}
    
    /**
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
	public String toString() {
		return RANKS[rankID] + SUITS[suitID];
	}
}
