/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.bot;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * A utility class that provides useful functions to the entire project.
 * 
 * @author Jamie Reid
 */
public final class Utils {
    /** The number of digits we round decimals to. */
    private static final double CHIPS_ROUND = 10.0;
    
    /** One credit. */
    private static final int ONE_CHIP = 1;

    /**
     * Hiding the default constructor.
     */
    private Utils() {

    }

    /** The number of ms in a min. */
    public static final int  MS_IN_MIN   = 60000;
    
    /** The number of ms in a sec. */
    public static final int  MS_IN_SEC   = 1000;

    /** The number of ms in a min. */
    public static final int  MIN_IN_HOUR = 60;

    /** The number of hours in a day. */
    private static final int HOUR_IN_DAY = 24;

    /** Message for denying bets if they bet less than 1 coin with more than 1 on account. */
    private static final String MINIMUM_BET = "%b%c04%who%c12: If you have more than 1 coin "
                                                            + "you must bet more than 1 coin";
    
    /** Not enough chips message. */
    private static final String  NO_CHIPS        = "%b%c04%username%c12: "
                                     + "You do not have enough chips for that!";
    

    /**
     * A method that will handle parsing of integers without throwing an
     * exception.
     * 
     * @param text The input string
     * 
     * @return The resulting integer
     */
    public static Integer tryParse(final String text) {
        try {
            return new Integer(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * A method that will handle parsing of doubles without throwing an
     * exception.
     * 
     * @param text The input string
     * 
     * @return The resulting double
     */
    public static Double tryParseDbl(final String text) {
        try {
            return new Double(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts a list of objects to a string included ',' and 'and' correctly.
     * 
     * @param <E> The object type.
     * @param list The list of objects
     * 
     * @return The output string
     */
    public static <E> String listToString(final List<E> list) {
        String out = "";
        int i = 0;
        for (E item : list) {
            if (item == null) {
                out += "NULL";
            } else {
                out += item.toString();
            }

            if (list.size() > 1) {
                if (i == (list.size() - 2)) {
                    out += " and ";
                } else if (i < (list.size() - 2)) {
                    out += ", ";
                }
            }
            i++;
        }
        return out;
    }

    /**
     * Formats chips as a String with the correct number of decimal points.
     * 
     * @param chips The amount of chips to format
     * 
     * @return A string representing the chips
     */
    public static String chipsToString(final double chips) {
        double out = Math.floor(chips * CHIPS_ROUND) / CHIPS_ROUND;
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(out);
    }

    /**
     * Checks if the first word of a string matches the provided word.
     * 
     * Note: this is case insensitive
     * 
     * @param message The entire message
     * @param start The word the first word should match
     * 
     * @return true if the word matches, false otherwise.
     */
    public static boolean startsWith(final String message,
                                     final String start) {
        String[] msg = message.split(" ");
        boolean ret = false;
        if (msg.length >= 1 && msg[0].equalsIgnoreCase(start)) {
            ret = true;
        }
        return ret;
    }

    /**
     * Formats seconds into a time string.
     * 
     * @param secs the number of seconds.
     * 
     * @return the string
     */
    public static String secondsToString(final int secs) {
        String fmt = "%%c04%d%%c12 day(s) " + "%%c04%d%%c12 hour(s) "
                + "%%c04%d%%c12 min(s)";
        int day = (MIN_IN_HOUR * MIN_IN_HOUR * HOUR_IN_DAY);
        int days = secs / day;
        int hours = (secs % day) / (MIN_IN_HOUR * MIN_IN_HOUR);
        int mins = ((secs % day) % (MIN_IN_HOUR * MIN_IN_HOUR)) / MIN_IN_HOUR;
        String duration = String.format(fmt, days, hours, mins);
        return duration;
    }
    
    /**
     * Wrapper for DB.checkCredits that prevents &lt; 1 chips bet when the
     * user  has more than 1. Also makes the user bet their total if
     * within current bet is within 0.05 of total on account.
     * 
     * @param user  The user who is betting
     * @param amount the amount being bet
     * @param bot the bot so we can send errrrr messages
     * @param chan the channel to send messages
     * 
     * @return the credits 
     */
    public static double checkCredits(final User user, 
                                      final Double amount,
                                      final IrcBot bot,
                                      final Channel chan) {
        double ret = 0.0;
        DB db = DB.getInstance();
        String username = user.getNick();
        
        try {
            double total = db.checkCredits(username, amount);
            // check if betting lt one and having gt 1
            if (total > 1 && amount < ONE_CHIP) {
                // tell them they need to bet at least one coinchip
                String out = MINIMUM_BET.replaceAll("%who", username);
                bot.sendIRCMessage(chan, out);
            } else {
                ret = total;
            }
        } catch (SQLException e) {
            EventLog.log(e, "Utils", "checkCredits");
        }
        return ret;
    }
    
    /**
     * Wrapper for DB.checkCredits that prevents &lt; 1 chips bet when the
     * user  has more than 1.
     * 
     * @param user  The user who is betting
     * @param amount the amount being bet
     * @param bot the bot so we can send errrrr messages
     * @param chan the channel to send messages
     * @param profile the profile type to check.
     * 
     * @return the credits 
     */
    public static int checkCreditsAsInt(final User user, 
                                      final int amount,
                                      final IrcBot bot,
                                      final Channel chan,
                                      final ProfileType profile) {
        int ret = 0;
        DB db = DB.getInstance();
        String username = user.getNick();
        
        try {
            int total = db.checkCreditsAsInt(username, profile);
            // check if betting lt one and having gt 1
            if (amount < ONE_CHIP) {
                // tell them they need to bet at least one coinchip
                String out = MINIMUM_BET.replaceAll("%who", username);
                bot.sendIRCMessage(chan, out);
            } else if (total < amount) {
                bot.sendIRCMessage(chan, NO_CHIPS.replaceAll("%username", username));
            } else {
                ret = amount;
            }
        } catch (SQLException e) {
            EventLog.log(e, "Utils", "checkCredits");
        }
        return ret;
    }
}
