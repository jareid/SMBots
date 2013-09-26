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
* An enumerate for the DB's credits transaction types.
* 
* @author Jamie Reid
*/
public enum TransactionType {
    /** admin transactions. */
	ADMIN("admin"),
	
    /** bet transactions. */
	BET("bet"),
	
	/** bid transactions. */
	AUCTION_BID("auctionbid"),
	
	/** pay for auction. */
	AUCTION_PAY("auctionpay"),

    /** escrow transaction.*/
    COMMISSION("commission"),

    /** escrow transaction.*/
    ESCROW("escrow"),
	
    /** cancel transactions. */
	CANCEL("cancel"),
	
    /** credit transactions. */
	CREDIT("credit"),
	
    /** lottery transactions. */
	LOTTERY("lottery"),
	
    /** lottery win transactions. */
	LOTTERY_WIN("lotterywin"),
	
	/** Blackjack insurance. */
	BJ_INSURE("bjinsure"),
	
    /** payout transactions. */
	PAYOUT("payout"),
	
    /** jackpot transactions. */
	JACKPOT("pkrjackpot"),
	
    /** referral transactions. */
    POINTS("points"),
    
    /** poker buy in transactions. */
	POKER_BUYIN("pokerbuy"),
	
    /** poker cash out transactions. */
	POKER_CASHOUT("pokercash"),
	
    /** referral transactions. */
    REFERRAL("referral"),
    
    /** reset transactions. */
	RESET("reset"),
	
    /** swap transactions. */
    SWAP("swap"),
	
    /** transfer transactions. */
	TRANSFER("transfer"),
	
    /** win transactions. */
	WIN("win");
	
	/** The text. */
	private final String text;
	
	/**
	 * Constructor.
	 * 
	 * @param txt  textual representation.
	 */
	TransactionType(final String txt) {
		text = txt;
	}
	
	/**
	 * Returns a long textual form of this action.
	 * 
	 * @return The textual representation.
	 */
	public String getText() { return text; }
	
	/** 
	 * (non-Javadoc).
	 * @see java.lang.Enum#toString()
	 * 
	 * @return the output
	 */
	@Override
	public String toString() { return text;	}
	
	/** 
	 * Converts a String to the correct TransactionType.
	 * 
	 * @param text the string to check
	 * @return the correct enumerate object
	 */
    public static TransactionType fromString(final String text) {
        if (text != null) {
        	String txtlc = text.toLowerCase();
        	for (TransactionType tt : TransactionType.values()) {
        		if (tt.getText().equals(txtlc)) {
        			return tt;
        		}
        	}
        }
        return null;
    }
}
