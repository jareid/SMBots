/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier;

import java.util.TimerTask;

import org.smokinmils.bot.IrcBot;
/**
 * Provides announcements about the betting on an irc server
 * 
 * Should be scheduled as a regular repeating task or timer should be cancelled.
 * This in no way considers the timer or trys to cancel it.
 * 
 * @author Jamie
 */
public class JackpotAnnounce extends TimerTask {
	/** The output message for the statistics */

	
	private IrcBot Bot;
	private String Channel;
	
	/**
	 * Constructor
	 * 
	 * @param bot
	 */
	public JackpotAnnounce(IrcBot bot, String chan) {
		Bot = bot;
		Channel = chan;
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		Bot.sendIRCMessage(Channel, Rake.getAnnounceString());
	}
}
