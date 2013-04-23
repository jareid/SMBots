/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */ 
package org.smokinmils.database.tables;

public class TransactionsTable {

	/** Table name */
	public static final String Name = "transactions";
	
	/** Column for the unique id */
	public static final String Col_ID = "id";
	
	/** Column for the userid */
	public static final String Col_UserID = "user_id";
	
	/** Column for the gameid */
	public static final String Col_GameID = "game_id";
	
	/** Column for the typeid */
	public static final String Col_TypeID = "type_id";
	
	/** Column for the timestamp */
	public static final String Col_Timestamp = "timestamp";
	
	/** Column for the amount */
	public static final String Col_Amount = "amount";
	
	/** Column for the profile type */
	public static final String Col_ProfileType = "profile_type";
	
	/** Column for the new total */
	public static final String Col_newtotal = "new_total";
}
