/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */ 
package org.smokinmils.database.tables;

public class UsersTable {

	/** Table name */
	public static final String Name = "users";

	/** Column for the id */
	public static final String Col_ID = "id";
	
	/** Column for the username */
	public static final String Col_Username = "username";
	
	/** Column for the number of wins */
	public static final String Col_Wins = "wins";
	
	/** Column for the number of losses */
	public static final String Col_Losses = "losses";
	
	/** Column for the total amount bet */
	public static final String Col_TotalBets = "total_bets";
	
	/** Column for the active profile */
	public static final String Col_ActiveProfile = "active_profile";
}
