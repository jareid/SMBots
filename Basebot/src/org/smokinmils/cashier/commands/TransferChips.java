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
import org.smokinmils.BaseBot;
import org.smokinmils.bot.CheckIdentified;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to transfer chips between users.
 * 
 * @author Jamie
 */
public class TransferChips extends Event {

    /** The command. */
    public static final String  COMMAND             = "!transfer";

    /** The command description. */
    public static final String  DESCRIPTION         = "%b%c12Transfer an "
                        + "amount of chips from a profile to another user";
    
    /** The command format. */
    public static final String  FORMAT              = "%b%c12" + COMMAND
                                                 + " <user> <amount> <profile>";

    /** The command length. */
    public static final int     CMD_LEN             = 4;

    /** The message when the user does not exist. */
    private static final String NO_USER             = "%b%c04%sender:%c12 "
                               + "%c04%who%c12 does not exist in the database";

    /** The transfer message for the channel. */
    private static final String TRANSFERRERD       = "%b%c04%sender%c12 has "
           + "transfered %c04%amount%c12 chips to the %c04%profile%c12 account"
           + " of %c04%who%c12";

    /** The transfer message for the receiver. */
    private static final String TRANSFERCHIPSUSER   = "%b%c12You have had "
                 + "%c04%amount%c12 chips transfered into your %c04%profile%c12"
                 + " account by %c04%sender%c12";

    /** The transfer message for the sender. */
    private static final String TRANSFERCHIPSSENDER = "%b%c12You have "
                + "transferred %c04%amount%c12 chips from your %c04%profile%c12"
                + " account to %c04%who%c12";

    /**
     * This method handles the command.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        synchronized (BaseBot.getLockObject()) {
            IrcBot bot = event.getBot();
            String message = event.getMessage();
            User senderu = event.getUser();
            String sender = senderu.getNick();
            Channel chan = event.getChannel();

            if (isValidChannel(chan.getName()) && bot.userIsIdentified(senderu)
                    && Utils.startsWith(message, COMMAND)) {
                String[] msg = message.split(" ");

                if (msg.length == CMD_LEN) {
                    String user = msg[1];
                    Integer amount = Utils.tryParse(msg[2]);
                    ProfileType profile = ProfileType
                            .fromString(msg[CMD_LEN - 1]);

                    if (!user.isEmpty() && !user.equals(sender)
                            && amount != null) {
                        // Check valid profile
                        if (!bot.userIsIdentified(user)
                                && bot.manualStatusRequest(user)) {
                            String out = CheckIdentified.NOT_IDENTIFIED
                                    .replaceAll("%user", user);
                            bot.sendIRCMessage(senderu, out);
                        } else if (profile == null) {
                            bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                        } else {
                            try {
                                int chips = DB.getInstance().checkCreditsAsInt(
                                        sender, profile);
                                if (amount > chips || amount < 0) {
                                    bot.noChips(senderu, amount, profile);
                                } else if (!DB.getInstance().checkUserExists(
                                        user)) {
                                    String out = NO_USER.replaceAll("%who",
                                            user);
                                    out = out.replaceAll("%sender", sender);
                                    bot.sendIRCMessage(chan, out);
                                } else {
                                    DB.getInstance().transferChips(sender,
                                            user, amount, profile);

                                    // Send message to channel
                                    String out = TRANSFERRERD.replaceAll(
                                            "%who", user);
                                    out = out.replaceAll("%sender", sender);
                                    out = out.replaceAll("%amount",
                                            Integer.toString(amount));
                                    out = out.replaceAll("%profile",
                                            profile.toString());
                                    bot.sendIRCMessage(chan, out);

                                    // Send notice to sender
                                    out = TRANSFERCHIPSUSER.replaceAll("%who",
                                            user);
                                    out = out.replaceAll("%sender", sender);
                                    out = out.replaceAll("%amount",
                                            Integer.toString(amount));
                                    out = out.replaceAll("%profile",
                                            profile.toString());

                                    User usr = bot.getUserChannelDao()
                                                                 .getUser(user);
                                    bot.sendIRCNotice(usr, out);

                                    // Send notice to user
                                    out = TRANSFERCHIPSSENDER.replaceAll(
                                            "%who", user);
                                    out = out.replaceAll("%sender", sender);
                                    out = out.replaceAll("%amount",
                                            Integer.toString(amount));
                                    out = out.replaceAll("%profile",
                                            profile.toString());
                                    bot.sendIRCNotice(senderu, out);
                                }
                            } catch (Exception e) {
                                EventLog.log(e, "TransferChips", "message");
                            }
                        }
                    } else {
                        bot.invalidArguments(senderu, FORMAT);
                    }
                } else {
                    bot.invalidArguments(senderu, FORMAT);
                }
            }
        }
    }
}
