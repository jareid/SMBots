/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.BaseBot;
import org.smokinmils.bot.CheckIdentified;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Pair;
import org.smokinmils.bot.SpamEnforcer;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.cashier.tasks.Competition;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.BetterInfo;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.UserStats;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality that is available for all users.
 * 
 * @author Jamie
 */
public class UserCommands extends Event {
    /** The check command. */
	public static final String CHKCMD           = "!check";

    /** The check command description. */
	public static final String CHKFMT           = "%b%c12" + CHKCMD + " ?user?";
	
    /** The profiles command. */
    public static final String        PROFSCMD  = "!profiles";

    /** The profiles command format. */
    public static final String        PROFSFMT  = "%b%c12" + PROFSCMD + "";

    /** The change profile command. */
    public static final String        PCHNGCMD  = "!profile";

    /** The change profile command format. */
    public static final String        PCHNGFMT  = "%b%c12" + PROFSCMD + " <profile>";
    
    /** The jack pots command. */
    public static final String JPCMD            = "!jackpots";

    /** The jack pots command format. */
    public static final String JPFMT            = "%b%c12" + JPCMD + "";
    
    /** The transfer command. */
    public static final String  TRANCMD         = "!transfer";
    
    /** The transfer command format. */
    public static final String  TRANFMT         = "%b%c12" + TRANCMD + " <user> <amount> <profile>";

    /** The transfer command length. */
    public static final int     TRAN_CMD_LEN    = 4;
    
    /** The position command. */
    public static final String  POSCMD          = "!position";
    
    /** The position command format. */
    public static final String  POSFMT          = "%b%c12" + POSCMD + " <profile> <user>";
    
    /** The stats command. */
    public static final String STATCMD = "!stats";
    
    /** The stats command. */
    public static final String SHOWSTATCMD = "!showstats";
    
    /** The competition command. */
    public static final String COMPCMD = "!competitions";
    
    /** The competition command. */
    public static final String COMPCMD2 = "!competition";    
    
    /** The position command length. */
    public static final int     POS_CMD_LEN     = 3;

    /** The message when the user does not exist. */
    private static final String NO_USER         = "%b%c04%sender:%c12 "
                                                + "%c04%who%c12 does not exist in the database";

    /** The transfer message for the channel. */
    private static final String TRANSFERRERD    = "%b%c04%sender%c12 has transfered %c04%amount%c12"
                                                + " chips to the %c04%profile%c12 account of "
                                                + "%c04%who%c12";

    /** The transfer message for the receiver. */
    private static final String TRANSFERCHIPSUSER   = "%b%c12You have had %c04%amount%c12 chips "
                                                    + "transfered into your %c04%profile%c12"
                                                    + " account by %c04%sender%c12";
            
    /** The transfer message for the sender. */
    private static final String TRANSFERCHIPSSENDER = "%b%c12You have transferred %c04%amount%c12 "
                                                    + "chips from your %c04%profile%c12"
                                                    + " account to %c04%who%c12";
    /** The public stats message. */
    private static final String PUBLIC_STATS   = "%b%c04%sender%c12: You have made your stats"
    		                                   + " public";
    
    /** The public stats message. */
    private static final String PRIVATE_STATS  = "%b%c04%sender%c12: You have made your stats"
                                               + " private";
    
    /** The no stats message. */
    private static final String NOSTATS   = "%b%c04%sender%c12: %c04%user%c12 has no stats to"
                                          + " display.";
    
    /** The no stats message. */
    private static final String HIDDENSTATS  = "%b%c04%sender%c12: %c04%user%c12 has disabled their"
                                          + " public stats.";
    
    /** The stats message. */
    private static final String STATS = "%b%c04%user%c12's %c04%profile%c12 stats: "
                                      + "Total Bet(%c04%bet_total%c12) "
                                      + "Total Won(%c04%win_total%c12) "
                                      + "Referral Earnings(%c04%refer_total%c12)";
    
	/** Used to specify the Check message on another user. */
	public static final String CHECKCREDITSMSG     =  "%b%c04%sender%c12: %c04%user %c12currently "
	                           + "has %c04%creds%c12 chips on the active profile (%c04%active%c12)";
	
	/** Used to specify the other profiles in the check message. */
	public static final String OTHERPROFILES       = " | %c04%name%c12 (%c04%amount%c12)";
	
	/** Used to specify the user has no credits. */
	public static final String NOCREDITS           = "%b%c04%sender: %c04%user%c12 %c12currently "
	                                               + "has %c04no%c12 available chips.";

    /** Profile changed message. */
    public static final String PROFILE_CHANGED     = "%b%c04%user %c12is now " 
                                                   + "using the %c04%profile%c12 game profile";
    
    /** Failed to change the profile for user message. */
    public static final String PROFILECHANGE_FAIL  = "%b%c04%user %c12tried to"
                                                   + " change to the %c04%profile%c12 game profile "
                                                   + "and it failed. Please try again!";


    /** The jack pot announce string. */
    public static final String JP_INFO             = "%b%c12The current jackpot sizes "
                                           + "are: %jackpots. Every poker hand and bet has a chance"
                                           + " to win the jackpot.";

    /** Message for a user's position in the competition. */
    private static final String       POSITION          = "%b%c04%sender:%c12 "
           + "%c04%who%c12 is currently in position %c04%position%c12 for the "
           + "%c04%profile%c12 competition with %c04%chips%c12 chips bet";
    
    /** Message when a user is not ranked for the competition. */
    private static final String       NOTRANKED         = "%b%c04%sender:%c12 "
                     + "%c04%who%c12 is currently in %c04unranked%c12 for the "
                     + "%c04%profile%c12 competition";

    /** Last 30 days message. */
    private static final String       LAST30DAYS        =
           "%b%c04(%c12Last 30 days on the %c04%profile%c12 profile%c04)%c12 "
       + "%c04%who%c12 highest bet was %c04%hb_chips%c12 on %c04%hb_game%c12 | "
       + "%c04%who%c12 bet total is %c04%hbt_chips%c12";
    
    
    /** No data for the last 30 days message. */
    private static final String       LAST30DAYS_NODATA = "%b%c04(%c12Last 30 "
            + "days on the %c04%profile%c12 profile%c04)%c12 There is no data "
            + "for %c04%who%c12 on this profile.";

    /** No competition running message. */
    private static final String       NOCOMPETITION     = "%b%c04%sender:%c12 "
           + "There is no competition running for the %c04%profile%c12 profile";

    @Override
    public final void join(final Join event) {
        String nick = event.getUser().getNick();
        if (!nick.equalsIgnoreCase(event.getBot().getNick())
                && event.getChannel().getName().equalsIgnoreCase(Rake.getJackpotChannel())) {
            // Existing user, announce better tier
            double bet = 0.0;
            try {
                bet = DB.getInstance().getAllTimeTotal(nick);
            } catch (SQLException e) {
                EventLog.log(e, "CheckIdentified", "join");
            }
            String tier = CheckIdentified.OTHERSTR;
        
            if (bet >= CheckIdentified.ELITE) {
                tier = CheckIdentified.ELITESTR;
            } else if (bet >= CheckIdentified.PLATINUM) {
                tier = CheckIdentified.PLATINUMSTR;
            } else if (bet >= CheckIdentified.GOLD) {
                tier = CheckIdentified.GOLDSTR;
            } else if (bet >= CheckIdentified.SILVER) {
                tier = CheckIdentified.SILVERSTR;
            } else if (bet >= CheckIdentified.BRONZE) {
                tier = CheckIdentified.BRONZESTR;
            }
            
            String out = CheckIdentified.JOIN_MSG.replaceAll("%user", event.getUser().getNick())
                                                 .replaceAll("%tier", tier);
            event.getBot().addJoinMsg(new Pair(event.getChannel(), out));
        }
    }
    
    /**
     * This method handles the chips commands.
     * 
     * @param event the Message event
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        
        if (isValidChannel(chan.getName()) && bot.userIsIdentified(sender)) {
            if (Utils.startsWith(message, CHKCMD)) {
                checkChips(event);
            } else if (Utils.startsWith(message, STATCMD)) {
                checkStats(event);
            } else if (Utils.startsWith(message, SHOWSTATCMD)) {
                showStats(event);
            } else if (Utils.startsWith(message, TRANCMD)) {
                transferChips(event);
            } else if (Utils.startsWith(message, JPCMD)) {
                jackpots(event);
            } else if (Utils.startsWith(message, PROFSCMD)) {
                profiles(event);
            } else if (Utils.startsWith(message, PCHNGCMD)) {
                changeProfile(event);
            } else if (Utils.startsWith(message, POSCMD)) {
                compPosition(event);
            } else if (Utils.startsWith(message, COMPCMD) || Utils.startsWith(message, COMPCMD2)) {
                competitions(event);
            }
        }
    }
    
    /**
     * This method handles the chips command.
     * 
     * @param event the Message event
     */
    public final void checkChips(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        if (isValidChannel(event.getChannel().getName())
                && bot.userIsIdentified(senderu)
                && Utils.startsWith(message, CHKCMD)) {
            String[] msg = message.split(" ");
            String user = "";

            if (msg.length == 1 || msg.length == 2) {
                if (msg.length > 1) {
                    user = msg[1];
                } else {
                    user = sender;
                }
                
                Map<ProfileType, Double> creds = null;
                boolean rsrct = (user.equalsIgnoreCase("HOUSE") || user.equalsIgnoreCase("POINTS"));
                if ((rsrct && (bot.userIsHalfOp(senderu, event.getChannel().getName())
                            || bot.userIsOp(senderu, event.getChannel().getName())))
                     || !rsrct) {
                    try {
                        creds = DB.getInstance().checkAllCredits(user);
                    } catch (Exception e) {
                        EventLog.log(e, "CheckChips", "message");
                    }
                }

                String credstr;
                if (creds.size() != 0) {
                    ProfileType active = null;
                    try {
                        active = DB.getInstance().getActiveProfile(user);
                    } catch (Exception e) {
                        EventLog.log(e, "CheckChips", "message");
                    }

                    credstr = CHECKCREDITSMSG;

                    Double activecreds = creds.get(active);
                    if (activecreds == null) {
                        activecreds = 0.0;
                    }
                    credstr = credstr.replaceAll("%creds",
                            Utils.chipsToString(activecreds));
                    credstr = credstr.replaceAll("%active", active.toString());

                    if (creds.size() > 1) {
                        for (Entry<ProfileType, Double> cred 
                                : creds.entrySet()) {
                            if (cred.getKey().compareTo(active) != 0) {
                                String othercred = OTHERPROFILES.replaceAll(
                                        "%name", cred.getKey().toString());
                                othercred = othercred.replaceAll("%amount",
                                        Utils.chipsToString(cred.getValue()));
                                credstr += othercred + " ";
                            }
                        }
                    }
                } else {
                    credstr = NOCREDITS;
                }

                /*if (user.equalsIgnoreCase(sender)) {
                    user = "You";
                }*/ 
                credstr = credstr.replaceAll("%user", user);
                credstr = credstr.replaceAll("%sender", sender);

                bot.sendIRCMessage(event.getChannel(), credstr);
            } else {
                bot.invalidArguments(senderu, CHKFMT);
            }
        }
    }
    
    /**
     * This method handles the show stats command.
     * 
     * @param event the Message event
     */
    public final void showStats(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();

        if (isValidChannel(event.getChannel().getName())
                && bot.userIsIdentified(senderu)
                && Utils.startsWith(message, SHOWSTATCMD)) {
            boolean canshow = false;
            try {
                canshow = DB.getInstance().hasPublicStats(sender);
                DB.getInstance().setPublicStats(sender, !canshow);
            } catch (Exception e) {
                EventLog.log(e, "CheckChips", "message");
            }
            
            String out = PRIVATE_STATS;
            if (!canshow) {
                out = PUBLIC_STATS;
            }
            
            out = out.replaceAll("%sender", sender);
            bot.sendIRCNotice(senderu, out);
            
        }
    }
    
    /**
     * This method handles the stats command.
     * 
     * @param event the Message event
     */
    public final void checkStats(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();

        if (isValidChannel(event.getChannel().getName())
                && bot.userIsIdentified(senderu)
                && Utils.startsWith(message, STATCMD)) {
            String[] msg = message.split(" ");
            String user = "";

            if (msg.length == 1 || msg.length == 2) {
                if (msg.length > 1) {
                    user = msg[1];
                } else {
                    user = sender;
                }
                
                Map<ProfileType, UserStats> stats = null;
                boolean canshow = true;
                boolean rsrct = (user.equalsIgnoreCase("HOUSE") || user.equalsIgnoreCase("POINTS"));
                if (!rsrct) {
                    try {
                        if (user != sender) {
                            canshow = DB.getInstance().hasPublicStats(user);
                        }
                        stats = DB.getInstance().checkStats(user);
                    } catch (Exception e) {
                        EventLog.log(e, "CheckChips", "message");
                    }
                }
                
                if (user.equalsIgnoreCase(sender)) {
                    user = "You";
                }
                
                String statstr = "";
                if (canshow) {
                    if (stats.size() != 0) {
                        for (Entry<ProfileType, UserStats> cred : stats.entrySet()) {
                            statstr = STATS;
                            statstr = statstr.replaceAll("%user", user);
                            statstr = statstr.replaceAll("%refer_total",
                                    Utils.chipsToString(cred.getValue().getRefertotal()));
                            statstr = statstr.replaceAll("%bet_total",
                                    Utils.chipsToString(cred.getValue().getBettotal()));
                            statstr = statstr.replaceAll("%win_total",
                                    Utils.chipsToString(cred.getValue().getWintotal()));
                            statstr = statstr.replaceAll("%profile", cred.getKey().toString());
                            statstr = statstr.replaceAll("%user", user);
                            statstr = statstr.replaceAll("%sender", sender);

                            bot.sendIRCNotice(senderu, statstr);
                        }
                    } else {
                        statstr = NOSTATS;
                        
                        statstr = statstr.replaceAll("%user", user);
                        statstr = statstr.replaceAll("%sender", sender);

                        bot.sendIRCNotice(senderu, statstr);
                    }
                } else {
                    statstr = HIDDENSTATS;
                    statstr = statstr.replaceAll("%user", user);
                    statstr = statstr.replaceAll("%sender", sender);

                    bot.sendIRCNotice(senderu, statstr);
                }
            } else {
                bot.invalidArguments(senderu, CHKFMT);
            }
        }
    }
    
    /**
     * This method handles the command.
     * 
     * @param event the message event.
     */
    public final void transferChips(final Message event) {
        synchronized (BaseBot.getLockObject()) {
            IrcBot bot = event.getBot();
            String message = event.getMessage();
            User senderu = event.getUser();
            String sender = senderu.getNick();
            Channel chan = event.getChannel();

            String[] msg = message.split(" ");

            if (msg.length == TRAN_CMD_LEN) {
                String user = msg[1];
                Integer amount = Utils.tryParse(msg[2]);
                ProfileType profile = ProfileType.fromString(msg[TRAN_CMD_LEN - 1]);

                if (!user.isEmpty() && !user.equals(sender) && amount != null) {
                    // Check valid profile
                    if (!bot.userIsIdentified(user) && bot.manualStatusRequest(user)) {
                        String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", user);
                        bot.sendIRCMessage(senderu, out);
                    } else if (profile == null) {
                        bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                    } else {
                        try {
                            int chips = DB.getInstance().checkCreditsAsInt(sender, profile);
                            
                            if (amount > chips || amount < 0) {
                                bot.noChips(senderu, amount, profile);
                            } else if (!DB.getInstance().checkUserExists(user)) {
                                String out = NO_USER.replaceAll("%who", user);
                                out = out.replaceAll("%sender", sender);
                                bot.sendIRCMessage(chan, out);
                            } else {
                                DB.getInstance().transferChips(sender, user, amount, profile);

                                // Send message to channel
                                String out = TRANSFERRERD.replaceAll("%who", user);
                                out = out.replaceAll("%sender", sender);
                                out = out.replaceAll("%amount", Integer.toString(amount));
                                out = out.replaceAll("%profile", profile.toString());
                                bot.sendIRCMessage(chan, out);

                                // Send notice to sender
                                out = TRANSFERCHIPSUSER.replaceAll("%who", user);
                                out = out.replaceAll("%sender", sender);
                                out = out.replaceAll("%amount", Integer.toString(amount));
                                out = out.replaceAll("%profile", profile.toString());

                                User usr = bot.getUserChannelDao().getUser(user);
                                bot.sendIRCNotice(usr, out);

                                // Send notice to user
                                out = TRANSFERCHIPSSENDER.replaceAll("%who", user);
                                out = out.replaceAll("%sender", sender);
                                out = out.replaceAll("%amount", Integer.toString(amount));
                                out = out.replaceAll("%profile", profile.toString());
                                bot.sendIRCNotice(senderu, out);
                            }
                        } catch (Exception e) {
                            EventLog.log(e, "TransferChips", "message");
                        }
                    }
                } else {
                    bot.invalidArguments(senderu, TRANFMT);
                }
            } else {
                bot.invalidArguments(senderu, TRANFMT);
            }
        }
    }
    
    /**
     * This method handles the jackpots command.
     * 
     * @param event the message event.
     */
    public final void jackpots(final Message event) {
        IrcBot bot = event.getBot();
        Channel chan = event.getChannel();

        bot.sendIRCMessage(chan, Rake.getAnnounceString());
    }
    
    /**
     * This method handles the profiles command.
     * 
     * @param event the Message event
     */
    public final void profiles(final Message event) {
        IrcBot bot = event.getBot();
        User sender = event.getUser();
        Channel chan = event.getChannel();

        bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
        bot.sendIRCNotice(sender, IrcBot.VALID_PROFILES);
    }
    
    /**
     * This method handles the change profile command.
     * 
     * @param event the Message event
     */
    public final void changeProfile(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");
        if (msg.length == 2) {
            ProfileType profile = ProfileType.fromString(msg[1]);
            if (profile != null) {
                boolean success = false;
                try {
                    success = DB.getInstance().updateActiveProfile(sender.getNick(), profile);
                } catch (Exception e) {
                    EventLog.log(e, "Profile", "message");
                }
                
                if (success) {
                    String out = PROFILE_CHANGED.replaceAll("%user", sender.getNick());
                    out = out.replaceAll("%profile", profile.toString());
                    
                    bot.sendIRCMessage(chan, out);
                } else {
                    String out = PROFILECHANGE_FAIL.replaceAll("%user", sender.getNick());
                    out = out.replaceAll("%profile", profile.toString());
                    
                    bot.sendIRCMessage(chan, out);
                    
                    EventLog.log(out, "Profile", "message");
                }
            } else {
                bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES.replaceAll("%profiles",
                                                                  ProfileType.values().toString()));
            }
        } else {
            bot.invalidArguments(event.getUser(), PROFSFMT);
        }
    }
    
    /**
     * This method handles the competition position command.
     * 
     * @param event The message event
     */
    public final void compPosition(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();       

        String[] msg = message.split(" ");
        
        SpamEnforcer se = SpamEnforcer.getInstance();
        
        if (msg.length == 2 || msg.length == POS_CMD_LEN) {
            if (se.checkPosition(event)) {
                bot.bePatient(event.getUser());
                String who;
                if (msg.length == 2) {
                    who = sender;
                } else {
                    who = msg[2];
                }
    
                ProfileType profile = ProfileType.fromString(msg[1]);
                if (profile == null) {
                    bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                } else if (!profile.hasComps()) {
                    String out = NOCOMPETITION.replaceAll("%sender", sender);
                    out = out.replaceAll("%profile", profile.toString());
                    bot.sendIRCMessage(chan, out);
                } else {
                    
                    BetterInfo better = null;
                    try {
                        better = DB.getInstance().competitionPosition(profile, who);
                    } catch (Exception e) {
                        EventLog.log(e, "CompPosition", "message");
                    }
    
                    String out = "";
                    if (better.getPosition() == -1) {
                        out = NOTRANKED.replaceAll("%profile", profile.toString());
                    } else {
                        out = POSITION.replaceAll("%profile", profile.toString());
                        out = out.replaceAll("%position", Integer.toString(better.getPosition()));
                        out = out.replaceAll("%chips", Long.toString(better.getAmount()));
                    }
    
                    out = out.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", who);
    
                    bot.sendIRCMessage(chan, out);
    
                    DB db = DB.getInstance();
                    for (ProfileType prof : ProfileType.values()) {
                        if (profile.hasComps()) {
                            try {
                                BetterInfo highbet = db.getHighestBet(prof, who);
                                BetterInfo topbet = db.getTopBetter(prof, who);
    
                                if (highbet.getUser() == null || topbet.getUser() == null) {
                                    out = LAST30DAYS_NODATA;
                                    out = out.replaceAll("%who", who);
                                } else {
                                    out = LAST30DAYS.replaceAll("%hb_game",
                                            highbet.getGame().toString());
                                    out = out.replaceAll("%hb_chips",
                                            Long.toString(highbet.getAmount()));
                                    out = out.replaceAll("%hbt_chips",
                                            Long.toString(topbet.getAmount()));
                                    out = out.replaceAll("%who", highbet.getUser());
                                }
                                out = out.replaceAll("%profile",  prof.toString());
    
                                bot.sendIRCNotice(senderu, out);
                            } catch (Exception e) {
                                EventLog.log(e, "BetDetails", "run");
                            }
                        }
                        
                    }  
                }
            }
        } else {
            bot.invalidArguments(senderu, POSFMT);
        }
    }
    
    /**
     * This method handles the competition command.
     * 
     * @param event The message event
     */
    public final void competitions(final Message event) {
        IrcBot bot = event.getBot();
        String chan = event.getChannel().getName();     
        
        Competition.announce(bot, chan);
    }
}

