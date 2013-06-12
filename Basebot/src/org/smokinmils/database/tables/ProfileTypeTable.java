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
* Represents the profile tpye table from the database.
* 
* @author Jamie
*/
public final class ProfileTypeTable {
    /**
     * Hiding the default constructor.
     */
    private ProfileTypeTable() { }
    
	/** Table name. */
	public static final String NAME = "profile_type";
	
	/** Column for the unique id. */
	public static final String COL_ID = "id";
	
	/** Column for the name of the profile. */
	public static final String COL_NAME = "name";
	
}
