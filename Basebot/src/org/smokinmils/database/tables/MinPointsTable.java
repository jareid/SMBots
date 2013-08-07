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
* Represents the competition id table from the database.
* 
* @author Jamie
*/
public final class MinPointsTable {	
    /**
     * Hiding the default constructor.
     */
    private MinPointsTable() { }
    
    /** Table name. */
	public static final String NAME = "minpoints";
	
	/** Column for the end date. */
	public static final String COL_POINTS = "points";
}
