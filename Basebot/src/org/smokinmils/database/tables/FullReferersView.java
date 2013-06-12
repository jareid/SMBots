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
public final class FullReferersView {
    /**
     * Hiding the default constructor.
     */
    private FullReferersView() { }
    
    /** The name of the table. */
    public static final String NAME = "full_referers";
    
    /** Column for the user id. */
    public static final String COL_USERID = "user_id";
    
    /** Column for the referrer id. */
    public static final String COL_REFERRERID = "referrer_id";
    
    /** Column for the group id. */
    public static final String COL_GROUPID = "group_id";
}
