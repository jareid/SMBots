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

/**
 * Handles auto joining of the correct channels.
 *
 * @author Jamie
 */
public class AutoJoin extends TimerTask {
    /** Reference to the IrcBot this AutoJoiner is for. */
    private final IrcBot irc;

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
    @Override
    public final void run() {
        if (irc.isConnected()) {
            List<String> channels = irc.getValidChannels();
            for (String chan: channels) {
                irc.sendIRC().joinChannel(chan);
			}
		}
	}
}
