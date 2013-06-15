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
        if (Rake.JACKPOTENABLED) {
            bot.sendIRCMessage(channel, Rake.getAnnounceString());
        }
    }
}
