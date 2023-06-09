/**
 * 
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.rake;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.pircbotx.Channel;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;
import org.smokinmils.bot.Utils;
import org.smokinmils.cashier.ManagerSystem;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for jackpot rakes.
 * 
 * @author Jamie
 */
public final class Rake {
    /** The amount the jackpot is reset to upon winning. */
    private static final int MIN_JACKPOT = 100;

    /**
     * Hiding the default constructor.
     */
    private Rake() {

    }

    /** The channel where jackpot is announce. */
    private static String       jackpotChannel;
    
    /** The 1 in X chance of winning the jack pot.*/
    private static final int    JACKPOTCHANCE  = 500000;
    
    /** Amount of rake taken/generated from bets. */
    private static final double RAKE           = 0.01;
    
    /** Denotes if the jackpot is enabled of not. */
    public static final boolean JACKPOTENABLED = true;

    /** The string used to announce a jackpot has been won. */
    private static final String JACKPOTWON     = "%b%c01The %c04%profile%c01"
            + " jackpot of %c04%chips%c01 chips has been won in a %c04%game%c01"
            + " game! Congratulations to the winner(s):%c04 %winners %c01who"
            + " have shared the jackpot";

    /** The jackpot announce string. */
    private static final String JACKPOTANNOUNCE   = "%b%c01 There is a jackpot "
            + "promotion running for all games! All bets contribute to the "
            + "jackpot and all bets have a chance to win it, including poker "
            + "hands. The current jackpot sizes are: %jackpots";

    /** The jackpot announce string for each jackpot. */
    private static final String JP_AMOUNT       = "%c04%profile%c01 "
                                                        + "(%c04%amount%c01) ";

    /** The group failed announce string. */
    private static final String GROUP_FAIL   = "%b%c04%group%c12 has failed this week by "
                                             + "%c04%points%c12 less than the minimum of %c04%min.";

    /**
     * Initialise.
     * 
     * @param chan The jackpot channel.
     */
    public static void init(final String chan) {
        setJackpotChannel(chan);
    }

    /**
     * Get the amount of rake from a any game.
     * 
     * @param user The user.
     * @param bet The size of the bet.
     * @param profile The profile this is for.
     * 
     * @return the amount of rake taken.
     */
    public static synchronized double getRake(final String user,
                                              final double bet,
                                              final ProfileType profile) {
        double rake = RAKE * bet;
        Referal.getInstance().addEvent(user, profile, rake);
        return rake;
    }

    /**
     * Get the amount of rake from a poker game.
     * 
     * @param user The user.
     * @param rake The amount of rake taken.
     * @param profile The profile this is for.
     */
    public static synchronized void getPokerRake(final String user,
                                                 final double rake,
                                                 final ProfileType profile) {
        Referal.getInstance().addEvent(user, profile, rake);
    }

    /**
     * Check if the jackpot has been won.
     * 
     * @param amount The amount bet
     * @return true if the jackpot was won, false otherwise.
     */
    public static synchronized boolean checkJackpot(final double amount) {
        boolean won = false;
        if (JACKPOTENABLED) {
            double chance = (1.0 / JACKPOTCHANCE) * amount;
            double random = Random.nextDouble();

            if (random <= chance) {
                won = true;
            }
        }
        return won;
    }

    /**
     * Jackpot has been won, split between all players on the table.
     * 
     * @param profile The jackpot profile
     * @param game The game it was won on
     * @param players The players who won
     * @param bot The bot used for output
     * @param channel The channel it was won on, can be null.
     */
    public static synchronized void jackpotWon(final ProfileType profile,
                                               final GamesType game,
                                               final List<String> players,
                                               final IrcBot bot,
                                               final Channel channel) {
        try {
            DB db = DB.getInstance();
            int jackpot = (int) Math.floor(db.getJackpot(profile));

            if (jackpot > 0) {
                int remainder = jackpot % players.size();
                jackpot -= remainder;

                if (jackpot != 0) {
                    int win = jackpot / players.size();
                    for (String player : players) {
                        db.jackpot(player, win, profile);
                    }

                    // Announce to channel
                    String out = JACKPOTWON.replaceAll(
                            "%chips", Integer.toString(jackpot));
                    out = out.replaceAll("%profile", profile.toString());
                    out = out.replaceAll("%winners", Utils.listToString(players));
                    out = out.replaceAll("%game", game.toString());

                    if (channel != null
                       && !channel.getName().equalsIgnoreCase(getJackpotChannel())) {
                        bot.sendIRCMessage(channel, out);
                        bot.sendIRCMessage(channel, out);
                        bot.sendIRCMessage(channel, out);
                    }
                    
                    Channel jpchan = bot.getUserChannelDao().getChannel(getJackpotChannel());

                    bot.sendIRCMessage(jpchan, out);
                    bot.sendIRCMessage(jpchan, out);
                    bot.sendIRCMessage(jpchan, out);

                    db.updateJackpot(profile, MIN_JACKPOT);
                }
            }
        } catch (Exception e) {
            EventLog.log(e, "Jackpot", "updateJackpot");
        }
    }

    /**
     * @return the current state of jackpots.
     */
    public static String getJackpotsString() {
        String jackpotstr = "";
        int i = 0;
        for (ProfileType profile : ProfileType.values()) {
            int jackpot = 0;
            try {
                jackpot = (int) Math.floor(DB.getInstance().getJackpot(profile));
            } catch (Exception e) {
                EventLog.log(e, "Jackpot", "getJackpotsString");
            }

            jackpotstr += JP_AMOUNT.replaceAll("%profile", profile.toString())
                    .replaceAll("%amount", Integer.toString(jackpot));
            if (i == (ProfileType.values().length - 2)) {
                jackpotstr += " and ";
            } else if (i < (ProfileType.values().length - 2)) {
                jackpotstr += ", ";
            }
            i++;
        }
        return jackpotstr;
    }

    /**
     * @return The announce line for all jackpots.
     */
    public static String getAnnounceString() {
        String jackpotstr = Rake.getJackpotsString();
        String out = JACKPOTANNOUNCE.replaceAll("%jackpots", jackpotstr);
        return out;
    }
    
    /**
     * Process rank point earnings.
     * 
     * @param bot the irc bot instance.
     * 
     * @throws SQLException when a database error occurs.
     */
    public static void processPoints(final IrcBot bot) throws SQLException {
        DB db = DB.getInstance();
        // get the total points
        int points = db.getPointTotal();
        int minpoints = db.getMinPoints();
        Map<String, Integer> grouppoints = new HashMap<String, Integer>();
        Map<String, Integer> groupusers = new HashMap<String, Integer>();
        
        Map<String, Integer> allpoints = db.getPoints();
        for (Entry<String, Integer> ent: allpoints.entrySet()) {
            // note how many points the group have.
            String group = db.getRankGroup(ent.getKey());
            if (!grouppoints.containsKey(group)) {
                grouppoints.put(group, ent.getValue());
                groupusers.put(group, 1);
            } else {
                grouppoints.put(group, grouppoints.get(group) + ent.getValue());
                groupusers.put(group, groupusers.get(group) + 1);
            }
        }
        
        for (ProfileType profile: ProfileType.values()) {
            double pointearnings = db.checkCredits(DB.POINTS_USER, profile);
            double eachpoint = pointearnings / points;
            if (eachpoint == Double.NaN) {
                eachpoint = 0.0;
            }
            
            // calculate each users 
            for (Entry<String, Integer> ent: allpoints.entrySet()) {            
                if (ent.getValue() > 0) {
                    double amnt = ent.getValue() * eachpoint;
    
                    db.adjustChips(ent.getKey(), amnt, profile,
                                   GamesType.ADMIN, TransactionType.POINTS);
                }
            }
            
            db.adjustChips(DB.POINTS_USER, -pointearnings, profile,
                    GamesType.ADMIN, TransactionType.POINTS);
        }
        
        // See which groups failed.
        for (Entry<String, Integer> entry: grouppoints.entrySet()) {
            Integer users = groupusers.get(entry.getKey());
            if (users != null) {
                int min = users * minpoints;
                if (minpoints > entry.getValue()) {
                    int fail = min - entry.getValue();
                    String out = GROUP_FAIL.replaceAll("%group", entry.getKey());
                    out = out.replaceAll("%min", Integer.toString(min));
                    out = out.replaceAll("%points", Integer.toString(fail));
                    
                    Channel chan = bot.getUserChannelDao()
                                                .getChannel(ManagerSystem.getManagerChan());
                    bot.sendIRCMessage(chan, out);
                }
            }
        }
        
        // reset points
        db.resetPoints();
    }

    /**
     * @return the jackpotChannel
     */
    public static String getJackpotChannel() {
        return jackpotChannel;
    }

    /**
     * @param chan the jackpotChannel to set
     */
    private static void setJackpotChannel(final String chan) {
        Rake.jackpotChannel = chan;
    }
}
