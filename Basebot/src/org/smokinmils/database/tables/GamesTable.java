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
* Represents the total bets view from the database.
* 
* @author Jamie
*/
public final class GamesTable {
    /**
     * Hiding the default constructor.
     */
    private GamesTable() { }
    
    /** The name of the table. */
	public static final String NAME = "games";
	
	/** Column for the unique id. */
	public static final String COL_ID = "id";
	
	/** Column for the name of the game. */
	public static final String COL_NAME = "name";
}
