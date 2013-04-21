/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.enums;

import org.smokinmils.pokerbot.settings.DBSettings;

/**
* An enumerate for the DB's credits transaction types
* 
* @author Jamie Reid
*/
public enum TransactionType {
	BUYIN(DBSettings.PokerBuyIn),
	CASHOUT(DBSettings.PokerCashOut),
	ADMIN(DBSettings.TransAdmin),
	JACKPOT(DBSettings.PokerJackpot)
	;
	
	/** The text. */
	private final String text;
	
	/**
	 * Constructor.
	 * 
	 * @param name  The name.
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
}
