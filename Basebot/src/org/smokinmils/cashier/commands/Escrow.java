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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.CheckIdentified;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.XMLLoader;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.EscrowResult;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to payout a user some chips.
 * 
 * @author Jamie
 */
public class Escrow extends Event {
    /** The payout command. */
    public static final String  TRADECMD       = "!trade";

    /** The payout command format. */
    public static final String  TRADEFMT      = "%b%c12" + TRADECMD + " <user> <amount> <profile>";
    
    /** The payout command length. */
    public static final int     TRADE_CMD_LEN     = 4;
    
    /** The give command. */
    public static final String  CONFIRMCMD     = "!confirm";

    /** The give command format. */
    public static final String  CONFIRMFMT      = "%b%c12" + CONFIRMCMD + " <user>";

    /** The message to the channel on trade start. */
    private static final String TRADE   = "%b%c04[TRADE]%c12 A %c04%profile%c12 trade between "
                                        + "%c04%rank%c12 and %c04%user%c12 has been started. "
                                        + "The %c04%chips%c12 chips have been removed from "
                                        + "%c04%rank%c12's account and are locked until "
                                        + "confirmation is received. %c04%rank%c12 when you "
                                        + "receive your gold please type %c04 " + CONFIRMCMD
                                        + " %user";
    
    /** The message to the channel on trade start. */
    private static final String NO_TRADE = "%b%c04[TRADE]%c12 No trade exists between "
                                         + "%c04%rank%c12 and %c04%user%c12. Please use %c04"
                                         + TRADECMD + "%c12 to start one.";
    
    /** The message to the channel on trade start. */
    private static final String NO_TRADE_OR_CONFIRM = "%b%c04[TRADE]%c12 No trade exists between "
                                         + "%c04%rank%c12 and %c04%user%c12or you can not confirm "
                                         + " this trade.";
    
    /** The message to the channel on trade start. */
    private static final String TRADE_EXIST = "%b%c04[TRADE]%c12 A trade already exists between "
                                            + "%c04%rank%c12 and %c04%user%c12. Please use "
                                            + CONFIRMCMD + "%c12 to confirm it.";
    
    /** The message to the channel on trade complete. */
    private static final String TRADECONF = "%b%c04[TRADE]%c12 A %c04%profile%c12 trade between "
                  + "%c04%rank%c12 and %c04%user%c12 for %c04%chips%c12 chips has been completed. ";
    
    /** Message when the user doesn't have enough chips. */
    public static final String  NOCHIPSMSG    = "%b%c12Sorry, %c04%user%c12 does not have "
                                              + "%c04%chips%c12 chips available for the "
                                              + "%c04%profile%c12 profile.";
    
    /** Percentage of chips taken. */
    private static final double PERCENTAGE = XMLLoader.getInstance().getTradeSetting("percent");
    
    /** Minimum chips to take. */
    private static final double MINIMUM = XMLLoader.getInstance().getTradeSetting("minimum");
    
    /** A list of users who have a confirm to do. */
    private final Map<String, List<String>> confirmList;
    
    /** A map of confirms and if they are a rank or not. */
    private final Map<String, Map<String, Boolean>> rankList;
    
    /**
     * Constructor.
     */
    public Escrow() {
        confirmList = new HashMap<String, List<String>>();
        rankList = new HashMap<String, Map<String, Boolean>>();
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
            if (Utils.startsWith(message, TRADECMD)) {
                sellChips(event);
            } else if (Utils.startsWith(message, CONFIRMCMD)) {
                confirm(event);
            }
        }
    }
    
    /**
     * This method handles the confirm command.
     * 
     * @param event the Message event
     */
    public final void confirm(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");
        
        List<String> usrlist = confirmList.get(sender.getNick().toLowerCase());
        
        if (msg.length == 2) {
            String user = msg[1];
            if (usrlist != null && usrlist.contains(user.toLowerCase())) {
                boolean exists = false;
                try {
                    exists = DB.getInstance().hasEscrow(sender.getNick(), user);
                } catch (Exception e) {
                    EventLog.log(e, "Escrow", "confirm");
                }
                
                if (bot.manualStatusRequest(user)) {
                    String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", user);
                    bot.sendIRCMessage(sender, out);
                } else if (!exists) {
                    String out = NO_TRADE.replaceAll("%user", user)
                                         .replaceAll("%rank", sender.getNick());
                    bot.sendIRCMessage(chan, out);
                } else {
                    DB db = DB.getInstance();
                    EscrowResult res = null;
                    try {
                        res = db.escrowConfirmed(sender.getNick(), user,
                                                 PERCENTAGE, MINIMUM,
                                                 getCommissionStatus(sender.getNick(), user));
                    } catch (Exception e) {
                        EventLog.log(e, "Escrow", "confirm");
                    }

                    if (res != null) {
                        double amnt = res.getAmount();
                        ProfileType profile = res.getProfile();
                        
                        String out = TRADECONF.replaceAll("%chips", Utils.chipsToString(amnt));
                        out = out.replaceAll("%user", user);
                        out = out.replaceAll("%rank", sender.getNick());
                        out = out.replaceAll("%profile", profile.toString());
                        
                        bot.sendIRCMessage(chan, out);                            
                    } else {
                        EventLog.log(sender.getNick() + " attempted to confirm a trade "
                                     + "the database failed", "GiveChips", "message");
                    }
                } // not identified.
            } else {
                String out = NO_TRADE_OR_CONFIRM.replaceAll("%user", user)
                                                .replaceAll("%rank", sender.getNick());
                bot.sendIRCMessage(chan, out);
            }
        } else {
            bot.invalidArguments(sender, CONFIRMFMT);
        }
    }
    
    /**
     * This method handles the payout command.
     * 
     * @param event the message event.
     */
    public final void sellChips(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");

        if (bot.userIsHost(event.getUser(), chan.getName())) {
            if (msg.length == TRADE_CMD_LEN) {
                String user = msg[1];
                Double amount = Utils.tryParseDbl(msg[2]);
                ProfileType profile = ProfileType.fromString(msg[TRADE_CMD_LEN - 1]);
                
                boolean exists = false;
                try {
                    exists = DB.getInstance().hasEscrow(sender, user);
                } catch (Exception e) {
                    EventLog.log(e, "Escrow", "confirm");
                }
                
                if (exists) {
                    String out = TRADE_EXIST.replaceAll("%user", user)
                                            .replaceAll("%rank", sender);
                    bot.sendIRCMessage(chan, out);
                } else if (amount != null && (amount > MINIMUM || amount < -MINIMUM)) {
                    DB db = DB.getInstance();
                    // positive means we are selling chips, negative buying them
                    String fromuser = sender;
                    String touser = user;
                    boolean commission = false;
                    if (amount < 0) {
                        fromuser = user;
                        touser = sender;
                        amount = -amount;
                        commission = true;
                    }
                    
                    double chips = 0;
                    try {
                        chips = db.checkCredits(fromuser, profile);
                    } catch (Exception e) {
                        EventLog.log(e, "Escrow", "sellChips");
                    }

                    //TODO: fix before launch
                    if (false == true/*bot.manualStatusRequest(user)*/) {
                        String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", user);
                        
                        bot.sendIRCMessage(senderu, out);
                    } else if (profile == null) {
                        bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                    } else if (chips < amount) {
                        String out = NOCHIPSMSG.replaceAll("%chips", Utils.chipsToString(amount));
                        out = out.replaceAll("%profile", profile.toString());
                        out = out.replaceAll("%user", user);
                        
                        bot.sendIRCMessage(chan, out);
                    } else {
                        boolean success = false;
                        try {
                            success = db.adjustChips(fromuser, -amount, profile,
                                                     GamesType.ADMIN, TransactionType.ESCROW);

                            db.addChipTransaction(fromuser, DB.HOUSE_USER,
                                                  -amount, TransactionType.ESCROW, profile);
                        } catch (Exception e) {
                            EventLog.log(e, "Escrow", "sellChips");
                        }

                        if (success) {
                            try {
                                db.addEscrow(fromuser, touser, amount, profile);
                                
                                String out = TRADE.replaceAll("%chips",
                                                              Utils.chipsToString(amount));
                                out = out.replaceAll("%user", user);
                                out = out.replaceAll("%rank", sender);
                                out = out.replaceAll("%profile", profile.toString());
                                
                                bot.sendIRCMessage(chan, out);
                                
                                addCommissionStatus(fromuser, touser, commission);
                            } catch (SQLException e) {
                                EventLog.log(e, "Escrow", "sellChips");
                            }
                        } else {
                            EventLog.log(sender + ":database failed on trade", "Escrow", "message");
                        }
                    }
                } else {
                    //TODO: change to invalid or less than minimum
                    bot.invalidArguments(senderu, TRADEFMT);
                }
            } else {
                bot.invalidArguments(senderu, TRADEFMT);
            }
        }
    }
    
    /**
     * Adds whether we give commission on this transaction or not.
     * 
     * @param fromuser  The from user.
     * @param touser    The to user.
     * @param comm      If commission is given or not.
     */
    private void addCommissionStatus(final String fromuser,
                                     final String touser,
                                     final boolean comm) {
        Map<String, Boolean> tolist = rankList.get(fromuser.toLowerCase());
        if (tolist == null) {
            tolist = new HashMap<String, Boolean>();
        }
        tolist.put(touser.toLowerCase(), comm);
        
        List<String> usrlist = confirmList.get(fromuser.toLowerCase());
        if (usrlist == null) {
            usrlist = new ArrayList<String>();
        }
        usrlist.add(touser.toLowerCase());
        confirmList.put(fromuser.toLowerCase(), usrlist);
    }
    
    /**
     * Retrieves if we give commission or not.
     * 
     * @param fromuser  The from user.
     * @param touser    The to user.
     * @return          If commission is given or not.
     */
    private boolean getCommissionStatus(final String fromuser, final String touser) {
        Map<String, Boolean> tolist = rankList.get(fromuser.toLowerCase());
        boolean res = false;
        if (tolist != null) {
            Boolean exists = tolist.get(touser.toLowerCase());
            if (exists != null) {
                res = exists;
            }
        }
        return res;
    }
}
