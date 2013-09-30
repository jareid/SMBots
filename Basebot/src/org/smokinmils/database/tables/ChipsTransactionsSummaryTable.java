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
* Represents the transactions table from the database.
* 
* @author Jamie
*/
public final class ChipsTransactionsSummaryTable {
    /**
     * Hiding the default constructor.
     */
    private ChipsTransactionsSummaryTable() { }

	/** Table name. */
	public static final String NAME = "chipstransactions_summary";
	
	/** Column for the userid. */
	public static final String COL_USERID = "user_id";
	
	/** Column for the gameid. */
	public static final String COL_ADMINID = "admin_id";
	
	/** Column for the typeid. */
	public static final String COL_TYPEID = "type_id";
	
	/** Column for the amount. */
	public static final String COL_AMOUNT = "amount";
	
	/** Column for the profile type. */
	public static final String COL_PROFILETYPE = "profile_type";
}
