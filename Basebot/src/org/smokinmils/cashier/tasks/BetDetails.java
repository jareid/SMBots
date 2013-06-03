/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier.tasks;

import java.util.TimerTask;

import org.smokinmils.bot.IrcBot;
import org.smokinmils.database.DB;
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
	private static final String AnnounceLine = "%b%c04(%c01Last 30 days on the %c04%profile%c01 profile%c04)%c01 " +
										 		"Highest bet made by %c04%hb_user%c01 with %c04%hb_chips%c01 on %c04%hb_game%c01 | " +
										 		"%c01 Highest bet total was %c04%hbt_user%c01 with %c04%hbt_chips%c01";

	private IrcBot Bot;
	private String Channel;
	
	/**
	 * Constructor
	 * 
	 * @param bot
	 */
	public BetDetails(IrcBot bot, String chan) {
		Bot = bot;
		Channel = chan;
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		DB db = DB.getInstance();
		for (ProfileType profile: ProfileType.values()) {
		    if (profile.hasComps()) {
    			try {
    				BetterInfo high_bet = db.getHighestBet(profile);
    				BetterInfo top_better = db.getTopBetter(profile);
    				
    				String out = AnnounceLine.replaceAll("%profile", profile.toString() );
    				out = out.replaceAll("%hb_user", high_bet.User);
    				out = out.replaceAll("%hb_game", high_bet.Game.toString());
    				out = out.replaceAll("%hb_chips", Long.toString(high_bet.Amount));
    				out = out.replaceAll("%hbt_user", top_better.User);
    				out = out.replaceAll("%hbt_chips", Long.toString(top_better.Amount));
    				
    				Bot.sendIRCMessage(Channel, out);
    			} catch (Exception e) {
    				EventLog.log(e, "BetDetails", "run");
    			}
		    }
		}
	}
}
