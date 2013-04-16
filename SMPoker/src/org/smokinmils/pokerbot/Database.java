/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.smokinmils.pokerbot.enums.TransactionType;
import org.smokinmils.pokerbot.logging.EventLog;
import org.smokinmils.pokerbot.settings.DBSettings;

import com.mchange.v2.c3p0.DataSources;

/**
 * A singleton Database access system for the poker bot
 * 
 * @author Jamie Reid
 */
public class Database {	
	/** Instance variable */
	private static Database instance = new Database();

   /** Static 'instance' method */
   public static Database getInstance() { return instance; }
   
   /** The un-pooled data source */
   private DataSource unpooled;
   
   /** The pooled data source */
   private DataSource pooled;
   
   /** The database URL */
   private String url = "jdbc:mysql://" + DBSettings.DBServer
		   				+ ":" + DBSettings.DBPort
		   				+ "/" + DBSettings.DBName;   
   
   /**
    * Constructor
    */
   private Database() {
	   try {
		   unpooled = DataSources.unpooledDataSource(url, DBSettings.DBUser, DBSettings.DBPass);
		   pooled = DataSources.pooledDataSource( unpooled );
		} catch (Exception e) {
			EventLog.fatal( e, "Database", "constructor");
			System.exit(1);
		}
   }
   
   /**
    * Getter method for a connection from the connection pool
    * 
    * @returns A connection
    */
   private Connection getConnection() {
	   try {
		   return pooled.getConnection();
	   } catch (SQLException e) {
		   EventLog.fatal( e, "Database", "getConnection");
		   System.exit(1);
		   return null;
	   }
   }
   
   /**
    * Adds a new hostmask to the DB for this user
    * 
    * If the user does not exist, we need to add the user to the user table
    * 
    * @param username	The user
    * @param hostmask	The hostmask
    */
   public void addHostmask(String username, String hostmask) {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String user_exist  = "SELECT " + DBSettings.Col_Users_ID +
					   		" FROM " + DBSettings.Table_Users +
						 	" WHERE " + DBSettings.Col_Users_Username +
						 	" LIKE '" + username + "'";
	   
	   String insert_user = "INSERT INTO " + DBSettings.Table_Users + "(" +
			   				DBSettings.Col_Users_Username + ") VALUES('" + username + "')"; 
	   	   
	   String insert_hostmask = "INSERT IGNORE INTO " + DBSettings.Table_Hostmasks + "(" +
  													DBSettings.Col_Hostmasks_Host + ", " +
  													DBSettings.Col_Hostmasks_UserID + ") " +  														
  								"VALUES('" + hostmask + "', '%userid')";
	   int user_id = -1;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.setMaxRows(1); 
		   rs = stmt.executeQuery(user_exist);
		   
		   if ( rs.next() ) {
			   user_id = rs.getInt(DBSettings.Col_Users_ID) ;
		   }
		   
		   // User didn't exist, add them and get their ID.
		   if (user_id == -1) {
			   stmt.executeUpdate(insert_user, Statement.RETURN_GENERATED_KEYS);
		       rs = stmt.getGeneratedKeys();
		       if (rs.next()) {
		    	   user_id = rs.getInt(1);
		       } else {
		           throw new RuntimeException("Can't find most recent user we just added");
		       }
		   }
		   
		   // Add the hostmask
		   insert_hostmask = insert_hostmask.replaceAll("%userid", Integer.toString(user_id));
		   stmt.execute( insert_hostmask );
		   
	   } catch (SQLException e) {
			EventLog.log( e, "Database", "addHostmask");
	   } catch (RuntimeException e) {
			EventLog.log( e, "Database", "addHostmask");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "addHostmask");
		   }
	   }
   }
   /**
    * Getter method for a user's active profile text
    *  
    * Performs an SQL statement on the DB
    * 
    * @param username	The profile name
    * 
    * @return			The amount of credits
    */
   public String getActiveProfile(String username) {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_UserActiveChips_ActiveProfile +
			   		" FROM " + DBSettings.View_UserActiveChips +
				 	" WHERE " + DBSettings.Col_Users_Username +
				 	" LIKE '" + username + "'";
	   String name;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.setMaxRows(1); 
		   rs = stmt.executeQuery(sql);
		   
		   if ( rs.next() ) {
			   name = rs.getString(DBSettings.Col_UserActiveChips_ActiveProfile) ;
		   } else {
			   name = "";
			   EventLog.info( "No results for '"+ username +"', continuing", "Database", "getActiveProfile");
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "getActiveProfile");
		   name = "";
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "getActiveProfile");
		   }
	   }
	   return name;
   }
   
   /**
    * Getter method for a user's active profile ID
    * 
    * Performs an SQL statement on the DB
    * 
    * @param username	The username
    * 
    * @return			The profile id
    */
   public int getActiveProfileID(String username) {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_UserActiveChips_ActiveProfile +
			   		" FROM " + DBSettings.View_UserActiveChips +
				 	" WHERE " + DBSettings.Col_Users_Username +
				 	" LIKE '" + username + "'";

	   String toidsql = "SELECT " + DBSettings.Col_Profiles_ID +
			   			 " FROM " + DBSettings.Table_Profiles_Type +
			 			 " WHERE " + DBSettings.Col_Profiles_Name + " LIKE '%profile'";
	   String name;
	   int id = -1;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.setMaxRows(1); 
		   rs = stmt.executeQuery(sql);
		   
		   if ( rs.next() ) {
			   name = rs.getString(DBSettings.Col_UserActiveChips_ActiveProfile) ;
			   
			   rs = stmt.executeQuery(toidsql.replaceAll("%profile", name));
			   
			   if ( rs.next() ) {
				   id = rs.getInt(DBSettings.Col_Profiles_ID) ;
			   } else {
				   id = -1;
				   EventLog.info( "No results for profile '"+ name +"', continuing", "Database", "getActiveProfile");
			   }
		   } else {
			   name = "";
			   EventLog.info( "No results for '"+ username +"', continuing", "Database", "getActiveProfile");
		   }
		   

	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "getActiveProfile");
		   name = "";
		   id = -1;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "getActiveProfile");
		   }
	   }
	   return id;
   }
   
   /**
    * Getter method for a user's credit count on the DB
    * 
    * Performs an SQL statement on the DB
    * 
    * @param username	The username
    * 
    * @return			The amount of credits
    */
   public int checkCredits(String username) {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_Users_Chips +
			   		" FROM " + DBSettings.View_UserActiveChips +
				 	" WHERE " + DBSettings.Col_Users_Username +
				 	" LIKE '" + username + "'";
	   
	   int credits = 0;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.setMaxRows(1); 
		   rs = stmt.executeQuery(sql);
		   
		   if ( rs.next() ) {
			   credits = rs.getInt(DBSettings.Col_Users_Chips) ;
		   } else {
			   credits = 0;
			   EventLog.info( "No results for '"+ username +"', continuing", "Database", "checkCredits");
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "checkCredits("+username+")");
		   EventLog.info( "Failed to get chip count for '"+ username +"', continuing", "Database", "checkCredits");
		   credits = 0;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "checkCredits");
		   }
	   }
	   return credits;
   }
   
   /**
    * Getter method for a user's credit count on the DB
    * 
    * Performs an SQL statement on the DB
    * 
    * @param username	The username
    * @param profile_id	The profile id
    * 
    * @return			The amount of credits
    */
   public int checkCredits(String username, int profile_id) {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_UserProfiles_Amount +
			   		" FROM " + DBSettings.Table_UserProfiles +
				 	" WHERE " + DBSettings.Col_UserProfiles_UserID + " = (" + getUserIDSQL(username) + ") AND "
				 			  + DBSettings.Col_UserProfiles_TypeID + " = '" + Integer.toString(profile_id) + "'";
	   
	   int credits = 0;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.setMaxRows(1); 
		   rs = stmt.executeQuery(sql);
		   
		   if ( rs.next() ) {
			   credits = rs.getInt(DBSettings.Col_UserProfiles_Amount) ;
		   } else {
			   credits = 0;
			   EventLog.info( "No results for '"+ username +"', continuing", "Database", "checkCredits");
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "checkCredits");
		   credits = 0;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "checkCredits");
		   }
	   }
	   return credits;
   }
   
   /**
    * Getter method for a user's credit count on the DB
    * 
    * Performs an SQL statement on the DB
    * 
    * @param username	The username
    * @param profile	The profile name
    * 
    * @return			The amount of credits
    */
   public int checkCredits(String username, String profile) {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_UserProfiles_Amount +
			   		" FROM " + DBSettings.View_UserProfiles +
				 	" WHERE " + DBSettings.Col_UserProfiles_Username + " = '" + username + "' AND "
				 			  + DBSettings.Col_UserProfiles_Name + " = '" + profile + "'";
	   
	   int credits = 0;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.setMaxRows(1); 
		   rs = stmt.executeQuery(sql);
		   
		   if ( rs.next() ) {
			   credits = rs.getInt(DBSettings.Col_UserProfiles_Amount) ;
		   } else {
			   EventLog.info( "No results for '"+ username +"', continuing", "Database", "checkCredits");
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "checkCredits");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "checkCredits");
		   }
	   }
	   return credits;
   }
   
   /**
    * Getter method for a user's credit count on the DB for all profiles
    * 
    * Performs an SQL statement on the DB
    * 
    * @param username	The username
    * 
    * @return			The amount of credits
    */
   public Map<String,Integer> checkAllCredits(String username) {
	   Map<String,Integer> res = new HashMap<String,Integer>();
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_UserProfiles_Name + "," + DBSettings.Col_UserProfiles_Amount +
			   		" FROM " + DBSettings.View_UserProfiles +
				 	" WHERE " + DBSettings.Col_UserProfiles_Username +
				 	" LIKE '" + username + "'";
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   rs = stmt.executeQuery(sql);
		   
		   while ( rs.next() ) {
			   res.put(rs.getString(DBSettings.Col_UserProfiles_Name), rs.getInt(DBSettings.Col_UserProfiles_Amount));
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "checkAllCredits("+username+")");
		   EventLog.info( "Failed to get chip count for '"+ username +"', continuing", "Database", "checkAllCredits");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "checkAllCredits");
		   }
	   }
	   return res;
   }
   
   /**
    * Updates the active profile for a users chips to the new profile
    * 
    * Performs an SQL statement on the DB
    * 
    * @param username	The username
    * 
    * @return			The amount of credits
    */
   public boolean updateActiveProfile(String username, String profile) {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "UPDATE " + DBSettings.Table_Users +
			   		" SET " + DBSettings.Col_UserActiveChips_ActiveProfile + " = "
			   				+ "(" + getProfileIDSQL(profile) + ")" +
				 	" WHERE " + DBSettings.Col_Users_Username +
				 	" LIKE '" + username + "'";
	   boolean result = false;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   int numrows = stmt.executeUpdate(sql);		   
		   result = (numrows == 1);
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "updateActiveProfile("+username+","+profile+")");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
			   EventLog.log( e, "Database", "updateActiveProfile("+username+","+profile+")");
		   }
	   }
	   return result;
   }
   
   /**
    * Restores all the bets from when the bot crashed.
    * 
    * Performs an SQL statement on the DB
    * 
    * @param username	The username
    * 
    * @return			The amount of credits
    */
   public void addPokerTableCount(String username, int table_id, int profile_id, int amount) {
	   Connection conn = null;
	   Statement stmt = null;
	   String updsql = "UPDATE " + DBSettings.Table_PokerBets +
			   		   " SET " + DBSettings.Col_PokerBets_Amount + " = '" + Integer.toString(amount) + "'" +
			   		   " WHERE " + DBSettings.Col_PokerBets_UserID + " = (" + getUserIDSQL(username) + ") AND " +
			   		   			   DBSettings.Col_PokerBets_ProfileID + " = '" + Integer.toString(profile_id) + "' AND " +
					   		   	   DBSettings.Col_PokerBets_TableID + " = '" + Integer.toString(table_id) + "'";

	   String inssql = "INSERT INTO " + DBSettings.Table_PokerBets
			   				+ " ("	+ DBSettings.Col_PokerBets_UserID + ","
			   						+ DBSettings.Col_PokerBets_ProfileID + ","
			   						+ DBSettings.Col_PokerBets_TableID + ","
			   						+ DBSettings.Col_PokerBets_Amount + ") " +
			   			"VALUES((" + getUserIDSQL(username) + "), "
			   					   + Integer.toString(profile_id) + ", "
					   			   + Integer.toString(table_id) + ", "	
					   			   + Integer.toString(amount) + ")";
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   int numrows = stmt.executeUpdate(updsql);
		   
		   if (numrows == 0) {
			   numrows = stmt.executeUpdate(inssql);
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "addPokerTableCount");
	   } finally {
		   try {
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "addPokerTableCount");
		   }
	   }
   }
   
   /**
    * Restores all the bets from when the bot crashed.
    * 
    * Performs an SQL statement on the DB
    * 
    * @param username	The username
    * 
    * @return			The amount of credits
    */
   public void restoreBets() {
       EventLog.info("Restoring any bets from crashes", "Database", "restoreBets");
	   Connection conn = null;
	   Statement stmt = null;
	   Statement updstmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_PokerBets_UserID + ","
			   				  + DBSettings.Col_PokerBets_ProfileID + ","
					   		  + DBSettings.Col_PokerBets_Amount +
			   		" FROM " + DBSettings.Table_PokerBets;
	   
	   String updsql = "UPDATE " + DBSettings.Table_UserProfiles +
				  	   " SET " + DBSettings.Col_UserProfiles_Amount
				  	           + " = (" + DBSettings.Col_UserProfiles_Amount + " + %amount)" +
				  	   " WHERE " + DBSettings.Col_UserProfiles_UserID + " = '%user_id' AND "
				  		    	 + DBSettings.Col_UserProfiles_TypeID + " = '%type_id'";

	   String delsql = "DELETE FROM " + DBSettings.Table_PokerBets;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   updstmt = conn.createStatement();
		   rs = stmt.executeQuery(sql);
		   
		   while ( rs.next() ) {
			   updsql = updsql.replaceAll("%amount", Integer.toString( rs.getInt(DBSettings.Col_PokerBets_Amount) ) );
			   updsql = updsql.replaceAll("%user_id", Integer.toString( rs.getInt(DBSettings.Col_PokerBets_UserID) ) );
			   updsql = updsql.replaceAll("%type_id", Integer.toString( rs.getInt(DBSettings.Col_PokerBets_ProfileID) ) );
			   updstmt.executeUpdate(updsql);
		   }
		   updstmt.executeUpdate(delsql);
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "restoreBets");
		   EventLog.info( "Failed to get restore chips counts.", "Database", "restoreBets");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (updstmt != null) stmt.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "restoreBets");
		   }
	   }
   }
   
   /**
    * Getter method for a user's credit count on the DB
    * 
    * Performs an SQL statement on the DB
    * 
    * @return			The list of profile types
    */
   public String getProfileName(int id) {
	   String result = null;
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_Profiles_Name +
			   		" FROM " + DBSettings.Table_Profiles_Type +
			   		" WHERE "  + DBSettings.Col_Profiles_ID + " = '" + id + "'";
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.setMaxRows(1);
		   rs = stmt.executeQuery(sql);
		   
		   if ( rs.next() ) {
			   result = rs.getString( DBSettings.Col_Profiles_Name );
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "getProfileName");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
			   EventLog.log( e, "Database", "getProfileName");
		   }
	   }
	   return result;
   }
   
   /**
    * Getter method for a user's credit count on the DB
    * 
    * Performs an SQL statement on the DB
    * 
    * @return			The list of profile types
    */
   public int getProfileID(String name) {
	   int result = -1;
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = getProfileIDSQL(name);
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.setMaxRows(1);
		   rs = stmt.executeQuery(sql);
		   
		   if ( rs.next() ) {
			   result = rs.getInt(1);
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "getProfileName");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
			   EventLog.log( e, "Database", "getProfileName");
		   }
	   }
	   return result;
   }
   
   /**
    * Getter method for a user's credit count on the DB
    * 
    * Performs an SQL statement on the DB
    * 
    * @return			The list of profile types
    */
   public List<String> getProfileTypes() {
	   ArrayList<String> results = new ArrayList<String>();
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + DBSettings.Col_Profiles_Name +
			   		" FROM " + DBSettings.Table_Profiles_Type;
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   rs = stmt.executeQuery(sql);
		   
		   while ( rs.next() ) {
			   results.add( rs.getString(DBSettings.Col_Profiles_Name) );
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "getProfileTypes");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
			   EventLog.log( e, "Database", "getProfileTypes");
		   }
	   }
	   return results;
   }
   
   /**
    * Gives a players chips
    * 
    * @param username	The player's username
    * @param amount		The cash out value
    * @param profile	The profile type text
 * @return 
    */
   public boolean giveChips(String username, int amount, String profile) {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   
	   String chips_sql = "SELECT * FROM " + DBSettings.View_UserProfiles +
			   				" WHERE " + DBSettings.Col_UserProfiles_Name + " LIKE " + "'" + profile + "'" +
			   					" AND " + DBSettings.Col_UserProfiles_Username + " LIKE " + "'" + username + "'";
	   
	   //TODO: remove ignore
	   String ins_sql = "INSERT IGNORE INTO " + DBSettings.Table_UserProfiles + "(" 
  							+ DBSettings.Col_UserProfiles_UserID + ", " 
			   				+ DBSettings.Col_UserProfiles_TypeID + ", "			
			   				+ DBSettings.Col_UserProfiles_Amount
			   				+ ") VALUES((" + getUserIDSQL(username) + "), ("
			   							  + getProfileIDSQL(profile) + "), "
			   							  + "'" + Integer.toString(amount) + "') ";
	   
	   String ins_user_sql = "INSERT INTO " + DBSettings.Table_Users + "(" 
					+ DBSettings.Col_UserProfiles_Username 
					+ ") VALUES('" + username + "')";
	   
	   String upd_sql = "UPDATE " + DBSettings.Table_UserProfiles +
			   			" SET " + DBSettings.Col_UserProfiles_Amount + "= (" + DBSettings.Col_UserProfiles_Amount
			   																+ " + " + amount + ")" +
			   			" WHERE " + DBSettings.Col_UserProfiles_TypeID + " LIKE (" + getProfileIDSQL(profile) +
			   				") AND " + DBSettings.Col_UserProfiles_UserID + " LIKE (" + getUserIDSQL(username) + ")";
	   boolean result = false;
	   int profile_id = -1;
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   
		   // check valid profile
		   rs = stmt.executeQuery(getProfileIDSQL(profile));
		   if (rs.next() ) {
			   profile_id = rs.getInt(DBSettings.Col_Profiles_ID);
			   // decide whether to insert or update
			   rs = stmt.executeQuery(chips_sql);
			   if (rs.next()) {
				   // update
				   int numrows = stmt.executeUpdate(upd_sql);
				   result = (numrows == 1);
			   } else {
				   // check user exists
				   if (stmt.execute(getUserIDSQL(username)) ) {
					   // user exists so just add to profiles
					   int numrows = stmt.executeUpdate(ins_sql);	
					   result = (numrows == 1);
				   } else {
					   // user didnt exist so add to both users and profiles
					   int numrows = stmt.executeUpdate(ins_user_sql);
					   if (numrows == 1)  {
						   numrows = stmt.executeUpdate(ins_sql);	
						   result = (numrows == 1);
					   } else {
						   result = false;
					   }
				   }
			   }
		   } else {
			   result = false;
		   }
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "giveChips("+username+","+Integer.toString(amount)+","+profile+")");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
			   EventLog.log( e, "Database", "giveChips("+username+","+Integer.toString(amount)+","+profile+")");
		   }
		   
		   if (result) {
			   addTransaction(username, amount, TransactionType.ADMIN, profile_id);
		   }
	   }
	   
	   return result;
   }
   
   /**
    * Adds a players poker chips back to their balance
    * We don't care if the user doesn't exist as they needed to exist to join the table
    * 
    * @param username	The player's username
    * @param amount		The cash out value
    */
   public void cashOut(String username, int amount, int profile_id) {
	   Connection conn = null;
	   Statement stmt = null;
	   
	   String sql = "UPDATE " + DBSettings.Table_UserProfiles + 
			   		" SET " + DBSettings.Col_UserProfiles_Amount + " = ("
			    			   + DBSettings.Col_UserProfiles_Amount + " + " + Integer.toString(amount) + ")" +		   		
			    	" WHERE " + DBSettings.Col_UserProfiles_UserID + " = (" + getUserIDSQL(username) + ") AND "
			 			  	  + DBSettings.Col_UserProfiles_TypeID + " = '" + Integer.toString(profile_id) + "'";
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.executeUpdate(sql);
	   } catch (SQLException e) {
			EventLog.log( e, "Database", "cashOut");
	   } finally {
		   try {
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "cashOut");
		   }
	   }
	   addTransaction(username, amount, TransactionType.CASHOUT, profile_id);
	   
   }
   
   /**
    * Removes poker chips from a player's balance
    * We don't care if the user doesn't exist as they needed to exist to join the table
    * 
    * @param username	The player's username
    * @param amount		The cash out value
    */
   public void buyIn(String username, int amount, int profile_id) {
	   Connection conn = null;
	   Statement stmt = null;

	   String sql = "UPDATE " + DBSettings.Table_UserProfiles + 
			   		" SET " + DBSettings.Col_UserProfiles_Amount + " = ("
			    			   + DBSettings.Col_UserProfiles_Amount + " - " + Integer.toString(amount) + ")" +		   		
			    	" WHERE " + DBSettings.Col_UserProfiles_UserID + " = (" + getUserIDSQL(username) + ") AND "
			 			  	  + DBSettings.Col_UserProfiles_TypeID + " = '" + Integer.toString(profile_id) + "'";
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.executeUpdate(sql);
	   } catch (SQLException e) {
			EventLog.log( e, "Database", "buyIn");
	   } finally {
		   try {
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "buyIn");
		   }
	   }
	   
	   addTransaction(username, -amount, TransactionType.BUYIN, profile_id);
	}
   
   /**
    * Getter method for the next hand ID
    * 
    * Performs an SQL statement on the DB where the new hand is created with a blank winner
    * 
    * @return  The handID
    */
   public int getHandID() {
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   int hand_id = 0;
	   String sql = "INSERT INTO " + DBSettings.Table_Hands +
	   			" ( " + DBSettings.Col_Hands_WinnerID + 
	   			", " + DBSettings.Col_Hands_Amount + ") " +
	   			"VALUES ('0', '0')";

	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
	       rs = stmt.getGeneratedKeys();
	       if (rs.next()) {
	           hand_id = rs.getInt(1);
	       } else {
	           throw new RuntimeException("Can't find most recent hand ID we just created");
	       }
	   } catch (SQLException e) {
			hand_id = - 1;
		    EventLog.log( e, "Database", "getHandID");
	   } catch (RuntimeException e) {
    	   hand_id = - 1;
			EventLog.log( e, "Database", "getHandID");
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				EventLog.log( e, "Database", "getHandID");
		   }
	   }
	return hand_id;
   }
   
   /**
    * Updates the hand table with the hand winner and pot size
    * 
    * @param hand_id	The ID
    * @param username	The winner
    * @param pot		The pot size
    */
   public void setHandWinner(int hand_id, String username, int pot) {
	   Connection conn = null;
	   Statement stmt = null;
	   String sql = "UPDATE " + DBSettings.Table_Hands +
				    "SET " + DBSettings.Col_Hands_WinnerID + " = (" + getUserIDSQL(username) + ")"
				           + DBSettings.Col_Hands_Amount + " = " + Integer.toString(pot) + " " +
	   				"WHERE " + DBSettings.Col_Hands_ID + " = " + Integer.toString(hand_id);
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.executeUpdate(sql);
	   } catch (SQLException e) {
			   EventLog.log( e, "Database", "setHandWinner");
	   } finally {
		   try {
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
			   EventLog.log( e, "Database", "setHandWinner");
		   }
	   }
   }
   
   /**
    * Adds a new transaction to the transaction table
    * 
    * @param username 	The username 
    * @param amount		The amount
    * @param tzx_type	The type of transaction
    */
   public void addTransaction(String username, int amount,
		   						TransactionType tzx_type, int profile_id) {
	   Connection conn = null;
	   Statement stmt = null;	   
	   String sql = "INSERT INTO " + DBSettings.Table_Transactions + "(" 
  							+ DBSettings.Col_Transactions_TypeID + ", " 
			   				+ DBSettings.Col_Transactions_GameID + ", "			   				
			   				+ DBSettings.Col_Transactions_UserID + ", "
			   				+ DBSettings.Col_Transactions_Amount + ", "
					   		+ DBSettings.Col_Transactions_Profile + ") VALUES("
			   				  + "(" + getTzxTypeIDSQL(tzx_type) + "), "
			   				  + "(" + getGameIDSQL() + "), "
			   				  + "(" +  getUserIDSQL(username) + "), "
					   		  + "(" +   Integer.toString(amount) + "), "
			   				  + "'" + Integer.toString(profile_id) + "') ";
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   stmt.executeUpdate(sql);
	   } catch (SQLException e) {
		   EventLog.log( e, "Database", "addTransaction("+username+","+Integer.toString(amount)+","+tzx_type.toString()+")");
	   } finally {
		   try {
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
			   EventLog.log( e, "Database", "addTransaction("+username+","+Integer.toString(amount)+","+tzx_type.toString()+")");
		   }
	   }
   }
   
   
   
   /**
    * Provides the SQL for retrieving a user's ID
    * 
    * @param username The username
    * 
    * @return The ID
    */
   private static final String getUserIDSQL(String username) {
	   String out = "SELECT uu.id " +
					" FROM " + DBSettings.Table_Users + " uu " +
					" WHERE uu." + DBSettings.Col_Users_Username +
					" LIKE '" + username + "' LIMIT 1";
	   return out;
   }
   
   /**
    * Provides the SQL for retrieving the Poker game ID
    * 
    * @return The ID
    */
   private static final String getGameIDSQL() {
	   String out = "SELECT gg.id " +
					" FROM " + DBSettings.Table_Games + " gg" +
					" WHERE gg." + DBSettings.Col_Games_Name +
					" LIKE '" + DBSettings.PokerGame + "' LIMIT 1";
	   return out;
   }
   
   /**
    * Provides the SQL for checking a profile type
    */
   private static final String getProfileIDSQL(String profile) {
	   String prof_sql = "SELECT " + DBSettings.Col_Profiles_ID
 	  			+ " FROM " + DBSettings.Table_Profiles_Type
 	  			+ " WHERE " + DBSettings.Col_Profiles_Name + " LIKE " + "'" + profile + "'";
	   return prof_sql;
   }
   
   /**
    * Provides the SQL for getting a transaction type ID
    */
   private static final String getTzxTypeIDSQL(TransactionType tzx_type) {
	   String out = "SELECT " + DBSettings.Col_TransactionTypes_ID
	   			  + " FROM " + DBSettings.Table_TransactionsTypes 
		   		  + " WHERE " + DBSettings.Col_TransactionTypes_Type 
		   		  + " LIKE '" +  tzx_type.toString() + "'";
	   return out;
   }
}
