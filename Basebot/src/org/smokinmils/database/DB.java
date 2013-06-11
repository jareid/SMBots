/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */ 
package org.smokinmils.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.smokinmils.database.tables.*;
import org.smokinmils.database.types.*;
import org.smokinmils.settings.DBSettings;

import com.mchange.v2.c3p0.DataSources;

/**
 * A singleton Database access system for the poker bot
 * 
 * @author Jamie Reid
 */
public class DB {	
	/** Instance variable */
	private static DB instance;
    static {
        try {
            instance = new DB();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

   /** Static 'instance' method */
   public static DB getInstance() { return instance; }
   
   /** The un-pooled data source */
   private DataSource unpooled;
   
   /** The pooled data source */
   private DataSource pooled;
   
   /** The database URL */
   private String url = "jdbc:mysql://" + DBSettings.DBServer
		   				+ ":" + DBSettings.DBPort
		   				+ "/" + DBSettings.DBName
		   				+ "?autoReconnect=true";   
   
   /**
    * Constructor
    */
   private DB() throws Exception {
	   unpooled = DataSources.unpooledDataSource(url, DBSettings.DBUser, DBSettings.DBPass);
	   pooled = DataSources.pooledDataSource( unpooled );
   }
   
   /**
    * Getter method for a connection from the connection pool
    * 
    * @returns A connection
    */
	private Connection getConnection() throws SQLException {
	   return pooled.getConnection();
	}
   
   /**
    * Adds a new hostmask to the DB for this user
    * 
    * If the user does not exist, we need to add the user to the user table
    * 
    * @param username	The user
    * @param hostmask	The hostmask
    */
   public void addHostmask(String username, String hostmask) throws DBException,SQLException {
	   String user_exist  = "SELECT " + UsersTable.Col_ID +
					   		" FROM " + UsersTable.Name +
						 	" WHERE " + UsersTable.Col_Username +
						 	" LIKE '" + username + "'";
	   	   
	   String insert_hostmask = "INSERT IGNORE INTO " + HostmasksTable.Name + "(" +
			   											HostmasksTable.Col_Host + ", " +
			   											HostmasksTable.Col_UserID + ") " +  														
  								"VALUES('" + hostmask + "', '%userid')";
	   
	   int user_id = runGetIntQuery( user_exist );
	   if ( user_id != -1 ) {
		   insert_hostmask = insert_hostmask.replaceAll("%userid", Integer.toString(user_id));
		   runBasicQuery( insert_hostmask );
	   }
   }


   /**
    * Adds a bet to the database
    * 
    * @param username	The user making the bet
    * @param choice		The bet choice
    * @param amount		The bet amount
    * @param profile	The profile the bet is from
    * @param game		The game the bet is on
    * 
    * @return	true if the query was run successfully.
    */
	public boolean addBet(String username, String choice, double amount,
						ProfileType profile, GamesType game)
								   throws DBException, SQLException {
		String sql = "INSERT INTO " + BetsTable.Name + "("
										+ BetsTable.Col_UserID + ", "
										+ BetsTable.Col_Amount + ", "										
										+ BetsTable.Col_Choice + ", "										
										+ BetsTable.Col_Gameid + ", "									
										+ BetsTable.Col_Profile + ") " +
					 " VALUES((" + getUserIDSQL(username) + "), "
					 			+ "'" + Double.toString(amount) + "', "
							 	+ "'" + choice + "', "
							 	+ "(" + getGameIDSQL(game) + "), "
							 	+ "(" + getProfileIDSQL(profile) + "))";
		
		return runBasicQuery(sql) == 1;		
	}

	/**
	 * Deletes a bet from the database
	 * 
	 * @param bet
	 *            The bet to remove
	 * @param game
	 *            The game represented as a unique string
	 * @return 
	 */
	public boolean deleteBet(String username, GamesType game)
			   throws DBException, SQLException {
		String sql = "DELETE FROM " + BetsTable.Name +
					 " WHERE " + BetsTable.Col_UserID + " = (" + getUserIDSQL(username) + ")" +
					 " AND " + BetsTable.Col_Gameid + " = (" + getGameIDSQL(game) + ")";
		 
		return runBasicQuery(sql) == 1;
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
   public ProfileType getActiveProfile(String username)
		   throws DBException, SQLException  {
	   String sql = "SELECT " + UsersView.Col_ActiveProfile +
			   		" FROM " + UsersView.Name +
				 	" WHERE " + UsersView.Col_Username +
				 	" LIKE '" + username + "'";
	   return ProfileType.fromString( runGetStringQuery(sql) );
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
   public int getActiveProfileID(String username)
		   throws DBException, SQLException  {
	   ProfileType profile = getActiveProfile( username );
	   int id = -1;
	   if (profile != null) {
		   id = runGetIntQuery( getProfileIDSQL( profile ) );
	   }
	   return id;
   }
   
   
   /**
    * Checks a user exists in the user table, if they don't adds them
    * 
    * @param username The user's nickname
    * @param hostmask
    */
   public boolean checkUserExists(String username, String hostmask) throws DBException, SQLException {
	   String sql = "SELECT COUNT(*) FROM " + UsersTable.Name +
  				" WHERE " + UsersTable.Col_Username + " LIKE " + "'" + username + "'";
	   
	   String hostsql = "SELECT COUNT(" + HostmasksTable.Col_UserID + ") FROM " + HostmasksTable.Name +
 				" WHERE " + HostmasksTable.Col_Host + " LIKE " + "'" + hostmask + "'";
	   
	   String ins_user_sql = "INSERT INTO " + UsersTable.Name + "(" 
								+ UsersTable.Col_Username
								+ ") VALUES('" + username + "')";
	   boolean ret = true;

	   // check the user exists
	   if ( runGetIntQuery(sql) < 1 ) {
			// they don't so check they don't have more than 1 accounts
		   if ( runGetIntQuery(hostsql) < 1 ) {			
			   runBasicQuery(ins_user_sql);
			   addHostmask(username, hostmask);
		   } else {
			   // too many accounts
			   ret = false;
		   }
		} else {
			addHostmask(username, hostmask);
		}
		
		return ret;
   }
   
   /**
    * Checks a user exists in the user table, if they don't adds them
    * 
    * @param username The user's nickname
    * @param hostmask
    */
   public boolean checkUserExists(String username) throws DBException, SQLException {
	   String sql = "SELECT COUNT(*) FROM " + UsersTable.Name +
  				" WHERE " + UsersTable.Col_Username + " LIKE " + "'" + username + "'";
	
		return runGetIntQuery(sql) == 1;
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
   public int checkCreditsAsInt(String username) throws DBException, SQLException  {
       return (int)Math.floor(checkCredits(username));
   }
   public double checkCredits(String username) throws DBException, SQLException  {
	   ProfileType active = getActiveProfile( username );
	   return checkCredits(username, active);
   }
   public double checkCredits(String username, Double amount) throws DBException, SQLException  {
       ProfileType active = getActiveProfile( username );
       double chips = checkCredits(username, active);
       double chip_diff = chips - amount;
       double ret = 0.0;
       if (amount <= 1.0) {
           if (chips >= 1.0) ret = 1.0;
           else if (chips <= 1.0) ret = chips;
       } else if (amount > chips) {
           if (-chip_diff <= 1.0) ret = chips;
           else ret = 0.0;
       } else if (amount <= chips) {
           if (chip_diff <= 0.5) ret = chips;
           else ret = amount;
       }
       
       return ret;
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
   public int checkCreditsAsInt(String username, ProfileType profile)
           throws DBException, SQLException  {
       return (int)Math.floor(checkCredits(username, profile));
   }
   public double checkCredits(String username, ProfileType profile)
		   throws DBException, SQLException  {
	   String sql = "SELECT " + UserProfilesView.Col_Amount +
			   		" FROM " + UserProfilesView.Name +
				 	" WHERE " + UserProfilesView.Col_Username + " = '" + username + "' AND "
				 			  + UserProfilesView.Col_Profile + " = '" + profile.toString() + "'";
	   
	   double credits = runGetDblQuery( sql );
	   if (credits < 0) credits = 0;
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
   public Map<ProfileType,Double> checkAllCredits(String username)
		   throws DBException, SQLException {
	   Map<ProfileType,Double> res = new HashMap<ProfileType,Double>();
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String sql = "SELECT " + UserProfilesView.Col_Profile + "," + UserProfilesView.Col_Amount +
			   		" FROM " + UserProfilesView.Name +
				 	" WHERE " + UserProfilesView.Col_Username +
				 	" LIKE '" + username + "'";
	   
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   rs = stmt.executeQuery(sql);
		   
		   while ( rs.next() ) {
			   res.put(ProfileType.fromString(rs.getString(UserProfilesView.Col_Profile)), 
					   rs.getDouble(UserProfilesView.Col_Amount));
		   }
	   } catch (SQLException e) {
		  throw new DBException(e, sql);
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				  throw e;
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
   public boolean updateActiveProfile(String username, ProfileType profile)
		   throws DBException, SQLException {
	   String sql = "UPDATE " + UsersTable.Name +
			   		" SET " + UsersTable.Col_ActiveProfile + " = "
			   				+ "(" + getProfileIDSQL(profile) + ")" +
				 	" WHERE " + UsersTable.Col_Username +
				 	" LIKE '" + username + "'";
	   
	   return runBasicQuery( sql ) == 1;
   }
   

   /**
    * Adds a number of chips at a certain table for a user so it can be returned if the 
    * bot decides it needs to take a break and crashes
    * 
    * @param username
    * @param table_id
    * @param profile
    * @param amount
    * 
    * @throws DBException
    * @throws SQLException
    */
   public void addPokerTableCount(String username, int table_id, ProfileType profile, int amount)
		   throws DBException, SQLException {
	   String updsql = "UPDATE " + PokerBetsTable.Name +
			   		   " SET " + PokerBetsTable.Col_Amount + " = '" + Integer.toString(amount) + "'" +
			   		   " WHERE " + PokerBetsTable.Col_UserID + " = (" + getUserIDSQL(username) + ") AND " +
			   		   				PokerBetsTable.Col_ProfileID + " = (" + getProfileIDSQL(profile) + ") AND " +
			   		   				PokerBetsTable.Col_TableID + " = '" + Integer.toString(table_id) + "'";

	   String inssql = "INSERT INTO " + PokerBetsTable.Name
			   				+ " ("	+ PokerBetsTable.Col_UserID + ","
			   						+ PokerBetsTable.Col_ProfileID + ","
			   						+ PokerBetsTable.Col_TableID + ","
			   						+ PokerBetsTable.Col_Amount + ") " +
			   			"VALUES((" + getUserIDSQL(username) + "), ("
			   					   + getProfileIDSQL(profile) + "), "
					   			   + Integer.toString(table_id) + ", "	
					   			   + Integer.toString(amount) + ")";
	   int numrows = runBasicQuery( updsql );
	   if ( numrows == 0 ) {
		   runBasicQuery( inssql );
	   }
   }
   
   public void processRefunds() throws DBException, SQLException {
       processOtherRefunds();
       processPokerRefunds();
   }
   
	/**
	 * Run upon start up, refunds all active bets that were never called or
	 * rolled for since a crash / restart
	 * @throws DBSQLException
	 * @throws SQLException 
	 */
	public void processOtherRefunds() throws DBException, SQLException {
	    String sql_select = "SELECT ut." + UsersTable.Col_Username + ", "
	                                     + BetsTable.Col_Amount + ", " +
	                              " pt." + ProfileTypeTable.Col_Name +
	                        " FROM " + BetsTable.Name + " bt" +
	                        " JOIN " + UsersTable.Name + " ut ON ut."
	                                 + UsersTable.Col_ID + " = bt." + BetsTable.Col_UserID +
	                        " JOIN " + ProfileTypeTable.Name + " pt ON pt."
	                                 + ProfileTypeTable.Col_ID + " = bt." + BetsTable.Col_Profile;
	    String sql_delete = "DELETE FROM bets WHERE 1";
	    
	    Connection conn = null;
	    Statement stmt = null;
	    ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
            try {
                rs = stmt.executeQuery(sql_select);
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql_select);
            }
			while (rs.next()) {
				adjustChips(rs.getString("ut." + UsersTable.Col_Username),
				            rs.getInt(BetsTable.Col_Amount),
						 	ProfileType.fromString(rs.getString(ProfileTypeTable.Col_Name)), 
						 	GamesType.ADMIN, TransactionType.ADMIN);
			}
            try {
                stmt.executeUpdate("DELETE FROM bets WHERE 1");
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql_delete);
            }
	    } catch (SQLException ex) {
	       throw ex;
		} finally {
			try {
				if (rs != null) rs.close();
				if (stmt != null) stmt.close();
				if (conn != null) conn.close();
			} catch (SQLException e) {
				throw e;
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
   public void processPokerRefunds() throws DBException, SQLException {
	   String sql = "SELECT " + PokerBetsTable.Col_UserID + ","
			   				  + PokerBetsTable.Col_ProfileID + ","
					   		  + PokerBetsTable.Col_Amount +
			   		" FROM " + PokerBetsTable.Name;
	   
	   String updsql = "UPDATE " + UserProfilesTable.Name +
				  	   " SET " + UserProfilesTable.Col_Amount
				  	           + " = (" + UserProfilesTable.Col_Amount + " + %amount)" +
				  	   " WHERE " + UserProfilesTable.Col_UserID + " = '%user_id' AND "
				  		    	 + UserProfilesTable.Col_TypeID + " = '%type_id'";

	   String delsql = "DELETE FROM " + PokerBetsTable.Name;

	   Connection conn = null;
	   Statement stmt = null;
	   Statement updstmt = null;
	   ResultSet rs = null;
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   updstmt = conn.createStatement();
		   try {
			   rs = stmt.executeQuery(sql);
		   } catch (SQLException e) {
			   throw new DBException(e, sql);
		   }
		   
		   while ( rs.next() ) {
			   String updsql_inst = updsql.replaceAll("%amount", Integer.toString( rs.getInt(PokerBetsTable.Col_Amount) ) );
			   updsql_inst = updsql_inst.replaceAll("%user_id", Integer.toString( rs.getInt(PokerBetsTable.Col_UserID) ) );
			   updsql_inst = updsql_inst.replaceAll("%type_id", Integer.toString( rs.getInt(PokerBetsTable.Col_ProfileID) ) );
			   try {
				   updstmt.executeUpdate(updsql_inst);
			   } catch (SQLException e) {
				   throw new DBException(e, updsql_inst);
			   }
		   }
		   try {
			   updstmt.executeUpdate(delsql);
		   } catch (SQLException e) {
			   throw new DBException(e, delsql);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (updstmt != null) stmt.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException exc) {
				throw exc;
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
   public ProfileType getProfileName(int id) throws DBException, SQLException {
	   String sql = "SELECT " + ProfileTypeTable.Col_Name +
			   		" FROM " + ProfileTypeTable.Name +
			   		" WHERE "  + ProfileTypeTable.Col_ID + " = '" + id + "'";
	   
	   return ProfileType.fromString(runGetStringQuery( sql ));
   }
   
   /**
    * Getter method for a user's credit count on the DB
    * 
    * Performs an SQL statement on the DB
    * 
    * @return			The list of profile types
    */
   public int getProfileID(ProfileType profile) throws DBException, SQLException {
	   return runGetIntQuery( getProfileIDSQL(profile) );
   }
   
   /**
    * Transfer's chips from one user to another
    * 
    * Presumes both users exist and the sender has enough chips
    * 
    * @param sender	The user who is sending chips
    * @param user	The user to received the chips
    * @param profile The profile the chips are from.
    */
	public void transferChips(String sender, String user, int amount, ProfileType profile)
			throws DBException, SQLException {
	   String chips_sql = "SELECT COUNT(*) FROM " + UserProfilesView.Name +
   						  " WHERE " + UserProfilesView.Col_Profile + " LIKE " + "'" + profile.toString() + "'" +
   						  " AND " + UserProfilesView.Col_Username + " LIKE " + "'" + user + "'";

		String upd_minus_sql = "UPDATE " + UserProfilesTable.Name +
			   				   " SET " + UserProfilesTable.Col_Amount + "= (" + UserProfilesTable.Col_Amount
			   																+ " - " + amount + ")" +
			   				   " WHERE " + UserProfilesTable.Col_TypeID + " LIKE (" + getProfileIDSQL(profile) +
			   				   ") AND " + UserProfilesTable.Col_UserID + " LIKE (" + getUserIDSQL(sender) + ")";
	
	   String ins_sql = "INSERT INTO " + UserProfilesTable.Name + "(" 
									   + UserProfilesTable.Col_UserID + ", " 
									   + UserProfilesTable.Col_TypeID + ", "			
									   + UserProfilesTable.Col_Amount
   				        + ") VALUES((" + getUserIDSQL(user) + "), ("
   							  	 	   + getProfileIDSQL(profile) + "), "
   							  	 	   + "'" + Integer.toString(amount) + "') ";
		
		String upd_add_sql = "UPDATE " + UserProfilesTable.Name +
							 " SET " + UserProfilesTable.Col_Amount + "= (" + UserProfilesTable.Col_Amount
																+ " + " + amount + ")" +
				   	         " WHERE " + UserProfilesTable.Col_TypeID + " LIKE (" + getProfileIDSQL(profile) +
				             ") AND " + UserProfilesTable.Col_UserID + " LIKE (" + getUserIDSQL(user) + ")";
		
		
		if ( runGetIntQuery( chips_sql ) < 1 ) {
			runBasicQuery( ins_sql );
		} else {
			runBasicQuery( upd_add_sql );
		}
		runBasicQuery( upd_minus_sql );

	   addTransaction(sender, (0 - amount), GamesType.ADMIN, TransactionType.TRANSFER, profile);
	   addTransaction(user, amount, GamesType.ADMIN, TransactionType.TRANSFER, profile);
	}
	
   /**
    * Adjusts a players chips
    * 
    * @param username	The player's username
    * @param amount		The cash out value
    * @param profile	The profile type
    * 
    * @return true if it succeeded
    */
	public boolean giveChips(String username, double amount, ProfileType profile)
			throws DBException, SQLException {
		return adjustChips(username, amount, profile, GamesType.ADMIN, TransactionType.CREDIT);
	}
	
   /**
    * Adjusts a players chips
    * 
    * @param username	The player's username
    * @param amount		The cash out value
    * @param profile	The profile type
    * 
    * @return true if it succeeded
    */
	public boolean payoutChips(String username, double amount, ProfileType profile)
			throws DBException, SQLException {
		return adjustChips(username, (0-amount), profile, GamesType.ADMIN, TransactionType.PAYOUT);
	}
   
   /**
    * Adjusts a players chips
    * 
    * @param username	The player's username
    * @param amount		The cash out value
    * @param profile	The profile type
    * 
    * @return true if it succeeded
    */
   public boolean adjustChips(String username, int amount, ProfileType profile, 
		      				  GamesType game, TransactionType tzx_type) 
		   		   throws DBException, SQLException {
	   return adjustChips(username, (double)amount, profile, game, tzx_type);
   }
   
   public boolean adjustChips(String username, double amount, ProfileType profile, 
		   				      GamesType game, TransactionType tzx_type)
		   throws DBException, SQLException {	   
	   String chips_sql = "SELECT COUNT(*) FROM " + UserProfilesView.Name +
			   				" WHERE " + UserProfilesView.Col_Profile + " LIKE " + "'" + profile.toString() + "'" +
			   					" AND " + UserProfilesView.Col_Username + " LIKE " + "'" + username + "'";

	   String ins_sql = "INSERT INTO " + UserProfilesTable.Name + "(" 
  							+ UserProfilesTable.Col_UserID + ", " 
			   				+ UserProfilesTable.Col_TypeID + ", "			
			   				+ UserProfilesTable.Col_Amount
			   				+ ") VALUES((" + getUserIDSQL(username) + "), ("
			   							  + getProfileIDSQL(profile) + "), "
			   							  + "'" + Double.toString(amount) + "') ";
	   
	   String ins_user_sql = "INSERT INTO " + UsersTable.Name + "(" 
					+ UsersTable.Col_Username
					+ ") VALUES('" + username + "')";
	   
	   String upd_sql = "UPDATE " + UserProfilesTable.Name +
			   			" SET " + UserProfilesTable.Col_Amount + "= (" + UserProfilesTable.Col_Amount
			   																+ " + " + amount + ")" +
			   			" WHERE " + UserProfilesTable.Col_TypeID + " LIKE (" + getProfileIDSQL(profile) +
			   				") AND " + UserProfilesTable.Col_UserID + " LIKE (" + getUserIDSQL(username) + ")";
	   boolean result = false;
	   
	   int profile_id = runGetIntQuery( getProfileIDSQL(profile) );
	   // check valid profile
	   if (profile_id != -1) {
		   // decide whether to insert or update
		   if ( runGetIntQuery( chips_sql ) >= 1) {
			   // update
			   result = (runBasicQuery( upd_sql ) == 1);
		   } else {
			   // check user exists 
			   if (runGetIntQuery( getUserIDSQL(username) ) > 0) {
				   // user exists so just add to profiles
				   result = (runBasicQuery( ins_sql ) == 1);
			   } else {
				   // user didnt exist so add to both users and profiles
				   int numrows = runBasicQuery( ins_user_sql );
				   if (numrows == 1)  {
					   result = (runBasicQuery( ins_sql ) == 1);
				   }
			   }
		   }
	   }
		   
	   if (result) {
		   addTransaction(username, amount, game, tzx_type, profile);
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
   public void cashOut(String username, int amount, ProfileType prof_type)
		   throws DBException, SQLException {
	   String sql = "UPDATE " + UserProfilesTable.Name + 
		   		" SET " + UserProfilesTable.Col_Amount + " = ("
		    			   + UserProfilesTable.Col_Amount + " + " + Integer.toString(amount) + ")" +		   		
		    	" WHERE " + UserProfilesTable.Col_UserID + " = (" + getUserIDSQL(username) + ") AND "
		 			  	  + UserProfilesTable.Col_TypeID + " = (" + getProfileIDSQL(prof_type) + ")";

	   runBasicQuery(sql);
	   addTransaction(username, amount, GamesType.POKER, TransactionType.POKER_CASHOUT, prof_type);	   
   }
   
   /**
    * Updates the global jackpot total
    * 
    * @param prof_type	The profile the jackpot is for
    * @param amount		The amount to increase it by
    */
   public void updateJackpot(ProfileType prof_type, double amount)
		   throws DBException, SQLException {
	   String sql = "INSERT INTO " + JackpotTable.Name + "("
			   							+ JackpotTable.Col_Profile + ","
			   							+ JackpotTable.Col_Total + ")" + 
			   		" VALUES ((" + getProfileIDSQL(prof_type) + "), '" + amount + "')" +
			   		" ON DUPLICATE KEY UPDATE " + JackpotTable.Col_Total +
			   								 " = " + JackpotTable.Col_Total + " + " + amount;
	   runBasicQuery(sql);
   }
   
   /**
    * Retrieves the jackpot from the database
    * 
    * @param prof_type	The profile the jackpot is for
    * @param amount		The amount to increase it by
    */
   public double getJackpot(ProfileType prof_type) throws DBException, SQLException {
	   String sql = "SELECT " + JackpotTable.Col_Total + " FROM " + JackpotTable.Name +
			   		" WHERE " + JackpotTable.Col_Profile + " = (" + getProfileIDSQL(prof_type) + ")";
	   double res = runGetDblQuery(sql);
	   return (res <= 0.0 ? 0.0 : res);
   }
   
   /**
    * Adds the winnings from a jackpot
    * 
    * @param username	The player's username
    * @param amount		The cash out value
    */
   public void jackpot(String username, int amount, ProfileType prof_type) 
		   throws DBException, SQLException {
	   String sql = "UPDATE " + UserProfilesTable.Name + 
		   		" SET " + UserProfilesTable.Col_Amount + " = ("
		    			   + UserProfilesTable.Col_Amount + " + " + Integer.toString(amount) + ")" +		   		
		    	" WHERE " + UserProfilesTable.Col_UserID + " = (" + getUserIDSQL(username) + ") AND "
		 			  	  + UserProfilesTable.Col_TypeID + " = (" + getProfileIDSQL(prof_type) + ")";
	     
	   String reset = "INSERT INTO " + JackpotTable.Name + "("
							+ JackpotTable.Col_Profile + ","
							+ JackpotTable.Col_Total + ")" + 
					" VALUES ((" + getProfileIDSQL(prof_type) + "), '0')" +
					" ON DUPLICATE KEY UPDATE " + JackpotTable.Col_Total + " = '0'";
	   
	   runBasicQuery(sql);
	   runBasicQuery(reset);	 
	   addTransaction(username, amount, GamesType.ADMIN, TransactionType.JACKPOT, prof_type);	   
   }
   
   /**
    * Removes poker chips from a player's balance
    * We don't care if the user doesn't exist as they needed to exist to join the table
    * 
    * @param username	The player's username
    * @param amount		The cash out value
    */
   public void buyIn(String username, int amount, ProfileType prof_type) 
		   throws DBException, SQLException {
	   String sql = "UPDATE " + UserProfilesTable.Name + 
			   		" SET " + UserProfilesTable.Col_Amount + " = ("
			    			   + UserProfilesTable.Col_Amount + " - " + Integer.toString(amount) + ")" +		   		
			    	" WHERE " + UserProfilesTable.Col_UserID + " = (" + getUserIDSQL(username) + ") AND "
			 			  	  + UserProfilesTable.Col_TypeID + " = (" + getProfileIDSQL(prof_type) + ")";
	   
	   runBasicQuery(sql);
	   addTransaction(username, -amount, GamesType.POKER, TransactionType.POKER_BUYIN, prof_type);
	}
   
   /**
    * Getter method for the next hand ID
    * 
    * Performs an SQL statement on the DB where the new hand is created with a blank winner
    * 
    * @return  The handID
    */
   public int getHandID() throws DBException, SQLException {
	   String sql = "INSERT INTO " + PokerHandsTable.Name +
	   			" ( " + PokerHandsTable.Col_WinnerID + 
	   			", " + PokerHandsTable.Col_Amount + ") " +
	   			"VALUES ('0', '0')";

	   return runGetIDQuery(sql);
   }
   
   /**
    * Updates the hand table with the hand winner and pot size
    * 
    * @param hand_id	The ID
    * @param username	The winner
    * @param pot		The pot size
    */
   public void setHandWinner(int hand_id, String username, int pot)
		   throws DBException, SQLException {
	   String sql = "UPDATE " + PokerHandsTable.Name +
				    " SET " + PokerHandsTable.Col_WinnerID + " = (" + getUserIDSQL(username) + "), "
				           + PokerHandsTable.Col_Amount + " = '" + Integer.toString(pot) + "' " +
	   				"WHERE " + PokerHandsTable.Col_ID + " = '" + Integer.toString(hand_id) + "'";

	   runBasicQuery(sql);
   }
   
   /**
    * Updates the hand table with the hand winner and pot size
    * 
    * @param hand_id	The ID
    * @param username	The winner
    * @param pot		The pot size
    */
   public void addHandWinner(int hand_id, String username, int pot)
		   throws DBException, SQLException {
	   String sql = "INSERT INTO " + PokerHandsTable.Name + "("
			   						+ PokerHandsTable.Col_WinnerID + ", "
			   						+ PokerHandsTable.Col_Amount + ", "
			   						+ PokerHandsTable.Col_ID + ") VALUES( " 
			   						+ "(" + getUserIDSQL(username) + "),'" + Integer.toString(pot) + "','" + Integer.toString(hand_id) + "')";

	   runBasicQuery(sql);
   }
   
   /**
    * Gets a user's current position in the weekly competition
    * 
    * @param profile	The profile to check
    * @param user		The username
    * 
    * @return 	The position
    * 
    * @throws DBException
    * @throws SQLException
    */
   public BetterInfo competitionPosition(ProfileType profile, String user) 
		   throws DBException, SQLException {
	   String sql = "SELECT t.position FROM (SELECT c.*,(@position:=@position+1) AS position" +
			   		" FROM " + CompetitionView.Name + " c, (SELECT @position:=0) p WHERE "
			   				+ CompetitionView.Col_Profile + " LIKE '" + profile.toString() + "') t" +
			   		" WHERE " + CompetitionView.Col_Username + " LIKE '" + user + "'";
	   String csql = "SELECT " + CompetitionView.Col_Total + " FROM " + CompetitionView.Name
			   		+ " WHERE " + CompetitionView.Col_Profile + " LIKE '" + profile.toString() + "' AND "
			   					+ CompetitionView.Col_Username + " LIKE '" + user + "'";

	   return new BetterInfo(user, runGetIntQuery(sql), runGetLongQuery(csql));
	   
   }
   
   /**
    * Updates the competition id and time
    * 
    * @throws DBException
    * @throws SQLException
    */
   public void competitionEnd()  throws DBException, SQLException {
	   String sql = "UPDATE " + CompetitionIDTable.Name +
			   		" SET " + CompetitionIDTable.Col_ID + " = (" + CompetitionIDTable.Col_ID + " + 1), "
			   				+ CompetitionIDTable.Col_Ends + " = ADDDATE("
			   											  + CompetitionIDTable.Col_Ends + ", INTERVAL 7 DAY)";

	   runBasicQuery(sql);
   }
   
   /**
    * Checks if the competition for the current week is ended
    * 
    * @return true if the competition is over
    * 
    * @throws DBException
    * @throws SQLException
    */
   public boolean competitionOver() throws DBException, SQLException{
	   String sql = "SELECT IF(TIMESTAMPDIFF(MINUTE, CURRENT_TIMESTAMP(), " +
			   		CompetitionIDTable.Col_Ends + ") > 0, '1', '0')" +
			   		"FROM " + CompetitionIDTable.Name + " LIMIT 1";
	   return ( runGetIntQuery(sql) == 1 ? true : false);
   }
   
   /**
    * Checks if the competition for the current week is ended
    * 
    * @return the number of seconds until the competition ends
    * 
    * @throws DBException
    * @throws SQLException
    */
   public int getCompetitionTimeLeft() throws DBException, SQLException{
	   String sql = "SELECT (UNIX_TIMESTAMP(" +
			   		CompetitionIDTable.Col_Ends + ") - UNIX_TIMESTAMP(NOW()))" +
			   		" FROM " + CompetitionIDTable.Name + " LIMIT 1";
	   int res = runGetIntQuery(sql);
	   return ( res < 0 ? 0 : res );
   }
   
   /**
    * Gets the highest better for a profile
    * 
    * @param profile	The profile to check
    * @param profile	The profile to check
    * 
    * @return A list of users of the highest better for the provided profile
    * 
    * @throws DBException
    * @throws SQLException
    */
	public List<BetterInfo> getCompetition(ProfileType profile, int number) throws DBException, SQLException {
		String sql = "SELECT " + CompetitionView.Col_Username + ","
				   			   + CompetitionView.Col_Total + 
					 " FROM " + CompetitionView.Name +
					 " WHERE " + CompetitionView.Col_Profile + " LIKE '" + profile.toString() + "'" +
					 " ORDER BY " + CompetitionView.Col_Total + " DESC " +
					 " LIMIT 0, " + Integer.toString(number);
		
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String user = null;
	   long total = -1;
	   List<BetterInfo> winners = new ArrayList<BetterInfo>();
	   try {
		   try {
			   conn = getConnection();
			   stmt = conn.createStatement();
			   stmt.setMaxRows(number);
			   rs = stmt.executeQuery(sql);
			   
			   while ( rs.next() ) {
				   user = rs.getString(TotalBetsView.Col_Username);
				   total = rs.getLong(TotalBetsView.Col_Total);
				   winners.add( new BetterInfo(user, total) );
			   }
		   } catch (SQLException e) {
			   throw new DBException(e.getMessage(), sql);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				throw e;
		   }
	   }

	   return winners;
	}
   
   /**
    * Gets the highest better for a profile
    * 
    * @param profile	The profile to check
    * 
    * @return The username of the highest better for the provided profile
    * 
    * @throws DBException
    * @throws SQLException
    */
	public BetterInfo getTopBetter(ProfileType profile) throws DBException, SQLException {
		return getTopBetter(profile, null);
	}
	public BetterInfo getTopBetter(ProfileType profile, String who) throws DBException, SQLException {
		String sql = "SELECT " + TotalBetsView.Col_Username + ","
				   			   + TotalBetsView.Col_Total + 
					 " FROM " + TotalBetsView.Name +
					 " WHERE " + TotalBetsView.Col_Profile + " LIKE '" + profile.toString() + "'";
		
		if (who != null) sql += " AND " + TotalBetsView.Col_Username + " LIKE '" + who + "'";
		
		sql += " ORDER BY " + TotalBetsView.Col_Total + " DESC LIMIT 1";
		
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String user = null;
	   long total = -1;
	   try {
		   try {
			   conn = getConnection();
			   stmt = conn.createStatement();
			   stmt.setMaxRows(1);
			   rs = stmt.executeQuery(sql);
			   
			   if ( rs.next() ) {
				   user = rs.getString(TotalBetsView.Col_Username);
				   total = rs.getLong(TotalBetsView.Col_Total);
			   }
		   } catch (SQLException e) {
			   throw new DBException(e.getMessage(), sql);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				throw e;
		   }
	   }

		return new BetterInfo(user, total);
	}
	
   /**
    * Gets the highest single bet for a profile
    * 
    * @param profile	The profile to check
    * 
    * @return The username of the highest better for the provided profile
    * 
    * @throws DBException
    * @throws SQLException
    */
	public BetterInfo getHighestBet(ProfileType profile) throws DBException, SQLException {
		return getHighestBet(profile, null);
	}
	public BetterInfo getHighestBet(ProfileType profile, String who) throws DBException, SQLException {
		String sql = "SELECT " + OrderedBetsView.Col_Username + ","
							   + OrderedBetsView.Col_Game + ","
							   + OrderedBetsView.Col_Total + 
					 " FROM " + OrderedBetsView.Name +
					 " WHERE " + OrderedBetsView.Col_Profile + " LIKE '" + profile.toString() + "'";
		
		if (who != null) sql += " AND " + OrderedBetsView.Col_Username + " LIKE '" + who + "'";
		
		sql += " ORDER BY " + OrderedBetsView.Col_Total + " DESC LIMIT 1";
		
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String user = null;
	   String game = null;
	   long total = -1;
	   try {
		   try {
			   conn = getConnection();
			   stmt = conn.createStatement();
			   stmt.setMaxRows(1);
			   rs = stmt.executeQuery(sql);
			   
			   if ( rs.next() ) {
				   user = rs.getString(OrderedBetsView.Col_Username);
				   game = rs.getString(OrderedBetsView.Col_Game);
				   total = rs.getLong(OrderedBetsView.Col_Total);
			   }
		   } catch (SQLException e) {
			   throw new DBException(e.getMessage(), sql);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				throw e;
		   }
	   }

		return new BetterInfo(user, game, total);
	}
	
   /**
    * Buys a number of lottery ticket.
    * 
    * @param user	The user buying the tickets.
    * @param profile The profile being used
    * @param amount	 The number of tickets to buy
    * 
    * @throws DBException
    * @throws SQLException
    */
	public boolean buyLotteryTickets(String username, ProfileType profile, int amount)
				  throws DBException, SQLException {
		adjustChips( username, (0-amount), profile, GamesType.LOTTERY, TransactionType.LOTTERY );

		String sql = "INSERT INTO " + LotteryTicketsTable.Name + " (" +
								LotteryTicketsTable.Col_ID + ", " +
								LotteryTicketsTable.Col_UserID + ", " +
								LotteryTicketsTable.Col_ProfileID + ") VALUES ";
		
		int user_id = runGetIntQuery( getUserIDSQL(username) );
		int profile_id = runGetIntQuery( getProfileIDSQL(profile) );
		
		boolean ret = true;
		
		if (user_id != -1 && profile_id != -1) {
			String values = "(NULL, '" + user_id + "', '" + profile_id + "')";
		
			for (int i = 1; i <= amount; i++) { 
				sql += values;
				if (i != amount) sql += ", ";
			}
		
			ret = (runBasicQuery(sql) > 0);
		}
		
		return ret;
	}
	
   /**
    * Returns the size of the lottery
    * 
    * @param profile The profile being used
    * 
    * @throws DBException
    * @throws SQLException
    */
	public int getLotteryTickets(ProfileType profile)
				  throws DBException, SQLException {
		String sql = "SELECT COUNT(*) FROM " + LotteryTicketsTable.Name +
	  				" WHERE " + LotteryTicketsTable.Col_ProfileID + " = " + "(" + getProfileIDSQL(profile) + ")";
		
		return runGetIntQuery(sql);
	}
	
	
   /**
    * Returns the size of the lottery
    * 
    * @param profile The profile being used
    * 
    * return The random winner.
    * 
    * @throws DBException
    * @throws SQLException
    */
	public String getLotteryWinner(ProfileType profile)
				  throws DBException, SQLException {
		String sql = "SELECT u." + UsersTable.Col_Username + " FROM " + LotteryTicketsTable.Name + " l " +
				  	 "JOIN " + UsersTable.Name + " u ON l." + LotteryTicketsTable.Col_UserID + " = u." + UsersTable.Col_ID + 
	  				" WHERE " + LotteryTicketsTable.Col_ProfileID + " = " + "(" + getProfileIDSQL(profile) + ")" +
	  				" ORDER BY RAND() LIMIT 1";
		
		return runGetStringQuery(sql);
	}

   /**
    * Starts a new lottery by wiping the current tickets.
    * 
    * @throws DBException
    * @throws SQLException
    */
	public void endLottery()
				  throws DBException, SQLException {
		String sql = "DELETE FROM " + LotteryTicketsTable.Name;
		
		runBasicQuery(sql);
	}
    
    public boolean isRank(String user) throws DBException, SQLException {
        String sql = "SELECT COUNT(*) FROM " + HostGroupUsersTable.Name +
                " WHERE " + HostGroupUsersTable.Col_UserID + " = (" + getUserIDSQL(user) + ")";

        return (runGetIntQuery(sql) > 0);
    }


    public void addRank(String user, String group) throws DBException, SQLException {
        String sql = "INSERT INTO " + HostGroupUsersTable.Name +
                        "(" + HostGroupUsersTable.Col_UserID + ", " 
                             + HostGroupUsersTable.Col_GroupID + ")" +
                     " VALUES((" + getUserIDSQL(user) + "), (" + getRankGroupIDSQL(group) + "))";
        runBasicQuery(sql);
    }
    
    public String getRankGroup(String user) throws DBException, SQLException {
        String sql = "SELECT hg." + HostGroupsTable.Col_Name
                        + " FROM " + HostGroupUsersTable.Name + " hgu"
                        + " JOIN " + HostGroupsTable.Name + " hg ON "
                           + "hg." + HostGroupsTable.Col_ID + " = "
                           + "hgu." + HostGroupUsersTable.Col_GroupID
                     + " WHERE hgu." + HostGroupUsersTable.Col_UserID + " = (" + getUserIDSQL(user) + ")";
        return runGetStringQuery(sql);        
    }
    
    public void updateRank(String user, String group) throws DBException, SQLException {
        String sql = "UPDATE " + HostGroupUsersTable.Name +
                " SET " + HostGroupUsersTable.Col_GroupID + " = (" + getRankGroupIDSQL(group) + ")" +
                " WHERE " + HostGroupUsersTable.Col_UserID + " = (" + getUserIDSQL(user) + ")";
        runBasicQuery(sql);
    }

    public void kickRank(String user) throws DBException, SQLException {
        String sql = "DELETE FROM " + HostGroupUsersTable.Name +
                     " WHERE " + HostGroupUsersTable.Col_UserID + " = (" + getUserIDSQL(user) + ")";
        runBasicQuery(sql);
    }

    public boolean isRankGroup(String group) throws DBException, SQLException {
        String sql = "SELECT COUNT(*) FROM " + HostGroupsTable.Name +
                " WHERE " + HostGroupsTable.Col_Name + " LIKE '" + group + "'";

        return (runGetIntQuery(sql) > 0);
    }

    public void deleteRankGroup(String group) throws DBException, SQLException {
        String sql = "DELETE FROM " + HostGroupsTable.Name +
                     " WHERE " + HostGroupsTable.Col_Name + " LIKE '" + group + "'";
        runBasicQuery(sql);
    }

    public void newRankGroup(String group) throws DBException, SQLException {
        String sql = "INSERT INTO " + HostGroupsTable.Name + "(" + HostGroupsTable.Col_Name + ")" +
                     " VALUES('" + group + "')";

        checkUserExists(group, group + "!" + group + "@" + group);
        runBasicQuery(sql);
    }

    public void renameRankGroup(String oldgroup, String newgroup) throws DBException, SQLException {
        String sql = "UPDATE " + HostGroupsTable.Name +
                     " SET " + HostGroupsTable.Col_Name + " = '" + newgroup + "'" +
                     " WHERE " + HostGroupsTable.Col_Name + " LIKE '" + oldgroup + "'";
        runBasicQuery(sql);
        
        sql = "UPDATE " + UsersTable.Name +
                " SET " + UsersTable.Col_Username + " = '" + newgroup + "'" +
                " WHERE " + UsersTable.Col_Username + " LIKE '" + oldgroup + "'";
        runBasicQuery(sql);
    }
    
    public List<String> listRankGroupUsers(String group)
            throws DBException, SQLException {
        List<String> res = new ArrayList<String>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT u." + UsersTable.Col_Username +
                     " FROM " + HostGroupUsersTable.Name + " hgu" +
                     " JOIN " + UsersTable.Name + " u ON u." + UsersTable.Col_ID + " = "
                                                + "hgu." + HostGroupUsersTable.Col_UserID +
                     " WHERE " + HostGroupUsersTable.Col_GroupID + " = ("
                                 + getRankGroupIDSQL(group) + ")";
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while ( rs.next() ) {
                res.add(rs.getString("u." + UsersTable.Col_Username));
            }
        } catch (SQLException e) {
           throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                   throw e;
            }
        }
        return res;
    }
    
    public List<String> listRankGroups() throws DBException, SQLException {
        List<String> res = new ArrayList<String>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT " + HostGroupsTable.Col_Name +
                     " FROM " + HostGroupsTable.Name +
                     " WHERE 1";
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while ( rs.next() ) {
                res.add(rs.getString(HostGroupsTable.Col_Name));
            }
        } catch (SQLException e) {
           throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                   throw e;
            }
        }
        return res;
    }
    
    public void addReferer(String user, String referrer) throws DBException, SQLException {
        String sql = "INSERT INTO " + ReferersTable.Name
                            + "(" + ReferersTable.Col_UserID + ", "
                                  + ReferersTable.Col_RefererID + ")" +
                     " VALUES((" + getUserIDSQL(user) + "), (" + getUserIDSQL(referrer) + "))";
                       
        runBasicQuery(sql);
    }
    
    public void delReferer(String user, String referrer) throws DBException, SQLException {
        String sql = "DELETE FROM " + ReferersTable.Name + " WHERE "
                      + ReferersTable.Col_UserID + " = (" + getUserIDSQL(user) + ")"
                      + ReferersTable.Col_RefererID + " = (" + getUserIDSQL(referrer) + ")";
                       
        runBasicQuery(sql);
    }
    
	public ReferrerType getRefererType(String username) throws DBException, SQLException {
	    String sql = "SELECT COUNT(*) FROM " + FullReferersTextView.Name +
	                 " WHERE " + FullReferersTextView.Col_Username + " LIKE '" + username + "'";

        String grpsql = "SELECT COUNT(*) FROM " + FullReferersTextView.Name +
                        " WHERE " + FullReferersTextView.Col_Username + " LIKE '" + username + "'" +
                          " AND " + FullReferersTextView.Col_Group + " IS NOT NULL";
	    
	    int res = runGetIntQuery(sql);
	    ReferrerType ret = ReferrerType.NONE;
	    if (res != 0) {
            int grpres = runGetIntQuery(grpsql);
	        if (grpres >= 1 || res > 1) {
	            ret = ReferrerType.GROUP;	
	        } else {
	            ret = ReferrerType.PUBLIC;
	        }
	    }
	    
	    return ret;
	}
	
	public List<ReferalUser> getReferalUsers(String user) throws DBException, SQLException {
        String sql = "SELECT " + "t." + FullReferersTextView.Col_Username + ","
                                + "t." + FullReferersTextView.Col_Group + 
                     " FROM " + FullReferersTextView.Name + " t" +
                     " WHERE " + FullReferersTextView.Col_Referer + " LIKE '" + user + "'";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<ReferalUser> referers = new ArrayList<ReferalUser>();
        String ref = null;
        String group = null;
        try {
            try {
                conn = getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
    
                while ( rs.next() ) {
                    ref = rs.getString("t." + FullReferersTextView.Col_Username);
                    group = rs.getString("t." + FullReferersTextView.Col_Group);
                    referers.add( new ReferalUser(ref, group) );
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
             throw e;
            }
        }
	    return referers;
	}
   
	public void giveReferalFee(double fee, String user, ProfileType profile)
	        throws DBException, SQLException {
	    adjustChips(user, fee, profile, GamesType.ADMIN, TransactionType.REFERRAL);	    
	}
	
	public void houseFees(double fee, ProfileType profile) throws DBException, SQLException {
	    checkUserExists("HOUSE", "HOUSE!HOUSE@HOUSE");
        adjustChips("HOUSE", fee, profile, GamesType.ADMIN, TransactionType.REFERRAL);   
	}
   
   /**
    * Adds a new transaction to the transaction table
    * 
    * @param username 	The username 
    * @param amount		The amount
    * @param tzx_type	The type of transaction
    */
   public void addTransaction(String username, int amount,
							  GamesType game_type,
							  TransactionType tzx_type,
							  ProfileType prof_type) 
					  throws DBException, SQLException {
	   addTransaction(username, (double)amount, game_type, tzx_type, prof_type);
   }
   public void addTransaction(String username, double amount,
		   					  GamesType game_type,
		   					  TransactionType tzx_type,
		   					  ProfileType prof_type) 
		   							  throws DBException, SQLException { 
	   String sql = "INSERT INTO " + TransactionsTable.Name + "(" 
  							+ TransactionsTable.Col_TypeID + ", " 
			   				+ TransactionsTable.Col_GameID + ", "			   				
			   				+ TransactionsTable.Col_UserID + ", "
			   				+ TransactionsTable.Col_Amount + ", "
					   		+ TransactionsTable.Col_ProfileType + ") VALUES("
			   				  + "(" + getTzxTypeIDSQL(tzx_type) + "), "
			   				  + "(" + getGameIDSQL(game_type) + "), "
			   				  + "(" + getUserIDSQL(username) + "), "
					   		  + "(" + Double.toString(amount) + "), "
			   				  + "(" + getProfileIDSQL(prof_type) + "))";
	   
	   runBasicQuery(sql);
   }
   
   /**
    * Runs a single query that returns a single column and row
    * 
    * @param query	The query to execute
    * 
    * @return		The resulting String
    * 
    * @throws DBException
    * @throws SQLException
    */
   private String runGetStringQuery(String query) throws DBException, SQLException {
       query = query.replaceAll("_", "\\_");
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   String ret = null;
	   try {
		   try {
			   conn = getConnection();
			   stmt = conn.createStatement();
			   stmt.setMaxRows(1);
			   rs = stmt.executeQuery(query);
			   
			   if ( rs.next() ) {
				   ret = rs.getString(1);
			   }
		   } catch (SQLException e) {
			   throw new DBException(e.getMessage(), query);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				throw e;
		   }
	   }
	   return ret;
   }
      
   /**
    * Runs a single query that returns a single column and row
    * 
    * @param query	The query to execute
    * 
    * @return		The resulting integer
    * 
    * @throws DBException
    * @throws SQLException
    */
   private int runGetIntQuery(String query) throws DBException, SQLException {
       query = query.replaceAll("_", "\\_");
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   int ret = -1;
	   try {
		   try {
			   conn = getConnection();
			   stmt = conn.createStatement();
			   stmt.setMaxRows(1);
			   rs = stmt.executeQuery(query);
			   
			   if ( rs.next() ) {
				   ret = rs.getInt(1);
			   }
		   } catch (SQLException e) {
			   throw new DBException(e.getMessage(), query);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				throw e;
		   }
	   }
	   return ret;
   }
   
   /**
    * Runs a single query that returns a single column and row
    * 
    * @param query  The query to execute
    * 
    * @return       The resulting integer
    * 
    * @throws DBException
    * @throws SQLException
    */
   private double runGetDblQuery(String query) throws DBException, SQLException {
       query = query.replaceAll("_", "\\_");
       Connection conn = null;
       Statement stmt = null;
       ResultSet rs = null;
       double ret = -1;
       try {
           try {
               conn = getConnection();
               stmt = conn.createStatement();
               stmt.setMaxRows(1);
               rs = stmt.executeQuery(query);
               
               if ( rs.next() ) {
                   ret = rs.getDouble(1);
               }
           } catch (SQLException e) {
               throw new DBException(e.getMessage(), query);
           }
       } catch (DBException ex) {
           throw ex;
       } finally {
           try {
               if (rs != null) rs.close();
               if (stmt != null) stmt.close();
               if (conn != null) conn.close();
           } catch (SQLException e) {
                throw e;
           }
       }
       return ret;
   }
   
   /**
    * Runs a single query that returns a single column and row
    * 
    * @param query	The query to execute
    * 
    * @return		The resulting integer
    * 
    * @throws DBException
    * @throws SQLException
    */
   private long runGetLongQuery(String query) throws DBException, SQLException {
       query = query.replaceAll("_", "\\_");
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   long ret = -1;
	   try {
		   try {
			   conn = getConnection();
			   stmt = conn.createStatement();
			   stmt.setMaxRows(1);
			   rs = stmt.executeQuery(query);
			   
			   if ( rs.next() ) {
				   ret = rs.getLong(1);
			   }
		   } catch (SQLException e) {
			   throw new DBException(e.getMessage(), query);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				throw e;
		   }
	   }
	   return ret;
   }
   
   /**
    * Runs a single query that returns an ID from the tabke
    * 
    * @param query	The query to execute
    * 
    * @return		The resulting int ID
    * 
    * @throws DBException
    * @throws SQLException
    */
   private int runGetIDQuery(String query) throws DBException, SQLException {
       query = query.replaceAll("_", "\\_");
	   Connection conn = null;
	   Statement stmt = null;
	   ResultSet rs = null;
	   int ret = -1;
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   try {
			   stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
		       rs = stmt.getGeneratedKeys();
		       if (rs.next()) {
		           ret = rs.getInt(1);
		       } else {
		           throw new RuntimeException("Can't find most recent hand ID we just created");
		       }
		   } catch (SQLException e) {
			   throw new DBException(e.getMessage(), query);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (rs != null) rs.close();
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				throw e;
		   }
	   }
	   return ret;
   }
   
   /**
    * Runs a single query that returns nothing
    * 
    * @param query	The query to execute
    * 
    * @return the number of rows affected.
    * 
    * @throws DBException
    * @throws SQLException
    */
   private int runBasicQuery(String query) throws DBException, SQLException {
       query = query.replaceAll("_", "\\_");
	   Connection conn = null;
	   Statement stmt = null;
	   int numrows = -1;
	   try {
		   conn = getConnection();
		   stmt = conn.createStatement();
		   try {
			   numrows = stmt.executeUpdate(query);
		   } catch (SQLException e) {
			   throw new DBException(e.getMessage(), query);
		   }
	   } catch (DBException ex) {
		   throw ex;
	   } finally {
		   try {
			   if (stmt != null) stmt.close();
			   if (conn != null) conn.close();
		   } catch (SQLException e) {
				throw e;
		   }
	   }
	   return numrows;
   }
   
   /**
    * Provides the SQL for retrieving a user's ID
    * 
    * @param username The username
    * 
    * @return The ID
    */
   private static final String getUserIDSQL(String username) {
	   String out = "SELECT uu." + UsersTable.Col_ID +
					" FROM " + UsersTable.Name + " uu" +
					" WHERE uu." + UsersTable.Col_Username +
					" LIKE '" + username + "' LIMIT 1";
	   return out;
   }
   
   /**
    * Provides the SQL for retrieving the Poker game ID
    * 
    * @return The ID
    */
   private static final String getGameIDSQL(GamesType game) {
	   String out = "SELECT gg." + GamesTable.Col_ID +
					" FROM " + GamesTable.Name + " gg" +
					" WHERE gg." + GamesTable.Col_Name +
					" LIKE '" + game.toString() + "' LIMIT 1";
	   return out;
   }
   
   /**
    * Provides the SQL for checking a profile type
    */
   private static final String getProfileIDSQL(ProfileType profile) {
	   String prof_sql = "SELECT pt." + ProfileTypeTable.Col_ID
 	  			+ " FROM " + ProfileTypeTable.Name + " pt"
 	  			+ " WHERE pt." + ProfileTypeTable.Col_Name
 	  			+ " LIKE " + "'" + profile.toString() + "'";
	   return prof_sql;
   }
   
   /**
    * Provides the SQL for getting a transaction type ID
    */
   private static final String getTzxTypeIDSQL(TransactionType tzx_type) {
	   String out = "SELECT tt." + TransactionTypesTable.Col_ID
	   			  + " FROM " + TransactionTypesTable.Name + " tt"
		   		  + " WHERE tt." + TransactionTypesTable.Col_Type
		   		  + " LIKE '" +  tzx_type.toString() + "'";
	   return out;
   }
   
   /**
    * Provides the SQL for getting a host group ID
    */
   private static final String getRankGroupIDSQL(String group) {
       String out = "(SELECT hg." + HostGroupsTable.Col_ID + " FROM " + HostGroupsTable.Name + " hg" +
                    " WHERE hg." + HostGroupsTable.Col_Name + " LIKE '" + group + "')";
       return out;
   }
}
