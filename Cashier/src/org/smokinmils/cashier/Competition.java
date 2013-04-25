/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.smokinmils.Database;
import org.smokinmils.Utils;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.database.types.BetterInfo;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;
/**
 * Provides announcements about the competition for the top 5 total betters
 * 
 * @author Jamie
 */
public class Competition extends TimerTask {
	/** The output message for the statistics */
	private static final String AnnounceLine = "%b%c12[%c04Weekly Top Better Competition%c12] | [%c04%profile%c12] | Prizes: %c04%prizes %c12| Current leaders: %players | Time left: %c04%timeleft";
	private static final String UserLine = "%c04%who%c12(%c04%chips%c12)";

	private static final String WinnerAnnounceLine = "%b%c12[%c04Weekly Top Better Competition%c12] | [%c04%profile%c12] The weekly competition has ended! Congratulations to %players on their prizes";
	private static final String WinnerUserLine = "%c04%who%c12";
	private static final int NumberWinners = 5;
	
	
	private IrcBot Bot;
	private String Channel;
	
	/**
	 * Constructor
	 * 
	 * @param bot
	 */
	public Competition(IrcBot bot, String chan) {
		Bot = bot;
		Channel = chan;		
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		// check whether to announce or end
		boolean over = false;
		try {
			over = (Database.getInstance().getCompetitionTimeLeft() <= 0);
		} catch (Exception e) {
			EventLog.log(e, "Competition", "run");
		}
		
		if (over) {
			end();
		} else {
			announce();
		}
	}
	
	/**
	 * Just announce the current leaders
	 */
	private void announce() {
		Database db = Database.getInstance();
		Map<ProfileType, List<Integer>> all_prizes = readPrizes();
		for (ProfileType profile: ProfileType.values()) {
			try {
				List<BetterInfo> betters = db.getCompetition(profile, NumberWinners);
				int secs = db.getCompetitionTimeLeft();
				
				String duration = String.format("%%c04%d%%c12 day(s) %%c04%d%%c12 hour(s) %%c04%d%%c12 min(s)",
												secs/(60*60*24),
												(secs%(60*60*24))/(60*60),
												((secs%(60*60*24))%(60*60))/60);
				
				List<Integer> prizes = all_prizes.get(profile);
				// check there are enough prizes
				if (prizes == null || prizes.size() < betters.size()) {
					EventLog.log("Not enough prizes for " + profile.toString(), "Competition", "end");
					Bot.sendIRCMessage(Channel,"%b%c04No competition prizes set for + " + profile.toString() + ", please talk to an admin");
					continue;
				}
				
				String prizestr = "";
				int i = 0;
				for (Integer prize: prizes) {
					if (prize == null) prize = 0;
					prizestr += Integer.toString( prize );
					if (i == (betters.size() - 2)) {
						prizestr += " and ";
					} else if (i < (betters.size() - 2)) {
						prizestr += ", ";
					}
					i++;					
				}
				
				String out = AnnounceLine.replaceAll("%profile", profile.toString() );
				out = out.replaceAll("%timeleft", duration );
				out = out.replaceAll("%prizes", prizestr );
				String all_wins = "";
				
				i = 0;
				for (BetterInfo player: betters) {
					String winner = UserLine.replaceAll("%who", player.User);
					winner = winner.replaceAll("%chips", Long.toString(player.Amount));
					winner = winner.replaceAll("%position", Integer.toString(i+1));
					all_wins += winner;
					if (i == (betters.size() - 2)) {
						all_wins += " and ";
					} else if (i < (betters.size() - 2)) {
						all_wins += ", ";
					}
					i++;
				}
				
				out = out.replaceAll("%players", all_wins);
				
				Bot.sendIRCMessage(Channel, out);
			} catch (Exception e) {
				EventLog.log(e, "BetDetails", "run");
			}
		}
	}
	
	/**
	 * End the competition
	 */
	private void end() {
		Database db = Database.getInstance();
		Map<ProfileType, List<Integer>> all_prizes = readPrizes();
		for (ProfileType profile: ProfileType.values()) {
			try {
				List<BetterInfo> betters = db.getCompetition(profile, NumberWinners);
				
				String out = WinnerAnnounceLine.replaceAll("%profile", profile.toString() );
				String all_wins = "";
				
				List<Integer> prizes = all_prizes.get(profile);
				// check there are enough prizes
				if (prizes == null || prizes.size() < betters.size()) {
					EventLog.log("Not enough prizes for " + profile.toString(), "Competition", "end");
					Bot.sendIRCMessage(Channel,"%b%c04No competition prizes set for + " + profile.toString() + ", please talk to an admin");
					continue;
				}
				
				int i = 0;
				for (BetterInfo player: betters) {
					int prize = prizes.get(i);
					
					String winner = WinnerUserLine.replaceAll("%who", player.User);
					all_wins += winner;
					if (i == (betters.size() - 2)) {
						all_wins += " and ";
					} else if (i < (betters.size() - 2)) {
						all_wins += ", ";
					}
					
					db.giveChips(player.User, prize, profile);
					
					i++;
				}
				
				out = out.replaceAll("%players", all_wins);
				
				Bot.sendIRCMessage(Channel,out);
			} catch (Exception e) {
				EventLog.log(e, "Competition", "end");
			}
		}
		
		// Update the database for next competition
		try {
			db.competitionEnd();
		} catch (Exception e) {
			EventLog.log(e, "Competition", "end");
		}
	}
	
	private Map<ProfileType, List<Integer>> readPrizes() {
		Map<ProfileType, List<Integer>> results = new HashMap<ProfileType, List<Integer>>();
		for (ProfileType profile: ProfileType.values()) {
			List<Integer> prizes = new ArrayList<Integer>();
			// read the prizes from a file
			try {
				BufferedReader readFile = new BufferedReader(new FileReader("comp_prizes." + profile.toString()));
				String line = "";
				while ((line = readFile.readLine()) != null) {
					prizes.add( Utils.tryParse(line) );
				}
				readFile.close();
			} catch (IOException e) {
				EventLog.log(e, "Competition", "end");
				EventLog.log("No data for " + profile.toString(), "Competition", "end");
				Bot.sendIRCMessage(Channel,"%b%c04No competition prizes set for + " + profile.toString() + " please talk to an admin");
				continue;
			}
			results.put(profile, prizes);
		}
		return results;
	}
}