/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier.rake;

import java.util.List;

import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for jackpot rakes.
 * 
 * @author Jamie
 */
public class Rake {	
	private static String JackpotChannel;
	private static final int JACKPOTCHANCE = 500;
	private static final double RAKE = 0.05;
	public static final boolean JackpotEnabled = false;
	
	private static final String JackpotWon = "%b%c01The %c04%profile%c01 jackpot of %c04%chips%c01 chips has been won in a %c04%game%c01 game! " +
			 "Congratulations to the winner(s):%c04 %winners %c01who have shared the jackpot";
	
	//private static final String JackpotIncreased = "%b%c01The %c04%profile%c01 jackpot is now %c04%chips%c01 chips! " +
	//		  "Every bet and poker hand has a chance to win.";

	//private static final String JackpotWonTable = "%b%c01Congratulations to %c04%winners%c01 who won %c04%chips%c01 each from the %c04%profile%c01 Jackpot";
    
	private static final String AnnounceLine = "%b%c01 There is a jackpot promotion running for all games! All bets contribute to the jackppot and all bets have a chance to win it, including poker hands. The current jackpot sizes are: %jackpots";
	public static final String JackpotAmount = "%c04%profile%c01(%c04%amount%c01) ";

 	/**
	* Initialise
	*/
	public void init(String chan) {
		JackpotChannel = chan;
 	}
	
   public static synchronized double getRake(String user, int bet, ProfileType profile) {
       double rake = RAKE * bet;
       Referal.addEvent(user, profile, rake);       
       return rake;
   }
   
   public static synchronized double getRake(String user, double rake, ProfileType profile) {
       Referal.addEvent(user, profile, rake);       
       return rake;
   }

	/**
	 * Check if the jackpot has been won
	 */
	public static synchronized boolean checkJackpot() {
	    // TODO: add code for JACKPOT Sundays
		return (JackpotEnabled ? (Random.nextInt(JACKPOTCHANCE + 1) == JACKPOTCHANCE) : false);
	}
	
	/**
	 * Jackpot has been won, split between all players on the table
	 */
	public static synchronized void jackpotWon(ProfileType profile, GamesType game,
								  			   List<String> players,
								  			   IrcBot bot, String channel) {
		try {
			DB db = DB.getInstance();
			int jackpot = (int)Math.floor(db.getJackpot(profile));

			if (jackpot > 0) {
				int remainder = jackpot % players.size();
				jackpot -= remainder;

				if (jackpot != 0) {
					int win = jackpot;// / players.size();
					for (String player : players) {
						db.jackpot(player, win, profile);
					}

					// Announce to channel
					String out = JackpotWon.replaceAll("%chips", Integer.toString(jackpot));
					out = out.replaceAll("%profile", profile.toString());
					out = out.replaceAll("%winners", players.toString());
					out = out.replaceAll("%game", game.toString());

					if (channel != null && !channel.equalsIgnoreCase(JackpotChannel)) {
						bot.sendIRCMessage(channel, out);
						bot.sendIRCMessage(channel, out);
						bot.sendIRCMessage(channel, out);
					}
				
					bot.sendIRCMessage(JackpotChannel, out);
					bot.sendIRCMessage(JackpotChannel, out);
					bot.sendIRCMessage(JackpotChannel, out);
					
					db.updateJackpot(profile, remainder);
				}
			}
		} catch (Exception e) {
			EventLog.log(e, "Jackpot", "updateJackpot");
		}
	}
	
	public static String getJackpotsString() {
       String jackpotstr = "";
       int i = 0;
       for (ProfileType profile: ProfileType.values()) {
           int jackpot = 0;
           try {
               jackpot = (int)Math.floor(DB.getInstance().getJackpot(profile)); 
           } catch (Exception e) {
               EventLog.log(e, "Jackpot", "getJackpotsString");
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
       return jackpotstr;
	}
	
	public static String getAnnounceString() {
	    String jackpotstr = Rake.getJackpotsString();
	    String out = AnnounceLine.replaceAll("%jackpots", jackpotstr);
	    return out;
	}
}
