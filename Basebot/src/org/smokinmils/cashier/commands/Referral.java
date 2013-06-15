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

import org.pircbotx.Channel;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ReferrerType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for public referrals.
 * 
 * @author Jamie
 */
public class Referral extends Event {

    /** The command. */
    public static final String  COMMAND = "!refer";

    /** The command format. */
    private static final String FORMAT  = "%b%c12" + COMMAND + " <user>";

    /** The command length. */
    public static final int     CMD_LEN = 2;

    /** Message when the user doesn't exist. */
    private static final String NO_USER = "%b%c04%sender%c12: " 
                                    + "%c04%who%c12 does not exist as a user.";

    /** Message when informing you can't self refer. */
    private static final String NO_SELF = "%b%c04%sender%c12: " 
                                        + "You can not be your own referrer.";
    
    /** Message when the referral is successful. */
    private static final String SUCCESS = "%b%c04%sender%c12: "
                            + "Succesfully added %c04%who%c12 as your referer.";

    /** Message when the referral failed. */
    private static final String FAILED  = "%b%c04%sender%c12: "
                                        + "You already have a referrer.";

    /**
     * This method handles the command.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        String sender = event.getUser().getNick();
        Channel chan = event.getChannel();

        if (Utils.startsWith(message, COMMAND) && bot.userIsIdentified(sender)
                && isValidChannel(chan.getName())) {
            try {
                refer(event);
            } catch (Exception e) {
                EventLog.log(e, "Referral", "message");
            }
        }
    }

    /**
     * Handles the referral command.
     * 
     * @param event the message event
     * 
     * @throws SQLException on a database error
     */
    private void refer(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();

        if (msg.length == 2) {
            DB db = DB.getInstance();
            ReferrerType reftype = db.getRefererType(sender);
            if (reftype == ReferrerType.NONE) {
                String referrer = msg[1];
                if (referrer.equalsIgnoreCase(sender)) {
                    String out = NO_SELF.replaceAll("%sender", sender);
                    bot.sendIRCMessage(channel, out);
                } else if (!db.checkUserExists(referrer)) {
                    String out = NO_USER.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", referrer);
                    bot.sendIRCMessage(channel, out);
                } else {
                    db.addReferer(sender, referrer);
                    String out = SUCCESS.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", referrer);
                    bot.sendIRCMessage(channel, out);
                }
            } else {
                String out = FAILED.replaceAll("%sender", sender);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(sender, FORMAT);
        }
    }
}
