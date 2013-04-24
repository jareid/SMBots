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
public enum TransactionType {
	ADMIN("admin"),
	BET("bet"),
	CANCEL("cancel"),
	CREDIT("credit"),
	PAYOUT("payout"),
	JACKPOT("pkrjackpot"),
	POKER_BUYIN("pokerbuy"),
	POKER_CASHOUT("pokercash"),
	RESET("reset"),
	TRANSFER("transfer"),
	WIN("win"),
	;
	
	/** The text. */
	private final String text;
	
	/**
	 * Constructor.
	 * 
	 * @param text  textual representation.
	 */
	TransactionType(String text) {
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
	 * Converts a String to the correct TransactionType
	 * 
	 * @param text the string to check
	 * @return the correct enumerate object
	 */
    public static TransactionType fromString(String text) {
        if (text != null) {
        	text = text.toLowerCase();
        	for (TransactionType tt : TransactionType.values()) {
        		if ( tt.getText().compareTo(text) == 0 ) {
        			return tt;
        		}
        	}
        }
        return null;
    }
}
