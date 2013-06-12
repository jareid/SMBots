/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */ 
package org.smokinmils.database.tables;

/**
* Represents the poker bets table from the database.
* 
* @author Jamie
*/
public final class PokerBetsTable {
    /**
     * Hiding the default constructor.
     */
    private PokerBetsTable() { }
    
	/** Table name. */
	public static final String NAME = "poker_bets";
	
	/** Column for the userid. */
	public static final String COL_USERID = "user_id";
	
	/** Column for the profileid. */
	public static final String COL_PROFILEID = "profile_id";
	
	/** Column for the amount. */
	public static final String COL_AMOUNT = "amount";
	
	/** Column for the tableid. */
	public static final String COL_TABLEID = "table_id";
}
