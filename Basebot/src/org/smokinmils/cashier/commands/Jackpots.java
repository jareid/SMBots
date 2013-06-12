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
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.rake.Rake;

/**
 * Provides the functionality to output jack pots.
 * 
 * @author Jamie
 */
public class Jackpots extends Event {

    /** The command. */
    public static final String COMMAND   = "!jackpots";

//@formatter:off
    /** The command description. */
    public static final String DESC      = "b%c12Lists all the jackpot totals "
         + "for each profile. Each poker hand and bet has a chance of winning!";

    /** The command format. */
    public static final String FORMAT    = "%b%c12" + COMMAND + "";

    /** The jackpot announce string. */
    public static final String INFO_LINE = "%b%c12The current jackpot sizes "
                       + "are: %jackpots. Every poker hand and bet has a chance"
                       + " to win the jackpot.";
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
        Channel chan = event.getChannel();

        if (isValidChannel(chan.getName()) && bot.userIsIdentified(sender)
                && Utils.startsWith(message, COMMAND)) {
            bot.sendIRCMessage(chan, Rake.getAnnounceString());
        }
    }
}
