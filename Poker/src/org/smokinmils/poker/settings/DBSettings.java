/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.poker.settings;

/**
 * Database settings used with the database accessor
 * 
 * @author Jamie Reid
 */
public class DBSettings {
	/** The Server name of the database */
	public static final String  DBServer =			"199.101.50.187";
	
	/** The port number of the database (MySQL is typically 3306) */
	public static final int 	DBPort =			3306;
	
	/** The database name of the database */
	public static final String  DBName =			"live";
	
	/** The username of the database */
	public static final String  DBUser =			"smbot";
	
	/** The password of the database */
	public static final String  DBPass =			"SM_bot_2013$";

	/** Table storing active bets / poker cash outs */
	public static final String Table_Bets = "bets";
	
	/** Column that stores the unique key for the table */
	public static final String Col_Bets_ID = "id";
	
	/** Column that stores a unique user ID (Foreign Key constrained) */
	public static final String Col_Bets_UserID = "userid";
	
	/** Column that stores the bet amount */
	public static final String Col_Bets_Amount = "amount";
	
	/** Column that stores the bet choice */
	public static final String Col_Bets_Choice = "choice";
	
	/** Column that stores a unique game ID (Foreign Key constrained) */
	public static final String Col_Bets_GameID = "gameid";

	/** Table storing unique game names and their ID */
	public static final String Table_Games = "games";
	
	/** Column that stores the unique key for the table */
	public static final String Col_Games_ID = "id";
	
	/** Column that stores a the game name*/
	public static final String Col_Games_Name = "name";
	
	/** Game type constants the poker bot cares about */
	public static final String PokerGame = "Poker";

	/** Table that stores the in-game username associated with an IRC user */
	public static final String Table_GameNames = "game_names";
	
	/** Column that stores the unique key for the table */
	public static final String Col_GameNames_ID = "id";
	
	/** Column that stores the textual representation of the game name */
	public static final String Col_GameNames_Name = "name";
	
	/** Column that stores a unique user ID (Foreign Key constrained)*/
	public static final String Col_Games_UserID = "userid";

	/** Table that stores the poker hand ids and the winner for each hand */
	public static final String Table_Hands = "poker_hands";
	
	/** Column that stores the unique key for the table */
	public static final String Col_Hands_ID = "id";

	/** Column that stores the pot size for the hand */
	public static final String Col_Hands_Amount = "amount";
	
	/** Column that stores a unique user ID (Foreign Key constrained)*/
	public static final String Col_Hands_WinnerID = "winnerid";

	/** Table that stores the all IRC transactions */
	public static final String Table_Transactions = "transactions";
	
	/** Column that stores the unique user ID (Foreign Key constrained) */
	public static final String Col_Transactions_UserID = "user_id";
	
	/** Column that stores the unique game ID (Foreign Key constrained) */
	public static final String Col_Transactions_GameID = "game_id";
	
	/** Column that stores a the unique transaction type ID (Foreign Key constrained) */
	public static final String Col_Transactions_TypeID = "type_id";
	
	/** Column that stores a timestamp for the transaction */
	public static final String Col_Transactions_Timestamp = "timestamp";
	
	/** Column that stores an amount for the transaction */
	public static final String Col_Transactions_Amount = "amount";
	
	/** Column that stores the profile type ID for the transaction */
	public static final String Col_Transactions_Profile = "profile_type";

	/** Table that stores all possible unique transaction types */
	public static final String Table_TransactionsTypes = "transaction_types";
	
	/** Column that stores the unique key for the table */
	public static final String Col_TransactionTypes_ID = "id";
	
	/** Column that stores the textual representation of the type */
	public static final String Col_TransactionTypes_Type = "type";
	
	/** Transaction type constants the poker bot cares about */
	public static final String PokerBuyIn = "pokerbuy";
	public static final String PokerCashOut = "pokercash";
	public static final String TransAdmin = "admin";
	public static final String PokerJackpot = "pkrjackpot";
	
	/** Table that stores information about the IRC users */
	public static final String Table_Users = "users";
	
	/** Column that stores the unique key for the table */
	public static final String Col_Users_ID = "id";
	
	/** Column that stores the IRC username */
	public static final String Col_Users_Username = "username";
	
	/** Column that stores number of chips this user has */
	public static final String Col_Users_Chips = "chips";
	
	/** Column that stores number of chips won by this user */
	public static final String Col_Users_Wins = "wins";
	
	/** Column that stores number of losses by this user */
	public static final String Col_Users_Losses = "losses";
	
	/** Column that stores total bets made by this user */
	public static final String Col_Users_TotalBets = "total_bets";
	
	/** Table that stores host masks for identified users */
	public static final String Table_Hostmasks = "hostmasks";
	
	/** Column that stores the unique key for the table */
	public static final String Col_Hostmasks_ID = "id";
	
	/** Column that stores a hostmask*/
	public static final String Col_Hostmasks_Host = "host";
	
	/** Column that stores a unique user ID (Foreign Key constrained)*/
	public static final String Col_Hostmasks_UserID = "userid";
	
	/** View linking the User profiles table to the users table */
	public static final String View_UserProfiles = "user_profiles_as_text";
	
	/** Column of the username */
	public static final String Col_UserProfiles_Username = "username";

	/** Column of the profile name */
	public static final String Col_UserProfiles_Name = "name";
	
	/** Column of the amount */
	public static final String Col_UserProfiles_Amount = "amount";
	
	/** View linking the User profiles table to the users table */
	public static final String Table_UserProfiles = "user_profiles";
	
	/** Column of the username */
	public static final String Col_UserProfiles_UserID = "user_id";

	/** Column of the profile name */
	public static final String Col_UserProfiles_TypeID = "type_id";
	
	
	/** View linking the User profiles table to the users table */
	public static final String View_UserActiveChips = "users_and_chips";
	
	/** Column that stores the IRC username */
	public static final String Col_UserActiveChips_Username = "username";
	
	/** Column that stores number of chips this user has */
	public static final String Col_UserActiveChips_Chips = "chips";
	
	/** Column that stores number of chips won by this user */
	public static final String Col_UserActiveChips_Wins = "wins";
	
	/** Column that stores number of losses by this user */
	public static final String Col_UserActiveChips_Losses = "losses";
	
	/** Column that stores total bets made by this user */
	public static final String Col_UserActiveChips_TotalBets = "total_bets";
	
	/** Column that stores total bets made by this user */
	public static final String Col_UserActiveChips_ActiveProfile = "active_profile";	
	
	
	/** User profile types table */
	public static final String Table_Profiles_Type = "profile_type";
	
	/** Column that stores the Profile ID */
	public static final String Col_Profiles_ID = "id";
	
	/** Column that stores number of chips this user has */
	public static final String Col_Profiles_Name = "name";
	
	/** User profile types table */
	public static final String Table_PokerBets = "poker_bets";
	
	/** Column that stores the Profile ID */
	public static final String Col_PokerBets_ProfileID = "profile_id";
	
	/** Column that stores the User ID */
	public static final String Col_PokerBets_UserID = "user_id";
	
	/** Column that stores number of chips this user has */
	public static final String Col_PokerBets_Amount = "amount";
	
	/** Column that stores table id each record is for */
	public static final String Col_PokerBets_TableID = "table_id";
}
