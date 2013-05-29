/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */ 
package org.smokinmils.database.tables;

public class FullReferersTextView {
    /** The name of the table */
    public static final String Name = "full_referers";
    
    /** Column for the unique id */
    public static final String Col_Username = "username";
    
    /** Column for the name of the game */
    public static final String Col_Referer = "referer";
    
    public static final String Col_Group = "hostgroup";
}
