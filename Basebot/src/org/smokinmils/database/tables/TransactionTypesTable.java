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
public final class TransactionTypesTable {
    /**
     * Hiding the default constructor.
     */
    private TransactionTypesTable() { }
	
	/** Table name. */
	public static final String NAME = "transaction_types";
	
	/** Column for the unique id. */
	public static final String COL_ID = "id";
	
	/** Column for the type of transaction. */
	public static final String COL_TYPE = "type";
	
}
