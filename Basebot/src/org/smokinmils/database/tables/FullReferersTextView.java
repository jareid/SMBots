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
public final class FullReferersTextView {
    /**
     * Hiding the default constructor.
     */
    private FullReferersTextView() { }
    
    /** The name of the table. */
    public static final String NAME = "full_referers_text";
    
    /** Column for the user nick. */
    public static final String COL_USERNAME = "username";
    
    /** Column for the referrer nick. */
    public static final String COL_REFERRER = "referer";
    
    /** Column for the group name. */
    public static final String COL_GROUP = "hostgroup";
}
