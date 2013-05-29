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
	private IrcBot Irc;
	
	public AutoJoin (IrcBot bot) {
		Irc = bot;
	}
	
	public void run() {
		if ( Irc.isConnected() ) {
			List<String> channels = Irc.getValidChannels();
			for (String chan: channels) {
				boolean found = false;
				for (Channel allchan :Irc.getChannels()) {
					if (chan.equalsIgnoreCase(allchan.getName())) {
					    found = true;
					}
				}
				
				if (!found) {
				    Irc.joinChannel(chan);
				}
			}
		}
	}
}
