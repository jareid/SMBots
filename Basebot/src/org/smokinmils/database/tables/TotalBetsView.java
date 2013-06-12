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
public final class TotalBetsView {
    /**
     * Hiding the default constructor.
     */
    private TotalBetsView() { }
    
	/** Table name. */
	public static final String NAME = "totalbets";
	
	/** Column for the username. */
	public static final String COL_USERNAME = "Username";
	
	/** Column for the profile. */
	public static final String COL_PROFILE = "Profile";
	
	/** Column for the amount. */
	public static final String COL_TOTAL = "Total";
}
