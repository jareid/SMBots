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
	
	/** Column for the total won amount. */
    public static final String COL_WINTOTAL = "win_total";
    
    /** Column for the total wins. */
    public static final String COL_WINCOUNT = "win_count";
    
    /** Column for the total amount cancelled made. */
    public static final String COL_CXLTOTAL = "cancel_total";
    
    /** Column for the total cancels made. */
    public static final String COL_CXLCOUNT = "cancel_count";
    
    /** Column for the total referral fees earnt. */
    public static final String COL_REFERTOTAL = "refer_total";
    
    /** Column for the total amount bet. */
    public static final String COL_BETTOTAL = "bet_total";
    
    /** Column for the total bets made. */
    public static final String COL_BETCOUNT = "bet_count";
}
