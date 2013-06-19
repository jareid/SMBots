/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

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
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to payout a user some chips.
 * 
 * @author Jamie
 */
public class Payout extends Event {
    /** The command. */
    public static final String  COMMAND       = "!payout";
    
    /** The command description. */
    public static final String  DESC          = "%b%c12Payout a number of chips"
                                              + "from a players profile";

    /** The command format. */
    public static final String  FORMAT        = "%b%c12" + COMMAND
                                              + " <user> <amount> <profile>";
    
    /** The command length. */
    public static final int     CMD_LEN     = 4;

    /** The message to the channel on success. */
    private static final String PAYOUTCHIPS   = "%b%c04%sender:%c12 Paid out "
                  + "%c04%amount%c12 chips from the %c04%profile%c12 account of"
                  + " %c04%who%c12";
    
    /** The message to the user. */
    private static final String PAYOUTCHIPSPM = "%b%c12You have had " 
                        + "%c04%amount%c12 chips paid out from your account by "
                        + "%c04%sender%c12";
    
    /** Message when the user doesn't have enough chips. */
    public static final String  NOCHIPSMSG    = "%b%c12Sorry, %c04%user%c12 "
                            + "does not have %c04%chips%c12 chips available for"
                            + "the %c04%profile%c12 profile.";

    /**
     * This method handles the command.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        if (isValidChannel(chan.getName()) && bot.userIsIdentified(senderu)
                && Utils.startsWith(message, COMMAND)) {
            String[] msg = message.split(" ");

            if (bot.userIsHalfOp(event.getUser(), chan.getName()) 
                 || bot.userIsOp(event.getUser(), chan.getName())) {
                if (msg.length == CMD_LEN) {
                    String user = msg[1];
                    Double amount = Utils.tryParseDbl(msg[2]);
                    ProfileType profile = ProfileType
                            .fromString(msg[CMD_LEN - 1]);

                    if (amount != null && amount > 0) {
                        double chips = 0;
                        try {
                            chips = DB.getInstance().checkCredits(user,
                                                                  profile);
                        } catch (Exception e) {
                            EventLog.log(e, "Payout", "message");
                        }

                        if (!bot.userIsIdentified(user)
                                && bot.manualStatusRequest(user)) {
                            String out = CheckIdentified.NOT_IDENTIFIED
                                                     .replaceAll("%user", user);
                            
                            bot.sendIRCMessage(sender, out);
                        } else if (profile == null) {
                            bot.sendIRCMessage(chan.getName(),
                                                IrcBot.VALID_PROFILES);
                        } else if (chips < amount) {
                            String out = NOCHIPSMSG.replaceAll("%chips",
                                    Utils.chipsToString(amount));
                            out = out
                                    .replaceAll("%profile", profile.toString());
                            out = out.replaceAll("%user", user);
                            
                            bot.sendIRCMessage(chan, out);
                        } else {
                            boolean success = false;
                            try {
                                success = DB.getInstance().adjustChips(user,
                                        -amount, profile, GamesType.ADMIN,
                                        TransactionType.ADMIN);
                            } catch (Exception e) {
                                EventLog.log(e, "Payout", "message");
                            }

                            if (success) {
                                String out = PAYOUTCHIPS.replaceAll("%amount",
                                        Utils.chipsToString(amount));
                                out = out.replaceAll("%who", user);
                                out = out.replaceAll("%sender", sender);
                                out = out.replaceAll("%profile",
                                        profile.toString());
                                
                                bot.sendIRCMessage(chan, out);

                                out = PAYOUTCHIPSPM.replaceAll("%amount",
                                        Utils.chipsToString(amount));
                                out = out.replaceAll("%who", user);
                                out = out.replaceAll("%sender", sender);
                                out = out.replaceAll("%profile",
                                        profile.toString());
                                
                                bot.sendIRCNotice(user, out);
                            } else {
                                EventLog.log(sender + "database failed",
                                        "Payout", "message");
                            }
                        }
                    } else {
                        bot.invalidArguments(senderu, FORMAT);
                    }
                } else {
                    bot.invalidArguments(senderu, FORMAT);
                }
            } else {
                EventLog.info(sender + " attempted to pay out someone chips",
                        "Payout", "message");
            }
        }
    }
}
