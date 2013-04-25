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

import org.smokinmils.Database;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;
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
	private static final String AnnounceLine = "%b%c12 There is a jackpot promotion running for all games! All bets contribute to the jackppot and all bets have a chance to win it, including poker hands. The current jackpot sizes are: %jackpots";
	public static final String JackpotAmount = "%c04%profile%c12(%c04%amount%c12)";
	
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
		String jackpotstr = "";
		int i = 0;
		for (ProfileType profile: ProfileType.values()) {
			int jackpot = 0;
			try {
				jackpot = Database.getInstance().getJackpot(profile); 
			} catch (Exception e) {
				EventLog.log(e, "Jackpots", "message");
			}
			
			jackpotstr += JackpotAmount.replaceAll("%profile",
							profile.toString()).replaceAll("%amount", Integer.toString(jackpot));
			if (i == (ProfileType.values().length - 2)) {
				jackpotstr += " and ";
			} else if (i < (ProfileType.values().length - 2)) {
				jackpotstr += ", ";
			}
			i++;
		}
		
		String out = AnnounceLine.replaceAll("%jackpots", jackpotstr);
		Bot.sendIRCMessage(Channel, out);
	}
}
