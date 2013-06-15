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
 * Provides the functionality to give a user some chips.
 * 
 * @author Jamie
 */
public class GiveChips extends Event {
    /** The command. */
    public static final String  COMMAND     = "!chips";

    /** The command description. */
    public static final String  DESC        = "%b%c12Give a user a number of "
                                            + "chips to a certain game profile";

    /** The command format. */
    public static final String  FORMAT      = "%b%c12" + COMMAND
                                            + " <user> <amount> <profile>";

    /** The command length. */
    public static final int     CMD_LEN     = 4;

    /** The message sent to the channel on success. */
    private static final String GIVECHIPS   = "%b%c04%sender:%c12 Added "
      + "%c04%amount%c12 chips to the %c04%profile%c12 account of %c04%who%c12";
    
    /** Message sent to the user on success. */
    private static final String GIVECHIPSPM = "%b%c12You have had "
      + "%c04%amount%c12 chips deposited into your account by %c04%sender%c12";

    /**
     * This method handles the chips command.
     * 
     * @param event the Message event
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        String sender = event.getUser().getNick();
        Channel chan = event.getChannel();

        if (isValidChannel(chan.getName()) && bot.userIsIdentified(sender)
                && Utils.startsWith(message, COMMAND)) {
            String[] msg = message.split(" ");

            if (bot.userIsHalfOp(event.getUser(), chan.getName())) {
                if (msg.length == CMD_LEN) {
                    String user = msg[1];
                    Double amount = Utils.tryParseDbl(msg[CMD_LEN - 2]);
                    ProfileType profile = ProfileType
                            .fromString(msg[CMD_LEN - 1]);

                    if (amount != null && amount > 0) {
                        // Check valid profile
                        if (!bot.userIsIdentified(user)) {
                            String out = CheckIdentified.NOT_IDENTIFIED
                                    .replaceAll("%user", user);
                            bot.sendIRCMessage(sender, out);
                        } else if (profile != null) {
                            boolean success = false;
                            try {
                                success = DB.getInstance().adjustChips(user,
                                        amount, profile, GamesType.ADMIN,
                                        TransactionType.ADMIN);
                            } catch (Exception e) {
                                EventLog.log(e, "GiveChips", "message");
                            }

                            if (success) {
                                String out = GIVECHIPS.replaceAll("%amount",
                                        Utils.chipsToString(amount));
                                out = out.replaceAll("%who", user);
                                out = out.replaceAll("%sender", sender);
                                out = out.replaceAll("%profile",
                                        profile.toString());
                                bot.sendIRCMessage(chan.getName(), out);

                                out = GIVECHIPSPM.replaceAll("%amount",
                                        Utils.chipsToString(amount));
                                out = out.replaceAll("%who", user);
                                out = out.replaceAll("%sender", sender);
                                out = out.replaceAll("%profile",
                                        profile.toString());
                                bot.sendIRCNotice(user, out);
                            } else {
                                EventLog.log(sender + " attempted to give "
                                        + "someone chips and the database "
                                        + "failed", "GiveChips", "message");
                            }
                        } else {
                            bot.sendIRCMessage(chan.getName(),
                                    IrcBot.VALID_PROFILES);
                        }
                    } else {
                        bot.invalidArguments(sender, FORMAT);
                    }
                } else {
                    bot.invalidArguments(sender, FORMAT);
                }
            } else {
                EventLog.info(sender + " attempted to give someone chips",
                        "GiveChips", "message");
            }
        }
    }
}
