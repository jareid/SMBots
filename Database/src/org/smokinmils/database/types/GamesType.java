/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.database.types;

/**
* An enumerate for the DB's credits transaction types
* 
* @author Jamie Reid
*/
public enum GamesType {
	ADMIN("admin"),
	DICE_DUEL("Dice Duel"),
	OVER_UNDER("Over Under"),
	POKER("Poker"),
	ROULETTE_COLOUR("Roulette Colour"),
	ROULETTE_EVENODD("Roulette Even/Odd"),
	ROULETTE_NUMBER("Roulette Number"),
	ROULETTE_ROW("Roulette Row"),
	;
	
	/** The text. */
	private final String text;
	
	/**
	 * Constructor.
	 * @param text  textual representation.
	 */
	GamesType(String text) {
		this.text = text;
	}
	
	/**
	 * Returns a long textual form of this action.
	 * 
	 * @return The textual representation.
	 */
	public String getText() { return text; }
	
	/**
	 * (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() { return text;	}
	
	/** 
	 * Converts a String to the correct GamesType
	 * 
	 * @param text the string to check
	 * @return the correct enumerate object
	 */
    public static GamesType fromString(String text) {
        if (text != null) {
        	text = text.toLowerCase();
        	for (GamesType gt : GamesType.values()) {
        		if ( gt.getText().compareTo(text) == 0 ) {
        			return gt;
        		}
        	}
        }
        return null;
    }
}
