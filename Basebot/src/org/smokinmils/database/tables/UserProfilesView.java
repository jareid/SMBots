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
* Represents the user profiles view from the database.
* 
* @author Jamie
*/
public final class UserProfilesView {
    /**
     * Hiding the default constructor.
     */
    private UserProfilesView() { }
    
	/** Table name. */
	public static final String NAME = "user_profiles_as_text";
	
	/** Column for the userid. */
	public static final String COL_USERNAME = "username";
	
	/** Column for the typeid. */
	public static final String COL_PROFILE = "name";
	
	/** Column for the amount. */
	public static final String COL_AMOUNT = "amount";
}
