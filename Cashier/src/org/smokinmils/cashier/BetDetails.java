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
import org.smokinmils.database.types.BetterInfo;
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
public class BetDetails extends TimerTask {
	/** The output message for the statistics */
	private static final String AnnounceLine = "%b%c04(%c12Last 30 days on the %c04%profile%c12 profile%c04)%c12 " +
										 		"Highest bet made by %c04%hb_user%c12 with %c04%hb_chips%c12 on %c04%hb_game%c12| " +
										 		"%c12 Highest bet total was %c04%hbt_user%c12 with %c04%hbt_chips%c12";
	
	/** A reference to the IrcBot for this class */
	private IrcBot Bot;
	
	/** The channel to announce in */
	private String Channel;
	
	/**
	 * Constructor
	 * 
	 * @param bot
	 */
	public BetDetails(IrcBot bot, String channel) {
		Bot = bot;
		Channel = channel;
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		Database db = Database.getInstance();
		for (ProfileType profile: ProfileType.values()) {
			try {
				BetterInfo high_bet = db.getHighestBet(profile);
				BetterInfo top_better = db.getTopBetter(profile);
				
				String out = AnnounceLine.replaceAll("%profile", profile.toString() );
				out.replaceAll("%hb_user", high_bet.User);
				out.replaceAll("%hb_chips", Integer.toString(high_bet.Amount));
				out.replaceAll("%hbt_user", top_better.User);
				out.replaceAll("%hbt_chips", Integer.toString(top_better.Amount));
				
				Bot.sendIRCMessage(Channel, out);
			} catch (Exception e) {
				EventLog.log(e, "BetDetails", "run");
			}
		}
	}
}
