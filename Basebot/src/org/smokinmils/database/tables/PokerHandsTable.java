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
* Represents the poker hands table from the database.
* 
* @author Jamie
*/
public final class PokerHandsTable {
    /**
     * Hiding the default constructor.
     */
    private PokerHandsTable() { }
    
	/** Table name. */
	public static final String NAME = "poker_hands";
	
	/** Column for the unique id. */
	public static final String COL_ID = "id";
	
	/** Column for the winnerid. */
	public static final String COL_WINNNERID = "winnerid";
	
	/** Column for the amount. */
	public static final String COL_AMOUNT = "amount";

}
