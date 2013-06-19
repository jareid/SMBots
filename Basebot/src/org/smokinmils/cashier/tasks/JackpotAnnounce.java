/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.tasks;

import java.util.TimerTask;

import org.pircbotx.Channel;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.cashier.rake.Rake;

/**
 * Provides announcements about the betting on an irc server
 * 
 * Should be scheduled as a regular repeating task or timer should be cancelled.
 * This in no way considers the timer or trys to cancel it.
 * 
 * @author Jamie
 */
public class JackpotAnnounce extends TimerTask {
    /** The irc bot used to announce. */
    private final IrcBot bot;

    /** The channel to announce to. */
    private final String channel;

    /**
     * Constructor.
     * 
     * @param irc The irc bot
     * @param chan The announce channel
     */
    public JackpotAnnounce(final IrcBot irc, final String chan) {
        bot = irc;
        channel = chan;
    }

    /**
     * (non-Javadoc).
     * @see java.util.TimerTask#run()
     */
    @Override
    public final void run() {
        String out = "%b%c01The jackpot raises by %c041%c01 chip for every "
                   + "%c04200%c01 chips bet on all games. You have a "
                   + "%c040.001%%c01 chance to win per chip that you bet (so "
                   + "%c041%%c01 chance per 1000%c01 chips). To see the current"
                   + " jackpot sizes use %c04!jackpots%c01.";
        if (Rake.JACKPOTENABLED) {
            Channel chan = bot.getUserChannelDao().getChannel(channel);
            bot.sendIRCMessage(chan, out);
        }
    }
}
