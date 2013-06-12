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
* Represents the Hostmasks Table from the database.
* 
* @author Jamie
*/
public final class HostmasksTable {
    /**
     * Hiding the default constructor.
     */
    private HostmasksTable() { }
    
    /** The name of the table. */
	public static final String NAME = "hostmasks";
	
	/** Column for the unique id. */
	public static final String COL_ID = "id";
	
	/** Column for the hostmask. */
	public static final String COL_HOST = "host";
	
	/** Column for the userid. */
	public static final String COL_USERID = "userid";
	
}
