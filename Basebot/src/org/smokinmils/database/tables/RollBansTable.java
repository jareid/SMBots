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
 * Represents the users table from the database.
 * 
 * @author Jamie
 */
public final class RollBansTable {
    /**
     * Hiding the default constructor.
     */
    private RollBansTable() { }

	/** Table name. */
	public static final String NAME = "roll_bans";

	/** Column for the id. */
	public static final String COL_USERID = "user_id";
}
