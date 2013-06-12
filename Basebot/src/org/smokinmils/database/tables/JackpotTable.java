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
* Represents the jackpot table from the database.
* 
* @author Jamie
*/
public final class JackpotTable {	
    /**
     * Hiding the default constructor.
     */
    private JackpotTable() { }
    
    /** Table name.*/
	public static final String NAME = "jackpots";
	
	/** Column for the profile. */
	public static final String COL_PROFILE = "profile_id";
	
	/** Column for the amount. */
	public static final String COL_TOTAL = "amount";
}
