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

/**
 * Provides the functionality to list the valid profiles.
 * 
 * @author Jamie
 */
public class Profiles extends Event {
 
    /*@formatter:off*/
    /** The command. */
    public static final String        COMMAND           = "!profiles";
    
    /** The command description. */
    public static final String        DESCRIPTION       = 
            "%b%c12Lists the available profiles";

    /** The command format. */
    public static final String        FORMAT            = "%b%c12"
            + COMMAND + "";

    /** Changed profile message. */
    public static final String        PROFILECHANGED    = 
            "%b%c04%user %c12is now using the %c04%profile%c12 game profile";
    
    /** Failed to change profile message. */
    public static final String        PROFILECHANGEFAIL = 
            "%b%c04%user %c12tried to change to the %c04%profile%c12 game "
            + " profile and it failed. Please try again!";
    /*@formatter:on*/

    /**
     * This method handles the command.
     * 
     * @param event the Message event
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
            bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
            bot.sendIRCNotice(sender, IrcBot.VALID_PROFILES);
        }
    }
}
