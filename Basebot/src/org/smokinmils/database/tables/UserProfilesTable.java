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
* Represents the user profiles table from the database.
* 
* @author Jamie
*/
public final class UserProfilesTable {
    /**
     * Hiding the default constructor.
     */
    private UserProfilesTable() { }
	
	/** Table name. */
	public static final String NAME = "user_profiles";
	
	/** Column for the userid. */
	public static final String COL_USERID = "user_id";
	
	/** Column for the typeid. */
	public static final String COL_TYPEID = "type_id";
	
	/** Column for the amount. */
	public static final String COL_AMOUNT = "amount";
	
	/** Column for last updated. */
	public static final String COL_LASTUPDATED = "last_updated";

}
