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
* Represents the referrers table from the database.
* 
* @author Jamie
*/
public final class ReferersTable {
    /**
     * Hiding the default constructor.
     */
    private ReferersTable() { }

	/** The name of the table. */
	public static final String NAME = "referrers";
	
	/** Column for the unique id. */
	public static final String COL_USERID = "user_id";
	
	/** Column for the name of the game. */
    public static final String COL_REFERRERID = "referrer_id";
}
