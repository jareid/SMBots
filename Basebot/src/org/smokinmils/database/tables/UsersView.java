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
 * Represents the users view from the database.
 * 
 * @author Jamie
 */
public final class UsersView {
    /**
     * Hiding the default constructor.
     */
    private UsersView() { }
    
	/** Table name. */
	public static final String NAME = "users_and_chips";

	/** Column for the id. */
	public static final String COL_ID = "id";
	
	/** Column for the username. */
	public static final String COL_USERNAME = "username";
	
	/** Column for the current chips.
	 * @deprecated */
	public static final String COL_CHIPS = "chips";
	
	/** Column for the number of wins. */
	public static final String COL_WINS = "wins";
	
	/** Column for the number of losses. */
	public static final String COL_LOSSES = "losses";
	
	/** Column for the total amount bet. */
	public static final String COL_TOTALBETS = "total_bets";
	
	/** Column for the active profile. */
	public static final String COL_ACTIVEPROFILE = "active_profile";
}