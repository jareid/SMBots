/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to change a user's profile.
 * 
 * @author Jamie
 */
public class Profile extends Event {

    /** The command. */
    public static final String COMMAND           = "!profile";

//@formatter:off
    /** The command description. */
    public static final String DESCR             = "%b%c12Changes the active "
                                                 + "profile for you";

    /** The command format. */
    public static final String FORMAT            = "%b%c12" + COMMAND
                                                            + " <profile>";
    /** The command length. */
    public static final int    CMD_LEN           = 2;

    /** Profile changed message. */
    public static final String PROFILE_CHANGED   = "%b%c04%user %c12is now " 
                                    + "using the %c04%profile%c12 game profile";
    
    /** Failed to change the profile for user message. */
    public static final String PROFILECHANGE_FAIL = "%b%c04%user %c12tried to"
                              + " change to the %c04%profile%c12 game profile "
                              + "and it failed. Please try again!";
//@formatter:on

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
        String chan = event.getChannel().getName();

        if (isValidChannel(event.getChannel().getName())
                && bot.userIsIdentified(sender)
                && Utils.startsWith(message, COMMAND)) {
            String[] msg = message.split(" ");
            if (msg.length == 2) {
                ProfileType profile = ProfileType.fromString(msg[1]);
                if (profile != null) {
                    boolean success = false;
                    try {
                        success = DB.getInstance().updateActiveProfile(sender,
                                profile);
                    } catch (Exception e) {
                        EventLog.log(e, "Profile", "message");
                    }
                    if (success) {
                        String out = PROFILE_CHANGED
                                .replaceAll("%user", sender);
                        out = out.replaceAll("%profile", profile.toString());
                        bot.sendIRCMessage(chan, out);
                    } else {
                        String out = PROFILECHANGE_FAIL.replaceAll("%user",
                                sender);
                        out = out.replaceAll("%profile", profile.toString());
                        bot.sendIRCMessage(chan, out);
                        EventLog.log(out, "Profile", "message");
                    }
                } else {
                    bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES.replaceAll(
                            "%profiles", ProfileType.values().toString()));
                }
            } else {
                bot.invalidArguments(sender, FORMAT);
            }
        }
    }
}
