/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.smokinmils.bot.Utils;
import org.smokinmils.database.tables.AuctionProfilesTable;
import org.smokinmils.database.tables.AuctionsTable;
import org.smokinmils.database.tables.BetsTable;
import org.smokinmils.database.tables.ChipsTransactionsTable;
import org.smokinmils.database.tables.CompetitionIDTable;
import org.smokinmils.database.tables.CompetitionView;
import org.smokinmils.database.tables.FullReferersTextView;
import org.smokinmils.database.tables.GamesTable;
import org.smokinmils.database.tables.HostGroupUsersTable;
import org.smokinmils.database.tables.HostGroupsTable;
import org.smokinmils.database.tables.HostmasksTable;
import org.smokinmils.database.tables.JackpotTable;
import org.smokinmils.database.tables.LotteryTicketsTable;
import org.smokinmils.database.tables.OrderedBetsView;
import org.smokinmils.database.tables.PokerBetsTable;
import org.smokinmils.database.tables.PokerHandsTable;
import org.smokinmils.database.tables.ProfileTypeTable;
import org.smokinmils.database.tables.ReferersTable;
import org.smokinmils.database.tables.TotalBetsView;
import org.smokinmils.database.tables.TransactionTypesTable;
import org.smokinmils.database.tables.TransactionsTable;
import org.smokinmils.database.tables.UserProfilesTable;
import org.smokinmils.database.tables.UserProfilesView;
import org.smokinmils.database.tables.UsersTable;
import org.smokinmils.database.tables.UsersView;
import org.smokinmils.database.types.BetterInfo;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.ReferalUser;
import org.smokinmils.database.types.ReferrerType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.database.types.UserCheck;
import org.smokinmils.settings.DBSettings;

import com.mchange.v2.c3p0.DataSources;

/**
 * A singleton Database access system for the poker bot.
 * 
 * @author Jamie Reid
 */
public final class DB {
    /** The user for the points' chips. */
    public static final String POINTS_USER = "POINTS";

    /** The user for the house chips. */
    public static final String HOUSE_USER = "HOUSE";

    /** Number of minutes the connections can be idle for. */
    private static final int MAX_IDLE_MINS = 15;

    /** Instance variable. */
    private static DB instance;
    static {
        try {
            instance = new DB();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Static 'instance' method.
     * 
     * @return the singleton Instance
     */
    public static DB getInstance() {
        return instance;
    }

    /** The un-pooled data source. */
    private final DataSource    unpooled;

    /** The pooled data source. */
    private final DataSource    pooled;

    /** The database URL. */
    private final String        url         = "jdbc:mysql://"
                                            + DBSettings.SERVER + ":"
                                            + DBSettings.PORT + "/"
                                            + DBSettings.DB_NAME
                                            + "?autoReconnect=true";

    /** This is used to adjust bets when they are approaching zero. */
    private static final double CHIPS_LTONE = 1.0;

    /** This is used to adjust bets when they aren't approaching zero. */
    private static final double CHIPS_OTHER = 0.05;

    /**
     * Constructor.
     * 
     * @throws Exception when we fail to retrieve a connection
     * 
     * TODO: clean up datasources with a close method.
     *  see http://www.mchange.com/projects/c3p0/
     */
    private DB() throws Exception {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("minPoolSize", "12");
        props.put("initialPoolSize", "12");
        props.put("maxPoolSize", "30");
        props.put("numHelperThreads", "12");
        props.put("aquireIncrement", "5");
        props.put("maxIdleTime", new Integer(MAX_IDLE_MINS * Utils.MS_IN_MIN));
        
        unpooled = DataSources.unpooledDataSource(url,
                                                  DBSettings.DB_USER,
                                                  DBSettings.DB_PASS);
        pooled = DataSources.pooledDataSource(unpooled, props);
    }

    /**
     * Getter method for a connection from the connection pool.
     * 
     * @return A connection
     * 
     * @throws SQLException when an error occurs getting a connection from the
     *             pool
     */
    private Connection getConnection()
        throws SQLException {
        return pooled.getConnection();
    }

    /**
     * Adds a new hostmask to the DB for this user.
     * 
     * If the user does not exist, we need to add the user to the user table
     * 
     * @param username The user
     * @param hostmask The hostmask
     * 
     * @throws SQLException when a database error occurs
     */
    public void addHostmask(final String username,
                            final String hostmask) throws SQLException {
        String inshost = "INSERT IGNORE INTO " + HostmasksTable.NAME + "("
                + HostmasksTable.COL_HOST + ", " + HostmasksTable.COL_USERID
                + ") " + "VALUES('" + hostmask + "', '%userid')";

        int userid = runGetIntQuery(getUserIDSQL(username));
        if (userid != -1) {
            inshost = inshost.replaceAll("%userid", Integer.toString(userid));
            runBasicQuery(inshost);
        }
    }

    /**
     * Adds a bet to the database.
     * 
     * @param username The user making the bet
     * @param choice The bet choice
     * @param amount The bet amount
     * @param profile The profile the bet is from
     * @param game The game the bet is on
     * 
     * @return true if the query was run successfully.
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean addBet(final String username,
                          final String choice,
                          final double amount,
                          final ProfileType profile,
                          final GamesType game) throws SQLException {
        String sql = "INSERT INTO " + BetsTable.NAME + "("
                + BetsTable.COL_USERID + ", " + BetsTable.COL_AMOUNT + ", "
                + BetsTable.COL_CHOICE + ", " + BetsTable.COL_GAMEID + ", "
                + BetsTable.COL_PROFILE + ") " + " VALUES(("
                + getUserIDSQL(username) + "), " + "'"
                + Double.toString(amount) + "', " + "'" + choice + "', " + "("
                + getGameIDSQL(game) + "), " + "(" + getProfileIDSQL(profile)
                + "))";

        return runBasicQuery(sql) == 1;
    }

    /**
     * Deletes a bet from the database.
     * 
     * @param username The bet to remove
     * @param game The game represented as a unique string
     * 
     * @return true if the bet was deleted.
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean deleteBet(final String username,
                             final GamesType game)
        throws SQLException {
        String sql = "DELETE FROM " + BetsTable.NAME + " WHERE "
                + BetsTable.COL_USERID + " = (" + getUserIDSQL(username) + ")"
                + " AND " + BetsTable.COL_GAMEID + " = (" + getGameIDSQL(game)
                + ")";

        return runBasicQuery(sql) == 1;
    }

    
    /**
     * Adds an auction to the database!
     * @param itemname the name of the item
     * @param amount the startign amount
     * @param pr the auction profiles
     * @return yay or nay?
     * @throws SQLException when an error occues
     */
    public int addAuction(final String itemname,
                              final double amount,
                              final ArrayList<ProfileType> pr) 
                                      throws SQLException {
        String sql = "INSERT INTO " + AuctionsTable.NAME + "("
                + AuctionsTable.COL_ITEMNAME + ", " + AuctionsTable.COL_AMOUNT + ", "
                + AuctionsTable.COL_USERID + ") VALUES('"
                + itemname + "', " + String.valueOf(amount) + ", (" 
                + getUserIDSQL(HOUSE_USER) + "));";
         
        int newID = runGetIDQuery(sql);
        for (ProfileType p : pr) {
            sql = "INSERT INTO " + AuctionProfilesTable.NAME + "("
                    + AuctionProfilesTable.COL_AUCTIONID + ", " + AuctionProfilesTable.COL_PROFILEID
                    + ") VALUES(" + newID + ", (" + getProfileIDSQL(p) + "));";
           
            runBasicQuery(sql);
        }
        return newID;
    }
    
    /**
     * Updates an auction with the highest bid.
     * @param itemname the item we are updating...
     * @param amount the new amount 
     * @param username the user who is bidding
     * @return yay if it worked, nay if something went wrong :(
     * @throws SQLException when an error occurs
     */
    public boolean updateAuction(final String itemname,
                                 final double amount,
                                 final String username) throws SQLException {
        String sql = "UPDATE " + AuctionsTable.NAME + " SET " 
                                 + AuctionsTable.COL_AMOUNT + "=" + String.valueOf(amount) + ", " 
                                 + AuctionsTable.COL_USERID + "=(" + getUserIDSQL(username) 
                             + ") WHERE "
                                 + AuctionsTable.COL_ITEMNAME + "='" + itemname + "';";
        return runBasicQuery(sql) == 1;
    }
    
    /**
     * Ends an auction.
     * @param id the id of the item name we are ending
     * @return yay or nay
     * @throws SQLException  when an errors occures   
     */
    public boolean endAuction(final int id) throws SQLException {
        String sql = "UPDATE " + AuctionsTable.NAME + " SET " 
                + AuctionsTable.COL_FINISHED + "=1" 
            + " WHERE "
                + AuctionsTable.COL_ID + "=" + id + ";";
        return runBasicQuery(sql) == 1;
    }
    
    
    /**
     * Getter method for a user's active profile text.
     * 
     * Performs an SQL statement on the DB
     * 
     * @param username The profile name
     * 
     * @return The amount of credits
     * 
     * @throws SQLException when a database error occurs
     */
    public ProfileType getActiveProfile(final String username) throws SQLException {
        String sql = "SELECT " + UsersView.COL_ACTIVEPROFILE + " FROM "
                + UsersView.NAME + " WHERE " + UsersView.COL_USERNAME
                + " LIKE '" + username + "'";
        return ProfileType.fromString(runGetStringQuery(sql));
    }

    /**
     * Checks a user exists in the user table, if they don't adds them.
     * 
     * @param username The user's nickname
     * @param hostmask The user's hostmask
     * 
     * @return true false if we couldn't create an account.
     * 
     * @throws SQLException thrown when the database failed to execute.
     */
    public UserCheck checkUserExists(final String username,
                                     final String hostmask) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + UsersTable.NAME + " WHERE "
                + UsersTable.COL_USERNAME + " LIKE " + "'" + username + "'";

        String hostsql = "SELECT COUNT(" + HostmasksTable.COL_USERID
                + ") FROM " + HostmasksTable.NAME + " WHERE "
                + HostmasksTable.COL_HOST + " LIKE " + "'" + hostmask + "'";

        String insusersql = "INSERT INTO " + UsersTable.NAME + "("
                + UsersTable.COL_USERNAME + ") VALUES('" + username + "')";
        UserCheck ret = UserCheck.FAILED;

        // check the user exists
        if (runGetIntQuery(sql) < 1) {
            // they don't so check they don't have more than 1 accounts
            if (runGetIntQuery(hostsql) < 1) {
                runBasicQuery(insusersql);
                addHostmask(username, hostmask);
                ret = UserCheck.CREATED;
            } else {
                // too many accounts
                ret = UserCheck.FAILED;
            }
        } else {
            ret = UserCheck.EXISTED;
            addHostmask(username, hostmask);
        }

        return ret;
    }

    /**
     * Checks a user exists in the user table, if they don't adds them.
     * 
     * @param username The user's nickname
     * @param hostmask
     * 
     * @return true if user existed
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean checkUserExists(final String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + UsersTable.NAME + " WHERE "
                + UsersTable.COL_USERNAME + " LIKE " + "'" + username + "'";

        return runGetIntQuery(sql) == 1;
    }

    /**
     * Getter method for a user's credit count on the DB.
     * 
     * Performs an SQL statement on the DB
     * 
     * @param username The username
     * 
     * @return The amount of credits
     * 
     * @throws SQLException when a database error occurs
     */
    public int checkCreditsAsInt(final String username) throws SQLException {
        return (int) Math.floor(checkCredits(username));
    }

    /**
     * Getter method for a user's credit count on the DB.
     * 
     * Performs an SQL statement on the DB
     * 
     * @param username The username
     * 
     * @return The amount of credits
     * 
     * @throws SQLException when a database error occurs
     */
    public double checkCredits(final String username) throws SQLException {
        ProfileType active = getActiveProfile(username);
        return checkCredits(username, active);
    }

    /**
     * Getter method for a user's credit count on the DB.
     * 
     * Performs an SQL statement on the DB
     * 
     * @param username The username
     * @param amount The amount
     * 
     * @return The amount of credits
     * 
     * @throws SQLException when a database error occurs
     */
    public double checkCredits(final String username,
                               final Double amount) throws SQLException {
        double ret = 0.0;
        if (amount != null) {
            ProfileType active = getActiveProfile(username);
            double chips = checkCredits(username, active);
            double chipdiff = chips - amount;

            if (amount > chips) {
                if (-chipdiff <= CHIPS_LTONE) {
                    ret = chips;
                } else {
                    ret = 0.0;
                }
            } else if (amount <= chips) {
                if (chipdiff <= CHIPS_OTHER) {
                    ret = chips;
                } else {
                    ret = amount;
                }
            }
        }

        return ret;
    }

    /**
     * Getter method for a user's credit count on the DB.
     * 
     * Performs an SQL statement on the DB
     * 
     * @param username The username
     * @param profile The profile name
     * 
     * @return The amount of credits
     * 
     * @throws SQLException when a database error occurs
     */
    public int checkCreditsAsInt(final String username,
                                 final ProfileType profile) throws SQLException {
        return (int) Math.floor(checkCredits(username, profile));
    }

    /**
     * Getter method for a user's credit count on the DB.
     * 
     * Performs an SQL statement on the DB
     * 
     * @param username The username
     * @param profile The profile name
     * 
     * @return The amount of credits
     * 
     * @throws SQLException when a database error occurs
     */
    public double checkCredits(final String username,
                               final ProfileType profile) throws SQLException {
        String sql = "SELECT " + UserProfilesView.COL_AMOUNT + " FROM "
                + UserProfilesView.NAME + " WHERE "
                + UserProfilesView.COL_USERNAME + " = '" + username + "' AND "
                + UserProfilesView.COL_PROFILE + " = '" + profile.toString()
                + "'";

        double credits = runGetDblQuery(sql);
        if (credits < 0) {
            credits = 0;
        }
        return credits;
    }

    /**
     * Getter method for a user's credit count on the DB for all profiles.
     * 
     * Performs an SQL statement on the DB
     * 
     * @param username The username
     * 
     * @return The amount of credits
     * 
     * @throws SQLException when a database error occurs
     */
    public Map<ProfileType, Double> checkAllCredits(final String username) throws SQLException {
        Map<ProfileType, Double> res = new HashMap<ProfileType, Double>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT " + UserProfilesView.COL_PROFILE + ","
                + UserProfilesView.COL_AMOUNT + " FROM "
                + UserProfilesView.NAME + " WHERE "
                + UserProfilesView.COL_USERNAME + " LIKE '" + username + "'";

        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                res.put(ProfileType.fromString(rs
                        .getString(UserProfilesView.COL_PROFILE)), rs
                        .getDouble(UserProfilesView.COL_AMOUNT));
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return res;
    }

    /**
     * Updates the active profile for a users chips to the new profile.
     * 
     * Performs an SQL statement on the DB
     * 
     * @param username The username
     * @param profile The profile type
     * 
     * @return The amount of credits
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean updateActiveProfile(final String username,
                                       final ProfileType profile) throws SQLException {
        String sql = "UPDATE " + UsersTable.NAME + " SET "
                + UsersTable.COL_ACTIVEPROFILE + " = " + "("
                + getProfileIDSQL(profile) + ")" + " WHERE "
                + UsersTable.COL_USERNAME + " LIKE '" + username + "'";

        return runBasicQuery(sql) == 1;
    }

    /**
     * Adds a number of chips at a certain table for a user so it can be
     * returned if the bot decides it needs to take a break and crashes.
     * 
     * @param username the user
     * @param tableid the table id
     * @param profile the profile type
     * @param amount the amount
     * 
     * @throws SQLException when a database error occurs
     */
    public void addPokerTableCount(final String username,
                                   final int tableid,
                                   final ProfileType profile,
                                   final int amount)
        throws SQLException {
        String updsql = "UPDATE " + PokerBetsTable.NAME + " SET "
                + PokerBetsTable.COL_AMOUNT + " = '" + Integer.toString(amount)
                + "'" + " WHERE " + PokerBetsTable.COL_USERID + " = ("
                + getUserIDSQL(username) + ") AND "
                + PokerBetsTable.COL_PROFILEID + " = ("
                + getProfileIDSQL(profile) + ") AND "
                + PokerBetsTable.COL_TABLEID + " = '"
                + Integer.toString(tableid) + "'";

        String inssql = "INSERT INTO " + PokerBetsTable.NAME + " ("
                + PokerBetsTable.COL_USERID + ","
                + PokerBetsTable.COL_PROFILEID + ","
                + PokerBetsTable.COL_TABLEID + "," + PokerBetsTable.COL_AMOUNT
                + ") " + "VALUES((" + getUserIDSQL(username) + "), ("
                + getProfileIDSQL(profile) + "), " + Integer.toString(tableid)
                + ", " + Integer.toString(amount) + ")";
        int numrows = runBasicQuery(updsql);
        if (numrows == 0) {
            runBasicQuery(inssql);
        }
    }

    /**
     * Run upon start up, refunds all active bets that were never called or
     * rolled for since a crash / restart.
     * 
     * @throws SQLException when a database error occurs
     */
    public void processRefunds() throws SQLException {
        processOtherRefunds();
        processPokerRefunds();
    }

    /**
     * Run upon start up, refunds all active bets that were never called or
     * rolled for since a crash / restart.
     * 
     * @throws SQLException when a database error occurs
     */
    public void processOtherRefunds() throws SQLException {
        String sqlsel = "SELECT ut." + UsersTable.COL_USERNAME + ", "
                + BetsTable.COL_AMOUNT + ", " + " pt."
                + ProfileTypeTable.COL_NAME + " FROM " + BetsTable.NAME + " bt"
                + " JOIN " + UsersTable.NAME + " ut ON ut." + UsersTable.COL_ID
                + " = bt." + BetsTable.COL_USERID + " JOIN "
                + ProfileTypeTable.NAME + " pt ON pt."
                + ProfileTypeTable.COL_ID + " = bt." + BetsTable.COL_PROFILE;
        String sqldel = "DELETE FROM bets WHERE 1";

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            try {
                rs = stmt.executeQuery(sqlsel);
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sqlsel);
            }
            while (rs.next()) {
                adjustChips(
                        rs.getString("ut." + UsersTable.COL_USERNAME),
                        rs.getInt(BetsTable.COL_AMOUNT),
                        ProfileType.fromString(rs
                                .getString(ProfileTypeTable.COL_NAME)),
                        GamesType.ADMIN, TransactionType.ADMIN);
            }
            try {
                stmt.executeUpdate(sqldel);
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sqldel);
            }
        } catch (SQLException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    /**
     * Run upon start up, refunds all poker bets that were never called or
     * rolled for since a crash / restart.
     * 
     * @throws SQLException when a database error occurs
     */
    public void processPokerRefunds() throws SQLException {
        String sql = "SELECT " + PokerBetsTable.COL_USERID + ","
                + PokerBetsTable.COL_PROFILEID + ","
                + PokerBetsTable.COL_AMOUNT + " FROM " + PokerBetsTable.NAME;

        String updsql = "UPDATE " + UserProfilesTable.NAME + " SET "
                + UserProfilesTable.COL_AMOUNT + " = ("
                + UserProfilesTable.COL_AMOUNT + " + %amount)" + " WHERE "
                + UserProfilesTable.COL_USERID + " = '%user_id' AND "
                + UserProfilesTable.COL_TYPEID + " = '%type_id'";

        String delsql = "DELETE FROM " + PokerBetsTable.NAME;

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

            while (rs.next()) {
                String updsqlinst = updsql.replaceAll(
                        "%amount",
                        Integer.toString(rs.getInt(PokerBetsTable.COL_AMOUNT)));
                updsqlinst = updsqlinst.replaceAll(
                        "%user_id",
                        Integer.toString(rs.getInt(PokerBetsTable.COL_USERID)));
                updsqlinst = updsqlinst.replaceAll("%type_id", Integer
                        .toString(rs.getInt(PokerBetsTable.COL_PROFILEID)));
                try {
                    updstmt.executeUpdate(updsqlinst);
                } catch (SQLException e) {
                    throw new DBException(e, updsqlinst);
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
                if (rs != null) {
                    rs.close();
                }
                if (updstmt != null) {
                    stmt.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException exc) {
                throw exc;
            }
        }
    }

    /**
     * Transfer's chips from one user to another.
     * 
     * Presumes both users exist and the sender has enough chips.
     * 
     * @param sender The user who is sending chips
     * @param user The user to received the chips
     * @param amount The amount
     * @param profile The profile the chips are from.
     * 
     * @throws SQLException when a database error occurs
     */
    public void transferChips(final String sender, final String user,
                              final int amount, final ProfileType profile) throws SQLException {
        String chipssql = "SELECT COUNT(*) FROM " + UserProfilesView.NAME
                + " WHERE " + UserProfilesView.COL_PROFILE + " LIKE " + "'"
                + profile.toString() + "'" + " AND "
                + UserProfilesView.COL_USERNAME + " LIKE " + "'" + user + "'";

        String updminussql = "UPDATE " + UserProfilesTable.NAME + " SET "
                + UserProfilesTable.COL_AMOUNT + "= ("
                + UserProfilesTable.COL_AMOUNT + " - " + amount + ")"
                + " WHERE " + UserProfilesTable.COL_TYPEID + " LIKE ("
                + getProfileIDSQL(profile) + ") AND "
                + UserProfilesTable.COL_USERID + " LIKE ("
                + getUserIDSQL(sender) + ")";

        // TODO: use insert or update as in adjustChips (or use adjust chips)
        String inssql = "INSERT INTO " + UserProfilesTable.NAME + "("
                + UserProfilesTable.COL_USERID + ", "
                + UserProfilesTable.COL_TYPEID + ", "
                + UserProfilesTable.COL_AMOUNT + ") VALUES(("
                + getUserIDSQL(user) + "), (" + getProfileIDSQL(profile)
                + "), " + "'" + Integer.toString(amount) + "') ";

        String updaddsql = "UPDATE " + UserProfilesTable.NAME + " SET "
                + UserProfilesTable.COL_AMOUNT + "= ("
                + UserProfilesTable.COL_AMOUNT + " + " + amount + ")"
                + " WHERE " + UserProfilesTable.COL_TYPEID + " LIKE ("
                + getProfileIDSQL(profile) + ") AND "
                + UserProfilesTable.COL_USERID + " LIKE (" + getUserIDSQL(user)
                + ")";

        if (runGetIntQuery(chipssql) < 1) {
            runBasicQuery(inssql);
        } else {
            runBasicQuery(updaddsql);
        }
        runBasicQuery(updminussql);

        addTransaction(sender, -amount, GamesType.ADMIN, TransactionType.TRANSFER, profile);
        addTransaction(user, amount, GamesType.ADMIN, TransactionType.TRANSFER, profile);
        addChipTransaction(sender, user, -amount, TransactionType.TRANSFER, profile);
        addChipTransaction(user, sender, amount, TransactionType.TRANSFER, profile); 
    }

    /**
     * Adjusts a players chips.
     * 
     * @param username The player's username
     * @param amount The cash out value
     * @param profile The profile type
     * @param game The game type
     * @param tzxtype The transaction type
     * 
     * @return true if it succeeded
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean adjustChips(final String username,
                               final int amount,
                               final ProfileType profile,
                               final GamesType game,
                               final TransactionType tzxtype)
        throws SQLException {
        return adjustChips(username, (double) amount, profile, game, tzxtype);
    }

    /**
     * Adjusts a players chips.
     * 
     * @param username The player's username
     * @param amount The cash out value
     * @param profile The profile type
     * @param game The game type
     * @param tzxtype The transaction type
     * 
     * @return true if it succeeded
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean adjustChips(final String username,
                               final double amount,
                               final ProfileType profile,
                               final GamesType game,
                               final TransactionType tzxtype)
        throws SQLException {
        String inssql = "INSERT INTO " + UserProfilesTable.NAME + "("
                      + UserProfilesTable.COL_USERID + ", "
                      + UserProfilesTable.COL_TYPEID + ", "
                      + UserProfilesTable.COL_AMOUNT + ") " 
                      + "VALUES((" + getUserIDSQL(username) + "), ("
                      + getProfileIDSQL(profile)
                      + "), '" + Double.toString(amount) + "') "
                      + "ON DUPLICATE KEY UPDATE " + UserProfilesTable.COL_AMOUNT
                      + "= (" + UserProfilesTable.COL_AMOUNT
                      + " + " + Double.toString(amount) + ")";

        String insusersql = "INSERT INTO " + UsersTable.NAME + "("
                          + UsersTable.COL_USERNAME + ") VALUES('" + username + "')";
        
        boolean result = false;

        int profileid = runGetIntQuery(getProfileIDSQL(profile));
        // check valid profile
        if (profileid != -1) {
            int numrows = 1;
            // check user exists
            if (runGetIntQuery(getUserIDSQL(username)) < 1) {
                numrows = runBasicQuery(insusersql);
            }
            
            if (numrows == 1) {
                //TODO: find out why this adjusts 2 rows
                result = (runBasicQuery(inssql) >= 1);
            }
        }

        if (result) {
            addTransaction(username, amount, game, tzxtype, profile);
        }

        return result;
    }

    /**
     * Adds a players poker chips back to their balance We don't care if the
     * user doesn't exist as they needed to exist to join the table.
     * 
     * @param username The player's username
     * @param amount The cash out value
     * @param proftype The profile type
     * 
     * @throws SQLException when a database error occurs
     */
    public void cashOut(final String username,
                        final int amount,
                        final ProfileType proftype)
        throws SQLException {
        String sql = "UPDATE " + UserProfilesTable.NAME + " SET "
                + UserProfilesTable.COL_AMOUNT + " = ("
                + UserProfilesTable.COL_AMOUNT + " + "
                + Integer.toString(amount) + ")" + " WHERE "
                + UserProfilesTable.COL_USERID + " = ("
                + getUserIDSQL(username) + ") AND "
                + UserProfilesTable.COL_TYPEID + " = ("
                + getProfileIDSQL(proftype) + ")";

        runBasicQuery(sql);
        addTransaction(
                username, amount, GamesType.POKER,
                TransactionType.POKER_CASHOUT, proftype);
    }

    /**
     * Updates the global jackpot total.
     * 
     * @param proftype The profile the jackpot is for
     * @param amount The amount to increase it by
     * 
     * @throws SQLException when a database error occurs
     */
    public void updateJackpot(final ProfileType proftype,
                              final double amount)
        throws SQLException {
        String sql = "INSERT INTO " + JackpotTable.NAME + "("
                + JackpotTable.COL_PROFILE + "," + JackpotTable.COL_TOTAL + ")"
                + " VALUES ((" + getProfileIDSQL(proftype) + "), '" + amount
                + "')" + " ON DUPLICATE KEY UPDATE " + JackpotTable.COL_TOTAL
                + " = " + JackpotTable.COL_TOTAL + " + " + amount;
        runBasicQuery(sql);
    }

    /**
     * Retrieves the jackpot from the database.
     * 
     * @param proftype The profile the jackpot is for
     * 
     * @return the jackpot amount
     * 
     * @throws SQLException when a database error occurs
     */
    public double getJackpot(final ProfileType proftype)
        throws SQLException {
        String sql = "SELECT " + JackpotTable.COL_TOTAL + " FROM "
                + JackpotTable.NAME + " WHERE " + JackpotTable.COL_PROFILE
                + " = (" + getProfileIDSQL(proftype) + ")";
        double res = runGetDblQuery(sql);
        if (res < 0.0) {
            res = 0.0;
        }
        return res;
    }

    /**
     * Adds the winnings from a jackpot.
     * 
     * @param username The player's user name
     * @param amount The cash out value
     * @param proftype The profile the jackpot is for
     * 
     * @throws SQLException when a database error occurs
     */
    public void jackpot(final String username,
                        final int amount,
                        final ProfileType proftype)
        throws SQLException {
        String sql = "UPDATE " + UserProfilesTable.NAME + " SET "
                + UserProfilesTable.COL_AMOUNT + " = ("
                + UserProfilesTable.COL_AMOUNT + " + "
                + Integer.toString(amount) + ")" + " WHERE "
                + UserProfilesTable.COL_USERID + " = ("
                + getUserIDSQL(username) + ") AND "
                + UserProfilesTable.COL_TYPEID + " = ("
                + getProfileIDSQL(proftype) + ")";

        String reset = "INSERT INTO " + JackpotTable.NAME + "("
                + JackpotTable.COL_PROFILE + "," + JackpotTable.COL_TOTAL + ")"
                + " VALUES ((" + getProfileIDSQL(proftype) + "), '0')"
                + " ON DUPLICATE KEY UPDATE " + JackpotTable.COL_TOTAL
                + " = '0'";

        runBasicQuery(sql);
        runBasicQuery(reset);
        addTransaction(
                username, amount, GamesType.ADMIN, TransactionType.JACKPOT,
                proftype);
    }

    /**
     * Adds poker chips to a table for a player. We don't care if the user
     * doesn't exist as they will have had their chips checked prior to this.
     * 
     * @param username The player's user name
     * @param amount The cash out value
     * @param proftype The profile the buy in is for
     * 
     * @throws SQLException when a database error occurs
     */
    public void buyIn(final String username,
                      final int amount,
                      final ProfileType proftype)
        throws SQLException {
        String sql = "UPDATE " + UserProfilesTable.NAME + " SET "
                + UserProfilesTable.COL_AMOUNT + " = ("
                + UserProfilesTable.COL_AMOUNT + " - "
                + Integer.toString(amount) + ")" + " WHERE "
                + UserProfilesTable.COL_USERID + " = ("
                + getUserIDSQL(username) + ") AND "
                + UserProfilesTable.COL_TYPEID + " = ("
                + getProfileIDSQL(proftype) + ")";

        runBasicQuery(sql);
        addTransaction(
                username, -amount, GamesType.POKER,
                TransactionType.POKER_BUYIN, proftype);
    }

    /**
     * Getter method for the next hand ID.
     * 
     * Performs an SQL statement on the DB where the new hand is created with a
     * blank winner.
     * 
     * @return The handID
     * 
     * @throws SQLException when a database error occurs
     */
    public int getHandID()
        throws SQLException {
        String sql = "INSERT INTO " + PokerHandsTable.NAME + " ( "
                + PokerHandsTable.COL_WINNNERID + ", "
                + PokerHandsTable.COL_AMOUNT + ") " + "VALUES ('0', '0')";

        return runGetIDQuery(sql);
    }

    /**
     * Updates the hand table with the hand winner and pot size.
     * 
     * @param handid The ID
     * @param username The winner
     * @param pot The pot size
     * 
     * @throws SQLException when a database error occurs
     */
    public void setHandWinner(final int handid,
                              final String username,
                              final int pot)
        throws SQLException {
        String sql = "UPDATE " + PokerHandsTable.NAME + " SET "
                + PokerHandsTable.COL_WINNNERID + " = ("
                + getUserIDSQL(username) + "), " + PokerHandsTable.COL_AMOUNT
                + " = '" + Integer.toString(pot) + "' " + "WHERE "
                + PokerHandsTable.COL_ID + " = '" + Integer.toString(handid)
                + "'";

        runBasicQuery(sql);
    }

    /**
     * Updates the hand table with the hand winner and pot size.
     * 
     * @param handid The ID
     * @param username The winner
     * @param pot The pot size
     * 
     * @throws SQLException when a database error occurs
     */
    public void addHandWinner(final int handid,
                              final String username,
                              final int pot)
        throws SQLException {
        String sql = "INSERT INTO " + PokerHandsTable.NAME + "("
                + PokerHandsTable.COL_WINNNERID + ", "
                + PokerHandsTable.COL_AMOUNT + ", " + PokerHandsTable.COL_ID
                + ") VALUES( " + "(" + getUserIDSQL(username) + "),'"
                + Integer.toString(pot) + "','" + Integer.toString(handid)
                + "')";

        runBasicQuery(sql);
    }

    /**
     * Gets a user's current position in the weekly competition.
     * 
     * @param profile The profile to check
     * @param user The user name
     * 
     * @return The position
     * 
     * @throws DBException
     * @throws SQLException
     * 
     * @throws SQLException when a database error occurs
     */
    public BetterInfo competitionPosition(final ProfileType profile,
                                          final String user)
        throws SQLException {
        String sql = "SELECT t.position FROM "
                + "(SELECT c.*,(@position:=@position+1) AS position" + " FROM "
                + CompetitionView.NAME + " c, (SELECT @position:=0) p WHERE "
                + CompetitionView.COL_PROFILE + " LIKE '" + profile.toString()
                + "') t" + " WHERE " + CompetitionView.COL_USERNAME + " LIKE '"
                + user + "'";
        String csql = "SELECT " + CompetitionView.COL_TOTAL + " FROM "
                + CompetitionView.NAME + " WHERE "
                + CompetitionView.COL_PROFILE + " LIKE '" + profile.toString()
                + "' AND " + CompetitionView.COL_USERNAME + " LIKE '" + user
                + "'";

        return new BetterInfo(user, runGetIntQuery(sql), runGetLongQuery(csql));

    }

    /**
     * Updates the competition id and time.
     * 
     * @throws SQLException when a database error occurs
     */
    public void competitionEnd()
        throws SQLException {
        String sql = "UPDATE " + CompetitionIDTable.NAME + " SET "
                + CompetitionIDTable.COL_ID + " = ("
                + CompetitionIDTable.COL_ID + " + 1), "
                + CompetitionIDTable.COL_ENDS + " = ADDDATE("
                + CompetitionIDTable.COL_ENDS + ", INTERVAL 7 DAY)";

        runBasicQuery(sql);
    }

    /**
     * Checks if the competition for the current week is ended.
     * 
     * @return true if the competition is over
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean competitionOver()
        throws SQLException {
        String sql = "SELECT IF(TIMESTAMPDIFF(MINUTE, CURRENT_TIMESTAMP(), "
                + CompetitionIDTable.COL_ENDS + ") > 0, '1', '0')" + "FROM "
                + CompetitionIDTable.NAME + " LIMIT 1";
        return (runGetIntQuery(sql) == 1);
    }

    /**
     * Checks if the competition for the current week is ended.
     * 
     * @return the number of seconds until the competition ends
     * 
     * @throws SQLException when a database error occurs
     */
    public int getCompetitionTimeLeft()
        throws SQLException {
        String sql = "SELECT (UNIX_TIMESTAMP(" + CompetitionIDTable.COL_ENDS
                + ") - UNIX_TIMESTAMP(NOW()))" + " FROM "
                + CompetitionIDTable.NAME + " LIMIT 1";
        int res = runGetIntQuery(sql);

        if (res < 0) {
            res = 0;
        }
        return res;
    }

    /**
     * Gets the highest better for a profile.
     * 
     * @param profile The profile to check
     * @param number The number of users to retrieve
     * 
     * @return A list of users of the highest better for the provided profile
     * 
     * @throws SQLException when a database error occurs
     */
    public List<BetterInfo> getCompetition(final ProfileType profile,
                                           final int number)
        throws SQLException {
        String sql = "SELECT " + CompetitionView.COL_USERNAME + ","
                + CompetitionView.COL_TOTAL + " FROM " + CompetitionView.NAME
                + " WHERE " + CompetitionView.COL_PROFILE + " LIKE '"
                + profile.toString() + "'" + " ORDER BY "
                + CompetitionView.COL_TOTAL + " DESC " + " LIMIT 0, "
                + Integer.toString(number);

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

                while (rs.next()) {
                    user = rs.getString(TotalBetsView.COL_USERNAME);
                    total = rs.getLong(TotalBetsView.COL_TOTAL);
                    winners.add(new BetterInfo(user, total));
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }

        return winners;
    }

    /**
     * Gets the highest better for a profile.
     * 
     * @param profile The profile to check
     * 
     * @return The username of the highest better for the provided profile
     * 
     * @throws SQLException when a database error occurs
     */
    public BetterInfo getTopBetter(final ProfileType profile)
        throws SQLException {
        return getTopBetter(profile, null);
    }

    /**
     * Gets the specific better from the highest betters for a profile.
     * 
     * @param profile The profile to check
     * @param who The user to get
     * 
     * @return The username of the highest better for the provided profile
     * 
     * @throws SQLException when a database error occurs
     */
    public BetterInfo getTopBetter(final ProfileType profile,
                                   final String who)
        throws SQLException {
        String sql = "SELECT " + TotalBetsView.COL_USERNAME + ","
                + TotalBetsView.COL_TOTAL + " FROM " + TotalBetsView.NAME
                + " WHERE " + TotalBetsView.COL_PROFILE + " LIKE '"
                + profile.toString() + "'";

        if (who != null) {
            sql += " AND " + TotalBetsView.COL_USERNAME + " LIKE '" + who + "'";
        }

        sql += " ORDER BY " + TotalBetsView.COL_TOTAL + " DESC LIMIT 1";

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

                if (rs.next()) {
                    user = rs.getString(TotalBetsView.COL_USERNAME);
                    total = rs.getLong(TotalBetsView.COL_TOTAL);
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }

        return new BetterInfo(user, total);
    }

    /**
     * Gets the highest single bet for a profile.
     * 
     * @param profile The profile to check
     * 
     * @return The user name of the highest better for the provided profile
     * 
     * @throws SQLException when a database error occurs
     */
    public BetterInfo getHighestBet(final ProfileType profile)
        throws SQLException {
        return getHighestBet(profile, null);
    }

    /**
     * Gets the highest single bet for a profile for a person.
     * 
     * @param profile The profile to check
     * @param who The user to get
     * 
     * @return The user name of the highest better for the provided profile
     * 
     * @throws SQLException when a database error occurs
     */
    public BetterInfo getHighestBet(final ProfileType profile,
                                    final String who)
        throws SQLException {
        String sql = "SELECT " + OrderedBetsView.COL_USERNAME + ","
                + OrderedBetsView.COL_GAME + "," + OrderedBetsView.COL_TOTAL
                + " FROM " + OrderedBetsView.NAME + " WHERE "
                + OrderedBetsView.COL_PROFILE + " LIKE '" + profile.toString()
                + "'";

        if (who != null) {
            sql += " AND " + OrderedBetsView.COL_USERNAME + " LIKE '" + who
                    + "'";
        }

        sql += " ORDER BY " + OrderedBetsView.COL_TOTAL + " DESC LIMIT 1";

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

                if (rs.next()) {
                    user = rs.getString(OrderedBetsView.COL_USERNAME);
                    game = rs.getString(OrderedBetsView.COL_GAME);
                    total = rs.getLong(OrderedBetsView.COL_TOTAL);
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }

        return new BetterInfo(user, game, total);
    }

    /**
     * Buys a number of lottery ticket.
     * 
     * @param username The user buying the tickets.
     * @param profile The profile being used
     * @param amount The number of tickets to buy
     * 
     * @return true if the command was successful
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean buyLotteryTickets(final String username,
                                     final ProfileType profile,
                                     final int amount)
        throws SQLException {
        adjustChips(
                username, (0 - amount), profile, GamesType.LOTTERY,
                TransactionType.LOTTERY);

        String sql = "INSERT INTO " + LotteryTicketsTable.NAME + " ("
                + LotteryTicketsTable.COL_ID + ", "
                + LotteryTicketsTable.COL_USERID + ", "
                + LotteryTicketsTable.COL_PROFILEID + ") VALUES ";

        int userid = runGetIntQuery(getUserIDSQL(username));
        int profileid = runGetIntQuery(getProfileIDSQL(profile));

        boolean ret = true;

        if (userid != -1 && profileid != -1) {
            String values = "(NULL, '" + userid + "', '" + profileid + "')";

            for (int i = 1; i <= amount; i++) {
                sql += values;
                if (i != amount) {
                    sql += ", ";
                }
            }

            ret = (runBasicQuery(sql) > 0);
        }

        return ret;
    }

    /**
     * Returns the size of the lottery.
     * 
     * @param profile The profile being used
     * 
     * @throws SQLException when a database error occurs
     * 
     * @return the number of lottery tickets
     */
    public int getLotteryTickets(final ProfileType profile)
        throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + LotteryTicketsTable.NAME
                + " WHERE " + LotteryTicketsTable.COL_PROFILEID + " = " + "("
                + getProfileIDSQL(profile) + ")";

        return runGetIntQuery(sql);
    }

    /**
     * Returns the lottery winner for a profile.
     * 
     * @param profile The profile being used
     * 
     * @return The random winner.
     * 
     * @throws SQLException when a database error occurs
     */
    public String getLotteryWinner(final ProfileType profile)
        throws SQLException {
        String sql = "SELECT u." + UsersTable.COL_USERNAME + " FROM "
                + LotteryTicketsTable.NAME + " l " + "JOIN " + UsersTable.NAME
                + " u ON l." + LotteryTicketsTable.COL_USERID + " = u."
                + UsersTable.COL_ID + " WHERE "
                + LotteryTicketsTable.COL_PROFILEID + " = " + "("
                + getProfileIDSQL(profile) + ")" + " ORDER BY RAND() LIMIT 1";

        return runGetStringQuery(sql);
    }

    /**
     * Starts a new lottery by wiping the current tickets.
     * 
     * @throws SQLException when a database error occurs
     */
    public void endLottery() throws SQLException {
        String sql = "DELETE FROM " + LotteryTicketsTable.NAME;

        runBasicQuery(sql);
    }

    /**
     * Checks if a user is a rank.
     * 
     * @param user The user name.
     * 
     * @return true if they are a rank.
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean isRank(final String user) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + HostGroupUsersTable.NAME
                + " WHERE " + HostGroupUsersTable.COL_USERID + " = ("
                + getUserIDSQL(user) + ")";

        return (runGetIntQuery(sql) > 0);
    }

    /**
     * Adds a rank to a rank group.
     * 
     * @param user The user name
     * @param group The rank group
     * 
     * @throws SQLException when a database error occurs
     */
    public void addRank(final String user, final String group) throws SQLException {
        String sql = "INSERT INTO " + HostGroupUsersTable.NAME + "("
                + HostGroupUsersTable.COL_USERID + ", "
                + HostGroupUsersTable.COL_GROUPID + ")" + " VALUES(("
                + getUserIDSQL(user) + "), (" + getRankGroupIDSQL(group) + "))";
        runBasicQuery(sql);
    }

    /**
     * Retrieves the rank group associated with a user.
     * 
     * @param user The user name.
     * 
     * @return The rank group.
     * 
     * @throws SQLException when a database error occurs
     */
    public String getRankGroup(final String user) throws SQLException {
        String sql = "SELECT hg." + HostGroupsTable.COL_NAME + " FROM "
                + HostGroupUsersTable.NAME + " hgu" + " JOIN "
                + HostGroupsTable.NAME + " hg ON " + "hg."
                + HostGroupsTable.COL_ID + " = " + "hgu."
                + HostGroupUsersTable.COL_GROUPID + " WHERE hgu."
                + HostGroupUsersTable.COL_USERID + " = (" + getUserIDSQL(user)
                + ")";
        return runGetStringQuery(sql);
    }

    /**
     * Moves a rank to a new group.
     * 
     * @param user The user name.
     * @param group The rank group.
     * 
     * @throws SQLException when a database error occurs
     */
    public void updateRank(final String user, final String group) throws SQLException {
        String sql = "UPDATE " + HostGroupUsersTable.NAME + " SET "
                + HostGroupUsersTable.COL_GROUPID + " = ("
                + getRankGroupIDSQL(group) + ")" + " WHERE "
                + HostGroupUsersTable.COL_USERID + " = (" + getUserIDSQL(user)
                + ")";
        runBasicQuery(sql);
    }

    /**
     * Removes a rank from a rank group.
     * 
     * @param user The user name.
     * 
     * @throws SQLException when a database error occurs
     */
    public void kickRank(final String user)
        throws SQLException {
        String sql = "DELETE FROM " + HostGroupUsersTable.NAME + " WHERE "
                + HostGroupUsersTable.COL_USERID + " = (" + getUserIDSQL(user)
                + ")";
        runBasicQuery(sql);
    }

    /**
     * Verifies the supplied string is a rank group.
     * 
     * @param group The rank group
     * 
     * @return true if the group exists
     * 
     * @throws SQLException when a database error occurs
     */
    public boolean isRankGroup(final String group)
        throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + HostGroupsTable.NAME + " WHERE "
                + HostGroupsTable.COL_NAME + " LIKE '" + group + "'";

        return (runGetIntQuery(sql) > 0);
    }

    /**
     * Deletes a rank group.
     * 
     * @param group The rank group
     * 
     * @throws SQLException when a database error occurs
     */
    public void deleteRankGroup(final String group)
        throws SQLException {
        String sql = "DELETE FROM " + HostGroupsTable.NAME + " WHERE "
                + HostGroupsTable.COL_NAME + " LIKE '" + group + "'";
        runBasicQuery(sql);
    }

    /**
     * Creates a new rank group.
     * 
     * @param owner The owner name
     * @param group The group name
     * 
     * @throws SQLException when a database error occurs
     */
    public void newRankGroup(final String owner, final String group)
        throws SQLException {
        String sql = "INSERT INTO " + HostGroupsTable.NAME + "("
                    + HostGroupsTable.COL_NAME + "," + HostGroupsTable.COL_OWNER + ")"
                    + " VALUES('" + group + "', (" + getUserIDSQL(owner) + "))";

        checkUserExists(group, group + "!" + group + "@" + group);
        runBasicQuery(sql);
    }

    /**
     * Renames a rank group.
     * 
     * @param oldgroup the old name
     * @param newgroup the new name
     * 
     * @throws SQLException when a database error occurs
     */
    public void renameRankGroup(final String oldgroup,
                                final String newgroup)
        throws SQLException {
        String sql = "UPDATE " + HostGroupsTable.NAME + " SET "
                + HostGroupsTable.COL_NAME + " = '" + newgroup + "'"
                + " WHERE " + HostGroupsTable.COL_NAME + " LIKE '" + oldgroup
                + "'";
        runBasicQuery(sql);

        sql = "UPDATE " + UsersTable.NAME + " SET " + UsersTable.COL_USERNAME
                + " = '" + newgroup + "'" + " WHERE " + UsersTable.COL_USERNAME
                + " LIKE '" + oldgroup + "'";
        runBasicQuery(sql);
    }

    /**
     * Generates a list of users in a rank group.
     * 
     * @param group the rank group
     * 
     * @return the list of users
     * 
     * @throws SQLException when a database error occurs
     */
    public List<String> listRankGroupUsers(final String group)
        throws SQLException {
        List<String> res = new ArrayList<String>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT u." + UsersTable.COL_USERNAME + " FROM "
                + HostGroupUsersTable.NAME + " hgu" + " JOIN "
                + UsersTable.NAME + " u ON u." + UsersTable.COL_ID + " = "
                + "hgu." + HostGroupUsersTable.COL_USERID + " WHERE "
                + HostGroupUsersTable.COL_GROUPID + " = ("
                + getRankGroupIDSQL(group) + ")";

        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                res.add(rs.getString("u." + UsersTable.COL_USERNAME));
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return res;
    }

    /**
     * Generates a list of rank groups.
     * 
     * @return the list of group
     * 
     * @throws SQLException when a database error occurs
     */
    public List<String> listRankGroups()
        throws SQLException {
        List<String> res = new ArrayList<String>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT " + HostGroupsTable.COL_NAME + " FROM "
                + HostGroupsTable.NAME + " WHERE 1";

        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                res.add(rs.getString(HostGroupsTable.COL_NAME));
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return res;
    }

    /**
     * Adds a referrer to a user.
     * 
     * @param user The user
     * @param referrer The referrer
     * 
     * @throws SQLException when a database error occurs
     */
    public void addReferer(final String user,
                           final String referrer)
        throws SQLException {
        String sql = "INSERT INTO " + ReferersTable.NAME + "("
                + ReferersTable.COL_USERID + ", "
                + ReferersTable.COL_REFERRERID + ")" + " VALUES(("
                + getUserIDSQL(user) + "), (" + getUserIDSQL(referrer) + "))";

        runBasicQuery(sql);
    }

    /**
     * Deletes a referrer from a user.
     * 
     * @param user The user
     * @param referrer The referrer
     * 
     * @throws SQLException when a database error occurs
     */
    public void delReferer(final String user,
                           final String referrer)
        throws SQLException {
        String sql = "DELETE FROM " + ReferersTable.NAME + " WHERE "
                + ReferersTable.COL_USERID + " = (" + getUserIDSQL(user) + ")"
                + " AND " + ReferersTable.COL_REFERRERID + " = ("
                + getUserIDSQL(referrer) + ")";

        runBasicQuery(sql);
    }


    /**
     * Gives a rank a number of points.
     * 
     * @param who   The rank to give points to
     * @param points The number of points to give, can be negative.
     * 
     * @throws SQLException when a database error occurs
     */
    public void givePoints(final String who,
                           final int points) throws SQLException {
        String sql = "UPDATE " + HostGroupUsersTable.NAME 
                + " SET " + HostGroupUsersTable.COL_POINTS
                + " = (" + HostGroupUsersTable.COL_POINTS
                + " + " + Integer.toString(points) + ")"
                + " WHERE " + HostGroupUsersTable.COL_USERID
                + " = (" + getUserIDSQL(who) + ")";
        
        runBasicQuery(sql);
    }

    /**
     * Checks the number of points a rank has.
     * 
     * @param who The rank to check
     * 
     * @return the number of points
     * 
     * @throws SQLException when a database error occurs
     */
    public int checkPoints(final String who) throws SQLException {
        String sql = "SELECT " + HostGroupUsersTable.COL_POINTS 
                   + " FROM " + HostGroupUsersTable.NAME
                   + " WHERE " + HostGroupUsersTable.COL_USERID
                   + " = (" + getUserIDSQL(who) + ")";
        return runGetIntQuery(sql);
    }
    
    /**
     * @return The total number of points this week.
     * 
     * @throws SQLException when a database error occurs
     */
    public int getPointTotal() throws SQLException {
        String sql = "SELECT SUM(" + HostGroupUsersTable.COL_POINTS 
                + ") FROM " + HostGroupUsersTable.NAME
                + " WHERE 1";
        return runGetIntQuery(sql);
    }
    

    /**
     * @return a map of all users and the points they earned.
     * 
     * @throws SQLException when a database error occurs
     */
    public Map<String, Integer> getPoints() throws SQLException {
        Map<String, Integer> res = new HashMap<String, Integer>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT u." + UsersTable.COL_USERNAME
                        + ", hgu." + HostGroupUsersTable.COL_POINTS
                   + " FROM " + HostGroupUsersTable.NAME + " hgu" + " JOIN "
                   + UsersTable.NAME + " u ON u." + UsersTable.COL_ID + " = "
                   + "hgu." + HostGroupUsersTable.COL_USERID + " WHERE 1";
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String user = rs.getString("u." + UsersTable.COL_USERNAME);
                int points = rs.getInt("hgu." + HostGroupUsersTable.COL_POINTS);
                res.put(user, points);
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return res;
    }

    /**
     * Resets the points and the points earnings.
     * 
     * @throws SQLException when a database error occurs
     */
    public void resetPoints() throws SQLException {
        String sql = "UPDATE " + HostGroupUsersTable.NAME
                   + " SET " + HostGroupUsersTable.COL_POINTS + " = '0'"
                   + " WHERE 1";
        runBasicQuery(sql);
        
        sql = "UPDATE " + UserProfilesTable.NAME
            + " SET " + UserProfilesTable.COL_AMOUNT + " = '0'"
            + " WHERE " + UserProfilesTable.COL_USERID + " = ("
            + getUserIDSQL(POINTS_USER) + ")";
        runBasicQuery(sql);
    }
    
    /**
     * Gets a user's referral type.
     * 
     * @param username The user to check
     * 
     * @return The type
     * 
     * @throws SQLException when a database error occurs
     */
    public ReferrerType getRefererType(final String username)
        throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + FullReferersTextView.NAME
                + " WHERE " + FullReferersTextView.COL_USERNAME + " LIKE '"
                + username + "'";

        String grpsql = "SELECT COUNT(*) FROM " + FullReferersTextView.NAME
                + " WHERE " + FullReferersTextView.COL_USERNAME + " LIKE '"
                + username + "'" + " AND " + FullReferersTextView.COL_GROUP
                + " IS NOT NULL";

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

    /**
     * Lists the referrers for a specific user.
     * 
     * @param user The user to check
     * 
     * @return The list of referrers
     * 
     * @throws SQLException when a database error occurs
     */
    public List<ReferalUser> getReferalUsers(final String user)
        throws SQLException {
        String sql = "SELECT " + "t." + FullReferersTextView.COL_REFERRER + ","
                + "t." + FullReferersTextView.COL_GROUP + " FROM "
                + FullReferersTextView.NAME + " t" + " WHERE "
                + FullReferersTextView.COL_USERNAME + " LIKE '" + user + "'";

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

                while (rs.next()) {
                    ref = rs.getString(1);
                    group = rs.getString(2);
                    referers.add(new ReferalUser(ref, group));
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return referers;
    }

    /**
     * Gives a user an amount of referral fees.
     * 
     * @param fee The amount earnt
     * @param user The user receiving the fees
     * @param profile The profile type
     * 
     * @throws SQLException when a database error occurs
     */
    public void giveReferalFee(final double fee,
                               final String user,
                               final ProfileType profile)
        throws SQLException {
        adjustChips(user, fee, profile, GamesType.ADMIN, TransactionType.REFERRAL);
    }
    
    /**
     * Gives a user an amount of referral fees.
     * 
     * Does not check if the user exists, as they already should since we got this 
     * info from the database in the first place.
     * 
     * @param fees The amount earnt and by who
     * @param profile The profile type
     * 
     * @throws SQLException when a database error occurs
     */
    public void giveReferalFees(final Map<String, Double> fees,
                                final ProfileType profile) throws SQLException {
        Connection conn = null;
        PreparedStatement tstmt = null;
        PreparedStatement insstmt = null;
        
        String usersql = "SELECT uu." + UsersTable.COL_ID + " FROM "
                       + UsersTable.NAME + " uu" + " WHERE uu."
                       + UsersTable.COL_USERNAME + " LIKE ? LIMIT 1";
        
        String tsql = "INSERT INTO " + TransactionsTable.NAME + "("
                    + TransactionsTable.COL_TYPEID + ", "
                    + TransactionsTable.COL_GAMEID + ", "
                    + TransactionsTable.COL_USERID + ", "
                    + TransactionsTable.COL_AMOUNT + ", "
                    + TransactionsTable.COL_PROFILETYPE + ") VALUES(("
                    + getTzxTypeIDSQL(TransactionType.REFERRAL) + "), ("
                    + getGameIDSQL(GamesType.ADMIN) + "), ("
                    + usersql + "), ?, "
                    + "(" + getProfileIDSQL(profile) + "))";
        
        String inssql = "INSERT INTO " + UserProfilesTable.NAME + "("
                      + UserProfilesTable.COL_USERID + ", "
                      + UserProfilesTable.COL_TYPEID + ", "
                      + UserProfilesTable.COL_AMOUNT + ") " 
                      + "VALUES((" + usersql + "), (" + getProfileIDSQL(profile)
                      + "), ?) "
                      + "ON DUPLICATE KEY UPDATE " + UserProfilesTable.COL_AMOUNT
                      + "= (" + UserProfilesTable.COL_AMOUNT + " + ?)";
        
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            tstmt = conn.prepareStatement(tsql);
            insstmt = conn.prepareStatement(inssql);
            
            int profileid = runGetIntQuery(getProfileIDSQL(profile));
            // check valid profile
            if (profileid != -1) {            
                for (Entry<String, Double> entry: fees.entrySet()) {
                    String user = entry.getKey();
                    Double amnt = entry.getValue();
                    
                    int i = 0;
                    insstmt.setString(++i, user);
                    insstmt.setDouble(++i, amnt);
                    insstmt.setDouble(++i, amnt);
                    insstmt.executeUpdate();
                    
                    i = 0;
                    tstmt.setString(++i, user);
                    tstmt.setDouble(++i, amnt);
                    tstmt.executeUpdate();
                    
                    conn.commit();
                }
            }
        } catch (SQLException ex) {
            throw ex;
        } finally {
            try {
                if (tstmt != null) {
                    tstmt.close();
                }
                if (insstmt != null) {
                    insstmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }

    }
    
    /**
     * Gets the owner of a group.
     * 
     * @param group The group who's owner we want to retrieve
     * 
     * @return the name of the group owner.
     * 
     * @throws SQLException when a database error occurs
     */
    public String getOwner(final String group) throws SQLException {
        String sql = "SELECT uu." + UsersTable.COL_USERNAME
                   + " FROM " + HostGroupsTable.NAME + " hgu"
                   + " JOIN " + UsersTable.NAME + " uu ON uu."
                              + UsersTable.COL_ID + " = hgu." + HostGroupsTable.COL_OWNER
                   + " WHERE " + HostGroupsTable.COL_NAME + " LIKE '" + group + "'";
        return runGetStringQuery(sql);
    }

    /**
     * Checks HOUSE and POINTS exist.
     * 
     * @throws SQLException when a database error occurs
     */
    public void checkDBUsersExist()
        throws SQLException {
        checkUserExists(HOUSE_USER, "HOUSE!HOUSE@HOUSE");
        checkUserExists(POINTS_USER, "POINTS!POINTS@POINTS");
    }
    
    /**
     * Adds a new transaction to the transaction table.
     * 
     * @param username The username
     * @param admin The admin's username
     * @param amount The amount
     * @param tzxtype The type of transaction
     * @param proftype The type of profile
     * 
     * @throws SQLException when a database error occurs
     */
    public void addChipTransaction(final String username,
                                   final String admin,
                                   final int amount,
                                   final TransactionType tzxtype,
                                   final ProfileType proftype)
        throws SQLException {
        addChipTransaction(username, admin, (double) amount, tzxtype, proftype);
    }
    
    /**
     * Adds a new transaction to the transaction table.
     * 
     * @param username The username
     * @param admin The admin username
     * @param amount The amount
     * @param tzxtype The type of transaction
     * @param proftype The type of profile
     * 
     * @throws SQLException when a database error occurs
     */
    public void addChipTransaction(final String username,
                                   final String admin,
                                   final double amount,
                                   final TransactionType tzxtype,
                                   final ProfileType proftype)
        throws SQLException {
        String sql = "INSERT INTO " + ChipsTransactionsTable.NAME + "("
                + ChipsTransactionsTable.COL_TYPEID + ", "
                + ChipsTransactionsTable.COL_ADMINID + ", "
                + ChipsTransactionsTable.COL_USERID + ", "
                + ChipsTransactionsTable.COL_AMOUNT + ", "
                + ChipsTransactionsTable.COL_PROFILETYPE + ") VALUES(" + "("
                + getTzxTypeIDSQL(tzxtype) + "), ("
                + getUserIDSQL(admin) + "), ("
                + getUserIDSQL(username) + "), '"
                + Double.toString(amount) + "', ("
                + getProfileIDSQL(proftype) + "))";

        runBasicQuery(sql);
    }
    
    /**
     * Adds a new transaction to the transaction table.
     * 
     * @param username The username
     * @param amount The amount
     * @param gametype The type of game
     * @param tzxtype The type of transaction
     * @param proftype The type of profile
     * 
     * @throws SQLException when a database error occurs
     */
    public void addTransaction(final String username,
                               final int amount,
                               final GamesType gametype,
                               final TransactionType tzxtype,
                               final ProfileType proftype)
        throws SQLException {
        addTransaction(username, (double) amount, gametype, tzxtype, proftype);
    }

    /**
     * Adds a new transaction to the transaction table.
     * 
     * @param username The username
     * @param amount The amount
     * @param gametype The type of game
     * @param tzxtype The type of transaction
     * @param proftype The type of profile
     * 
     * @throws SQLException when a database error occurs
     */
    public void addTransaction(final String username,
                               final double amount,
                               final GamesType gametype,
                               final TransactionType tzxtype,
                               final ProfileType proftype)
        throws SQLException {
        String sql = "INSERT INTO " + TransactionsTable.NAME + "("
                + TransactionsTable.COL_TYPEID + ", "
                + TransactionsTable.COL_GAMEID + ", "
                + TransactionsTable.COL_USERID + ", "
                + TransactionsTable.COL_AMOUNT + ", "
                + TransactionsTable.COL_PROFILETYPE + ") VALUES(" + "("
                + getTzxTypeIDSQL(tzxtype) + "), " + "("
                + getGameIDSQL(gametype) + "), " + "(" + getUserIDSQL(username)
                + "), " + "(" + Double.toString(amount) + "), " + "("
                + getProfileIDSQL(proftype) + "))";

        runBasicQuery(sql);
    }

    /**
     * Runs a single query that returns a single column and row.
     * 
     * @param query The query to execute
     * 
     * @return The resulting String
     * 
     * @throws SQLException when a database error occurs
     */
    private String runGetStringQuery(final String query)
        throws SQLException {
        String sql = query.replaceAll("_", "\\_");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String ret = null;
        try {
            try {
                conn = getConnection();
                stmt = conn.createStatement();
                stmt.setMaxRows(1);
                rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    ret = rs.getString(1);
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return ret;
    }

    /**
     * Runs a single query that returns a single column and row.
     * 
     * @param query The query to execute
     * 
     * @return The resulting integer
     * 
     * @throws SQLException when a database error occurs
     */
    private int runGetIntQuery(final String query)
        throws SQLException {
        String sql = query.replaceAll("_", "\\_");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        int ret = -1;
        try {
            try {
                conn = getConnection();
                stmt = conn.createStatement();
                stmt.setMaxRows(1);
                rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    ret = rs.getInt(1);
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return ret;
    }

    /**
     * Runs a single query that returns a single column and row.
     * 
     * @param query The query to execute
     * 
     * @return The resulting integer
     * 
     * @throws SQLException when a database error occurs
     */
    private double runGetDblQuery(final String query)
        throws SQLException {
        String sql = query.replaceAll("_", "\\_");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        double ret = -1;
        try {
            try {
                conn = getConnection();
                stmt = conn.createStatement();
                stmt.setMaxRows(1);
                rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    ret = rs.getDouble(1);
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return ret;
    }

    /**
     * Runs a single query that returns a single column and row.
     * 
     * @param query The query to execute
     * 
     * @return The resulting integer
     * 
     * @throws SQLException when a database error occurs
     */
    private long runGetLongQuery(final String query)
        throws SQLException {
        String sql = query.replaceAll("_", "\\_");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        long ret = -1;
        try {
            try {
                conn = getConnection();
                stmt = conn.createStatement();
                stmt.setMaxRows(1);
                rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    ret = rs.getLong(1);
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return ret;
    }

    /**
     * Runs a single query that returns an ID from the table.
     * 
     * @param query The query to execute
     * 
     * @return The resulting int ID
     * 
     * @throws SQLException when a database error occurs
     */
    private int runGetIDQuery(final String query)
        throws SQLException {
        String sql = query.replaceAll("_", "\\_");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        int ret = -1;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            try {
                stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    ret = rs.getInt(1);
                } else {
                    throw new RuntimeException(
                            "Can't find most recent hand ID we just created");
                }
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return ret;
    }

    /**
     * Runs a single query that returns nothing.
     * 
     * @param query The query to execute
     * 
     * @return the number of rows affected.
     * 
     * @throws SQLException when a database error occurs
     */
    private int runBasicQuery(final String query)
        throws SQLException {
        String sql = query.replaceAll("_", "\\_");
        Connection conn = null;
        Statement stmt = null;
        int numrows = -1;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            try {
                numrows = stmt.executeUpdate(sql);
            } catch (SQLException e) {
                throw new DBException(e.getMessage(), sql);
            }
        } catch (DBException ex) {
            throw ex;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        return numrows;
    }

    /**
     * Provides the SQL for retrieving a user's ID.
     * 
     * @param username The username
     * 
     * @return The ID
     */
    private static String getUserIDSQL(final String username) {
        String out = "SELECT uu." + UsersTable.COL_ID + " FROM "
                + UsersTable.NAME + " uu" + " WHERE uu."
                + UsersTable.COL_USERNAME + " LIKE '" + username + "' LIMIT 1";
        return out;
    }

    /**
     * Provides the SQL for retrieving the Poker game ID.
     * 
     * @param game the game type
     * 
     * @return the ID query
     */
    private static String getGameIDSQL(final GamesType game) {
        String out = "SELECT gg." + GamesTable.COL_ID + " FROM "
                + GamesTable.NAME + " gg" + " WHERE gg." + GamesTable.COL_NAME
                + " LIKE '" + game.toString() + "' LIMIT 1";
        return out;
    }

    /**
     * Provides the SQL for checking a profile type.
     * 
     * @param profile the profile type
     * 
     * @return the ID query
     */
    private static String getProfileIDSQL(final ProfileType profile) {
        String out = "SELECT pt." + ProfileTypeTable.COL_ID + " FROM "
                + ProfileTypeTable.NAME + " pt" + " WHERE pt."
                + ProfileTypeTable.COL_NAME + " LIKE " + "'"
                + profile.toString() + "'";
        return out;
    }

    /**
     * Provides the SQL for getting a transaction type ID.
     * 
     * @param tzxtype the transaction type
     * 
     * @return the ID query
     */
    private static String getTzxTypeIDSQL(final TransactionType tzxtype) {
        String out = "SELECT tt." + TransactionTypesTable.COL_ID + " FROM "
                + TransactionTypesTable.NAME + " tt" + " WHERE tt."
                + TransactionTypesTable.COL_TYPE + " LIKE '"
                + tzxtype.toString() + "'";
        return out;
    }

    /**
     * Provides the SQL for getting a host group ID.
     * 
     * @param group the group name
     * 
     * @return the ID query
     */
    private static String getRankGroupIDSQL(final String group) {
        String out = "(SELECT hg." + HostGroupsTable.COL_ID + " FROM "
                + HostGroupsTable.NAME + " hg" + " WHERE hg."
                + HostGroupsTable.COL_NAME + " LIKE '" + group + "')";
        return out;
    }
    
  
    /**
     * Gets the total profit, or loss for a game on a particular profile.
     * @param game  the game
     * @param date  the date in the format YYYY-MM-DD
     * @param prof  the profile
     * @return  the profit or loss
     * @throws SQLException when a database error occurs
     */
    public double turnoverForGame(final GamesType game,
                                  final String date,
                                  final ProfileType prof)
        throws SQLException {
        double res = 0.0;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE " + TransactionsTable.COL_GAMEID + "=(" 
                + getGameIDSQL(game) + ") AND " + TransactionsTable.COL_PROFILETYPE + "=("
                + getProfileIDSQL(prof) + ") AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " 00:00:00' AND '" + date + " 23:59:59'";
              
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                res = rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        // return the negation since that is from the houses pov
        return -res;
    }
    
    /**
     * Gets the total chips sold for a profile to users.
     * @param date  the date in the format YYYY-MM-DD
     * @param prof  the profile
     * @return  the amount of chips sold to users
     * @throws SQLException when a database error occurs
     */
    public double chipsSoldForProfile(final String date,
                                  final ProfileType prof)
        throws SQLException {
        double res = 0.0;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE " + TransactionsTable.COL_GAMEID + "=(" 
                + getGameIDSQL(GamesType.ADMIN) + ") AND " + TransactionsTable.COL_PROFILETYPE + "=("
                + getProfileIDSQL(prof) + ") AND " + TransactionsTable.COL_TYPEID + "=(" 
                + getTzxTypeIDSQL(TransactionType.CREDIT) + ") AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " 00:00:00' AND '" + date + " 23:59:59'";
              
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                res = rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        
        return res;
    }
    
    /**
     * Gets the total chips paid out by a certain profile to users.
     * @param date  the date in the format YYYY-MM-DD
     * @param prof  the profile
     * @return  the amount of chips sold to users
     * @throws SQLException when a database error occurs
     */
    public double chipsPaidoutForProfile(final String date,
                                  final ProfileType prof)
        throws SQLException {
        double res = 0.0;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE " + TransactionsTable.COL_GAMEID + "=(" 
                + getGameIDSQL(GamesType.ADMIN) + ") AND " + TransactionsTable.COL_PROFILETYPE + "=("
                + getProfileIDSQL(prof) + ") AND " + TransactionsTable.COL_TYPEID + "=(" 
                + getTzxTypeIDSQL(TransactionType.PAYOUT) + ") AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " 00:00:00' AND '" + date + " 23:59:59'";
              
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                res = rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        
        return -res;
    }
    
 
    /**
     * Given a profile and a date, get the total bet volume.
     * @param date  the date we are looking at
     * @param prof  the profile we are looking at
     * @return the amount that was bet on that profile
     * @throws SQLException when a database error occurs
     */
    public double betVolumeProfile(final String date,
                                  final ProfileType prof)
        throws SQLException {
        double res = 0.0;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE "  + TransactionsTable.COL_PROFILETYPE + "=("
                + getProfileIDSQL(prof) + ") AND " + TransactionsTable.COL_AMOUNT + "<0 AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " 00:00:00' AND '" + date + " 23:59:59'";
              
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                res = rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        // return the negation since that is from the houses pov
        return -res;
    }
    
    /**
     * Gets the total bet volume game on a particular profile.
     * @param game  the game
     * @param date  the date in the format YYYY-MM-DD
     * @param prof  the profile
     * @return  the profit or loss
     * @throws SQLException when a database error occurs
     */
    public double volumeForGame(final GamesType game,
                                  final String date,
                                  final ProfileType prof)
        throws SQLException {
        double res = 0.0;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        String sql = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE " + TransactionsTable.COL_GAMEID + "=(" 
                + getGameIDSQL(game) + ") AND " + TransactionsTable.COL_PROFILETYPE + "=("
                + getProfileIDSQL(prof) + ") AND " + TransactionsTable.COL_AMOUNT + " < 0 AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " 00:00:00' AND '" + date + " 23:59:59'";
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            if (rs.next()) {
                res = rs.getDouble("total");
            }
        } catch (SQLException e) {
            throw new DBException(e, sql);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                throw e;
            }
        }
        // return the negation since that is from the houses pov
        return -res;
    }
}
