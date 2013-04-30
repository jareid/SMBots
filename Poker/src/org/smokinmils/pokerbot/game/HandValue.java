/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.game;

import org.smokinmils.pokerbot.enums.HandValueType;

/**
* Represents the value of a poker hand.
* 
* @author Jamie Reid
*/
public class HandValue implements Comparable<HandValue> {
 
	/** The hand. */
	private final Hand hand;
	
	/** The maximum number of cards in a hand. 
	public static final int MAX_WIN_CARDS = 5;
	 TODO
	/** Winning cards 
	private Card[] cards = new Card[MAX_WIN_CARDS];*/
	 
	/** The hand value type. */
	private final HandValueType type;
	 
	/** The exact, numeric hand value. */
	private final int value;
	 
	/**
	 * Constructor.
	 * 
	 * @param hand
	 *            The hand.
	 */
	public HandValue(Hand hand) {
		this.hand = hand;
		HandEvaluator evaluator = new HandEvaluator(hand);
		type = evaluator.getType();
		value = evaluator.getValue();
	}
	 
	/**
	 * Returns the hand.
	 * 
	 * @return The hand.
	 */
	public Hand getHand() {
		return hand;
	}
	 
	/**
	 * Returns the hand value type.
	 * 
	 * @return The hand value type.
	 */
	public HandValueType getType() {
		return type;
	}
	 
	/**
	 * Returns a description of the hand value type.
	 * 
	 * @return The description of the hand value type.
	 */
	public String getDescription() {
		return type.getDescription();
	}
	 
	/**
	 * Returns the exact, numeric hand value.
	 * 
	 * @return The exact, numeric hand value.
	 */
	public int getValue() {
		return value;
	}
	 
	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return value;
	}
	 
	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HandValue) {
		    return ((HandValue) obj).getValue() == value;
		} else {
		    return false;
		}
	}
	 
	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("%s", type.getDescription());
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(HandValue handValue) {
		if (value > handValue.getValue()) {
			return -1;
		} else if (value < handValue.getValue()) {
		    return 1;
		} else {
		    return 0;
		}
	}

}
