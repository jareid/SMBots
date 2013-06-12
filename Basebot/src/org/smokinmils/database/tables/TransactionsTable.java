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
public final class TransactionsTable {
    /**
     * Hiding the default constructor.
     */
    private TransactionsTable() { }

	/** Table name. */
	public static final String NAME = "transactions";
	
	/** Column for the unique id. */
	public static final String COL_ID = "id";
	
	/** Column for the userid. */
	public static final String COL_USERID = "user_id";
	
	/** Column for the gameid. */
	public static final String COL_GAMEID = "game_id";
	
	/** Column for the typeid. */
	public static final String COL_TYPEID = "type_id";
	
	/** Column for the timestamp. */
	public static final String COL_TIMESTAMP = "timestamp";
	
	/** Column for the amount. */
	public static final String COL_AMOUNT = "amount";
	
	/** Column for the profile type. */
	public static final String COL_PROFILETYPE = "profile_type";
	
	/** Column for the new total. */
	public static final String COL_NEWTOTAL = "new_total";
}
