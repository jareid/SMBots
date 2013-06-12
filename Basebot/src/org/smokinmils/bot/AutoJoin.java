/**
 * This file is part of a commercial IRC bot that
 * allows users to play online IRC games.
 *
 * The project was commissioned by Julian Clark
 *
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.bot;

import java.util.List;
import java.util.TimerTask;

import org.pircbotx.Channel;

/**
 * Handles auto joining of the correct channels.
 *
 * @author Jamie
 */
public class AutoJoin extends TimerTask {
    /** Reference to the IrcBot this AutoJoiner is for. */
    private IrcBot irc;

    /**
     * Constructor.
     *
     * @param bot The IRC bot for this server's autojoin
     */
    public AutoJoin(final IrcBot bot) {
        irc = bot;
    }

    /**
     * (non-Javadoc).
     * @see java.util.TimerTask#run()
     */
    public final void run() {
        if (irc.isConnected()) {
            List<String> channels = irc.getValidChannels();
            for (String chan: channels) {
                boolean found = false;
                for (Channel allchan :irc.getChannels()) {
					if (chan.equalsIgnoreCase(allchan.getName())) {
					    found = true;
					}
				}

				if (!found) {
				    irc.joinChannel(chan);
				}
			}
		}
	}
}
