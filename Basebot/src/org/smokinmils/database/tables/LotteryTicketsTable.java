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
* Represents the lottery tickets table from the database.
* 
* @author Jamie
*/
public final class LotteryTicketsTable {
    /**
     * Hiding the default constructor.
     */
    private LotteryTicketsTable() { }
    
    /** Table name. */
	public static final String NAME = "lottery_tickets";
	
	/** Column for the userid. */
	public static final String COL_USERID = "user_id";
	
	/** Column for the profile id. */
	public static final String COL_PROFILEID = "profile_id";
	
	/** Column for the id. */
	public static final String COL_ID = "id";
	

}
