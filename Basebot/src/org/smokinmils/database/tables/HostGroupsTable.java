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
* Represents the hostgroups table from the database.
* 
* @author Jamie
*/
public final class HostGroupsTable {
    /**
     * Hiding the default constructor.
     */
    private HostGroupsTable() { }
    
    /** The name of the table. */
	public static final String NAME = "hostgroups";
	
	/** Column for the unique id. */
	public static final String COL_ID = "id";
	
	/** Column for the name of the group. */
	public static final String COL_NAME = "name";
	
    /** Column for the owner of the group. */
    public static final String COL_OWNER = "owner_id";
}
