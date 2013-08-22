/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

import java.io.IOException;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.CheckIdentified;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.external.HTTPPoster;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to payout a user some chips.
 * 
 * @author Jamie
 */
public class Coins extends Event {
    /** The payout command. */
    public static final String  PAYCMD       = "!payout";

    /** The payout command format. */
    public static final String  PAYFMT        = "%b%c12" + PAYCMD + " <user> <amount> <profile>";
    
    /** The payout command length. */
    public static final int     PAY_CMD_LEN     = 4;
    
    /** The give command. */
    public static final String  GIVECMD     = "!coins";

    /** The give command format. */
    public static final String  GIVEFMT      = "%b%c12" + GIVECMD + " <user> <amount> <profile>";

    /** The give command length. */
    public static final int     GIVE_CMD_LEN     = 4;

    /** The message to the channel on success. */
    private static final String PAYOUTCHIPS   = "%b%c04%sender:%c12 Paid out %c04%amount%c12 coins "
                                              + "from the %c04%profile%c12 account of %c04%who%c12";
    
    /** The message to the user. */
    private static final String PAYOUTCHIPSPM = "%b%c12You have had %c04%amount%c12 coins " 
                                              + "paid out from your account by %c04%sender%c12";
    
    /** Message when the user doesn't have enough chips. */
    public static final String  NOCHIPSMSG    = "%b%c12Sorry, %c04%user%c12 does not have "
                                              + "%c04%coins%c12 coins available for the "
                                              + "%c04%profile%c12 profile.";

    /** The message sent to the channel on success. */
    private static final String GIVECHIPS   = "%b%c04%sender:%c12 Added "
      + "%c04%amount%c12 coins to the %c04%profile%c12 account of %c04%who%c12";
    
    /** Message sent to the user on success. */
    private static final String GIVECHIPSPM = "%b%c12You have had "
      + "%c04%amount%c12 coins deposited into your account by %c04%sender%c12";

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
            if (Utils.startsWith(message, GIVECMD)) {
                giveChips(event);
            } else if (Utils.startsWith(message, PAYCMD)) {
                payoutChips(event);
            }
        }
    }
    
    /**
     * This method handles the chips command.
     * 
     * @param event the Message event
     */
    public final void giveChips(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");

        if (bot.userIsOp(event.getUser(), chan.getName())) {
            if (msg.length == GIVE_CMD_LEN) {
                String user = msg[1];
                Double amount = Utils.tryParseDbl(msg[GIVE_CMD_LEN - 2]);
                ProfileType profile = ProfileType.fromString(msg[GIVE_CMD_LEN - 1]);

                if (amount != null && amount > 0.0) {
                    // Check valid profile
                    if (bot.manualStatusRequest(user)) {
                        String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", user);
                        bot.sendIRCMessage(sender, out);
                    } else if (profile != null) {
                        boolean success = false;
                        try {
                            DB db = DB.getInstance();
                            success = db.adjustChips(user, amount, profile,
                                                     GamesType.ADMIN, TransactionType.CREDIT);

                            db.addChipTransaction(user, sender.getNick(),
                                                  amount, TransactionType.CREDIT, profile);  
                        } catch (Exception e) {
                            EventLog.log(e, "GiveChips", "message");
                        }

                        if (success) {
                            String out = GIVECHIPS.replaceAll("%amount", 
                                                              Utils.chipsToString(amount));
                            out = out.replaceAll("%who", user);
                            out = out.replaceAll("%sender", sender.getNick());
                            out = out.replaceAll("%profile", profile.toString());
                            
                            bot.sendIRCMessage(chan, out);

                            out = GIVECHIPSPM.replaceAll("%amount", Utils.chipsToString(amount));
                            out = out.replaceAll("%who", user);
                            out = out.replaceAll("%sender", sender.getNick());
                            out = out.replaceAll("%profile", profile.toString());
                            
                            User usr = bot.getUserChannelDao().getUser(user);
                            bot.sendIRCNotice(usr, out);
                            
                            try {
                                if (!profile.equals(ProfileType.PLAY)) {
                                    HTTPPoster h = new HTTPPoster();
                                    h.sendGoldChange(sender.getNick(), amount, profile);
                                }
                            } catch (IOException e) {
                                EventLog.log(e, "Coins", "giveChips");
                                
                            }
                            
                        } else {
                            EventLog.log(sender.getNick() + " attempted to give someone coins and "
                                         + "the database failed", "GiveChips", "message");
                        }
                    } else {
                        bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                    }
                } else {
                    bot.invalidArguments(sender, GIVEFMT);
                }
            } else {
                bot.invalidArguments(sender, GIVEFMT);
            }
        } else {
            EventLog.info(sender + " attempted to give someone coins",  "GiveChips", "message");
        }
    }

    /**
     * This method handles the payout command.
     * 
     * @param event the message event.
     */
    public final void payoutChips(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");

        if (bot.userIsOp(event.getUser(), chan.getName())) {
            if (msg.length == PAY_CMD_LEN) {
                String user = msg[1];
                Double amount = Utils.tryParseDbl(msg[2]);
                ProfileType profile = ProfileType
                        .fromString(msg[PAY_CMD_LEN - 1]);

                if (amount != null && amount > 0) {
                    double chips = 0;
                    try {
                        chips = DB.getInstance().checkCredits(user, profile);
                    } catch (Exception e) {
                        EventLog.log(e, "Payout", "message");
                    }

                    if (bot.manualStatusRequest(user)) {
                        String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", user);
                        
                        bot.sendIRCMessage(senderu, out);
                    } else if (profile == null) {
                        bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                    } else if (chips < amount) {
                        String out = NOCHIPSMSG.replaceAll("%coins", Utils.chipsToString(amount));
                        out = out.replaceAll("%profile", profile.toString());
                        out = out.replaceAll("%user", user);
                        
                        bot.sendIRCMessage(chan, out);
                    } else {
                        boolean success = false;
                        try {                                
                            DB db = DB.getInstance();
                            success = db.adjustChips(user, -amount, profile,
                                                     GamesType.ADMIN, TransactionType.PAYOUT);

                            db.addChipTransaction(user, sender, amount, 
                                                  TransactionType.PAYOUT, profile);  
                        } catch (Exception e) {
                            EventLog.log(e, "Payout", "message");
                        }

                        if (success) {
                            String out = PAYOUTCHIPS.replaceAll("%amount",
                                    Utils.chipsToString(amount));
                            out = out.replaceAll("%who", user);
                            out = out.replaceAll("%sender", sender);
                            out = out.replaceAll("%profile", profile.toString());
                            
                            bot.sendIRCMessage(chan, out);

                            out = PAYOUTCHIPSPM.replaceAll("%amount",
                                    Utils.chipsToString(amount));
                            out = out.replaceAll("%who", user);
                            out = out.replaceAll("%sender", sender);
                            out = out.replaceAll("%profile", profile.toString());

                            User usr = bot.getUserChannelDao().getUser(user);
                            bot.sendIRCNotice(usr, out);
                            
                           
                            try {
                                if (!profile.equals(ProfileType.PLAY)) {
                                    HTTPPoster h = new HTTPPoster();
                                    h.sendGoldChange(sender, -amount, profile);
                                }
                            } catch (IOException e) {
                                EventLog.log(e, "Coins", "payoutChips");
                                
                            }
                        } else {
                            EventLog.log(sender + "database failed", "Payout", "message");
                        }
                    }
                } else {
                    bot.invalidArguments(senderu, PAYFMT);
                }
            } else {
                bot.invalidArguments(senderu, PAYFMT);
            }
        } else {
            EventLog.info(sender + " attempted to pay out someone coins", "Payout", "message");
        }
    }
}
