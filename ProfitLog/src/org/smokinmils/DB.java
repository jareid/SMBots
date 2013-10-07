/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.smokinmils.bot.Utils;
import org.smokinmils.database.tables.GamesTable;
import org.smokinmils.database.tables.ProfileTypeTable;
import org.smokinmils.database.tables.TransactionTypesTable;
import org.smokinmils.database.tables.TransactionsTable;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

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
                                            + "smgamer.com" + ":"
                                            + "3306" + "/"
                                            + "live" + "?autoReconnect=true";

    /**
     * Constructor.
     * 
     * @throws Exception when we fail to retrieve a connection
     * 
     *  see http://www.mchange.com/projects/c3p0/
     */
    private DB() throws Exception {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("minPoolSize", "12");
        props.put("initialPoolSize", "12");
        props.put("maxPoolSize", "30");
        props.put("numHelperThreads", "12");
        props.put("aquireIncrement", "5");
        props.put("autoReconnect", "true");
        props.put("testConnectionOnCheckout", "true");
        props.put("maxIdleTime", new Integer(MAX_IDLE_MINS * Utils.MS_IN_MIN));
        
        unpooled = DataSources.unpooledDataSource(url,
                                                  "smbot",
                                                  "SM_bot_2013$");
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
        String sqlTemplate = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") AS total FROM " 
                + TransactionsTable.NAME + " WHERE " + TransactionsTable.COL_GAMEID + "=(" 
                + getGameIDSQL(game) + ") AND " + TransactionsTable.COL_PROFILETYPE + "=("
                + getProfileIDSQL(prof) + ") AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " %min:00:00' AND '" + date + " %max:59:59'";
              
        try {
            conn = getConnection();
            for(int hour = 0; hour < 24; hour ++) {
	            stmt = conn.createStatement();
	            String sql = sqlTemplate.replaceAll("%min", String.format("%02d", hour));
	            sql = sql.replaceAll("%max", String.format("%02d", hour));
	            rs = stmt.executeQuery(sql);
	            if (rs.next()) {
	                res =  res + rs.getDouble("total");
	            }
            }
        } catch (SQLException e) {
            EventLog.log(e, "ProfileLog DB", "turnoverForGame");
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
        String sqlTemplate = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE " + TransactionsTable.COL_GAMEID + "=(" 
                + getGameIDSQL(GamesType.ADMIN) + ") AND " + TransactionsTable.COL_PROFILETYPE
                + "=("
                + getProfileIDSQL(prof) + ") AND " + TransactionsTable.COL_TYPEID + "=(" 
                + getTzxTypeIDSQL(TransactionType.CREDIT) + ") AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " %min:00:00' AND '" + date + " %max:59:59'";
              
        try {
        	 conn = getConnection();
             for(int hour = 0; hour < 24; hour ++) {
 	            stmt = conn.createStatement();
 	            String sql = sqlTemplate.replaceAll("%min", String.format("%02d", hour));
 	            sql = sql.replaceAll("%max", String.format("%02d", hour));
 	            rs = stmt.executeQuery(sql);
 	            if (rs.next()) {
 	                res =  res + rs.getDouble("total");
 	            }
             }
        } catch (SQLException e) {
            EventLog.log(e, "ProfitLog DB", "chipsSoldForProfile");

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
        String sqlTemplate = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE " + TransactionsTable.COL_GAMEID + "=(" 
                + getGameIDSQL(GamesType.ADMIN) + ") AND " + TransactionsTable.COL_PROFILETYPE
                + "=("
                + getProfileIDSQL(prof) + ") AND " + TransactionsTable.COL_TYPEID + "=(" 
                + getTzxTypeIDSQL(TransactionType.PAYOUT) + ") AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " %min:00:00' AND '" + date + " %max:59:59'";
              
        try {
        	 conn = getConnection();
             for(int hour = 0; hour < 24; hour ++) {
 	            stmt = conn.createStatement();
 	            String sql = sqlTemplate.replaceAll("%min", String.format("%02d", hour));
 	            sql = sql.replaceAll("%max", String.format("%02d", hour));
 	            rs = stmt.executeQuery(sql);
 	            if (rs.next()) {
 	                res =  res + rs.getDouble("total");
 	            }
             }
        } catch (SQLException e) {
            EventLog.log(e, "ProfitLog DB", "chipsPaidoutForPRofile");

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
        String sqlTemplate = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE "  + TransactionsTable.COL_PROFILETYPE + "=("
                + getProfileIDSQL(prof) + ") AND " + TransactionsTable.COL_AMOUNT + "<0 AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " %min:00:00' AND '" + date + " %max:59:59'";
              
        try {
        	 conn = getConnection();
             for(int hour = 0; hour < 24; hour ++) {
 	            stmt = conn.createStatement();
 	            String sql = sqlTemplate.replaceAll("%min", String.format("%02d", hour));
 	            sql = sql.replaceAll("%max", String.format("%02d", hour));
 	            rs = stmt.executeQuery(sql);
 	            if (rs.next()) {
 	                res =  res + rs.getDouble("total");
 	            }
             }
        } catch (SQLException e) {
            EventLog.log(e, "ProfitLog DB", "betVolumeProfile");
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
        String sqlTemplate = "SELECT sum(" + TransactionsTable.COL_AMOUNT + ") as total FROM " 
                + TransactionsTable.NAME + " WHERE " + TransactionsTable.COL_GAMEID + "=(" 
                + getGameIDSQL(game) + ") AND " + TransactionsTable.COL_PROFILETYPE + "=("
                + getProfileIDSQL(prof) + ") AND " + TransactionsTable.COL_AMOUNT + " < 0 AND "
                + TransactionsTable.COL_TIMESTAMP + " BETWEEN '" 
                + date + " %min:00:00' AND '" + date + " %max:59:59'";
        
        try {
        	 conn = getConnection();
             for(int hour = 0; hour < 24; hour ++) {
 	            stmt = conn.createStatement();
 	            String sql = sqlTemplate.replaceAll("%min", String.format("%02d", hour));
 	            sql = sql.replaceAll("%max", String.format("%02d", hour));
 	            rs = stmt.executeQuery(sql);
 	            if (rs.next()) {
 	                res =  res + rs.getDouble("total");
 	            }
             }
        } catch (SQLException e) {
            EventLog.log(e, "ProfitLog DB", "volumeForGame");
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
     * Provides the SQL for retrieving the Poker game ID.
     * 
     * @param game the game type
     * 
     * @return the ID query
     */
    private static String getGameIDSQL(final GamesType game) {
        String out = "SELECT gg." + GamesTable.COL_ID + " FROM "
                + GamesTable.NAME + " gg" + " WHERE gg." + GamesTable.COL_NAME
                + " = '" + game.toString() + "' LIMIT 1";
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
                + ProfileTypeTable.COL_NAME + " = " + "'"
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
                + TransactionTypesTable.COL_TYPE + " = '"
                + tzxtype.toString() + "'";
        return out;
    }
}
