/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier;

import java.sql.SQLException;

import org.pircbotx.Channel;
import org.smokinmils.Database;
import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DBException;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to give a user some chips
 * 
 * @author Jamie
 */
public class Lottery extends Event {
	public static final String Command = "!ticket";
	public static final String Description = "%b%c12Buys a quantity of lottery tickets for the active profile";
	public static final String Format = "%b%c12" + Command + " <quantity>";
	
	private static final String BoughtTickets = "%b%c12The %c04%profile%c12 Weekly Lottery is now at a total of %c04%amount%c12 chips! It's 1 chip per ticket, %c04%percent%%c12 of the pot is paid out. Time to draw: %c04%timeleft%c12. To buy 1 ticket with your active profile type %c04%cmd 1";
	private static final String LotteryEnded = "%b%c12The %c04%profile%c12 Weekly Lottery has now ended! This week's winner was %c04%winner%c12 and they won %c04%amount%c12 chips!";
	private static final String Reset = "%b%c12A new weekly lottery has begun! It's 1 chip per ticket, %c04%percent%%c12 of the pot is paid out. Time to draw: %c04%timeleft%c12. To buy 1 ticket with your active profile type %c04%cmd 1";

	private static final int LotteryPercent = 90;
	
	/**
	 * This method handles the chips command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */
	@Override
	public void message(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		Channel chan = event.getChannel();
		
		if ( isValidChannel( chan.getName() ) &&
				bot.userIsIdentified( sender ) &&
				message.toLowerCase().startsWith( Command ) ) {			
			String[] msg = message.split(" ");
			if (msg.length == 2) {
				Integer amount = Utils.tryParse(msg[1]);
				
				if (amount != null && amount > 0) {						
					try {
						Database db = Database.getInstance();
						int chips = db.checkCredits( sender );
						ProfileType profile = db.getActiveProfile(sender);
						if ( amount > chips ) {
							bot.NoChips(sender, amount, profile);
						} else {
							boolean res = db.buyLotteryTickets(sender, profile, amount);
							if (res) {
								// TODO notice user with buy message
								announceLottery(bot, profile, chan.getName());
							} else {
								EventLog.log("Failed to buy lottery tickets for " + sender, "Lottery", "message");
							}								
						}
					} catch (Exception e) {
						EventLog.log(e, "TransferChips", "message");
					}
				} else {
					bot.invalidArguments( sender, Format );
				}
			} else {
				bot.invalidArguments( sender, Format );
			}
		}
	}
	
	public static void announceLottery(IrcBot bot, ProfileType profile, String channel)
			throws DBException, SQLException {
		Database db = Database.getInstance();
		int secs = db.getCompetitionTimeLeft();
		int amount = db.getLotteryTickets(profile);
		
		String duration = String.format("%%c04%d%%c12 day(s) %%c04%d%%c12 hour(s) %%c04%d%%c12 min(s)",
				secs/(60*60*24),
				(secs%(60*60*24))/(60*60),
				((secs%(60*60*24))%(60*60))/60);
	
		String out = BoughtTickets.replaceAll("%profile", profile.toString() );
		out = out.replaceAll("%timeleft", duration );
		out = out.replaceAll("%amount", Integer.toString(amount) );
		out = out.replaceAll("%percent", Integer.toString(LotteryPercent) );
		out = out.replaceAll("%cmd", Command );
		
		bot.sendIRCMessage(channel, out);
	}
	
	public static void endLottery(IrcBot bot, String channel) {
		try {
			Database db = Database.getInstance();
			for (ProfileType profile: ProfileType.values()) {				
				int amount = db.getLotteryTickets(profile);
				if (amount > 0) {
					amount = (int) Math.round(amount * (LotteryPercent * 0.01));
					
					String winner = db.getLotteryWinner(profile);
					
					db.adjustChips(winner, amount, profile, GamesType.LOTTERY, TransactionType.LOTTERY_WIN);
					
					String out = LotteryEnded.replaceAll("%profile", profile.toString() );
					out = out.replaceAll("%winner", winner );
					out = out.replaceAll("%amount", Integer.toString(amount) );
					bot.sendIRCMessage(channel, out);
				}
				// TODO: announce no winners as no chips
			}
			
			// Start next week's lottery.
			db.endLottery();
		} catch (Exception e) {
			EventLog.log(e, "Lottery", "endLottery");
		}
	}
	
	public static void announceReset(IrcBot bot, String channel)
			throws DBException, SQLException {
		int secs = Database.getInstance().getCompetitionTimeLeft();
		
		String duration = String.format("%%c04%d%%c12 day(s) %%c04%d%%c12 hour(s) %%c04%d%%c12 min(s)",
				secs/(60*60*24),
				(secs%(60*60*24))/(60*60),
				((secs%(60*60*24))%(60*60))/60);
		
		String out = Reset.replaceAll("%timeleft", duration );
		out = out.replaceAll("%percent", Integer.toString(LotteryPercent) );
		out = out.replaceAll("%cmd", Command );

		bot.sendIRCMessage(channel, out);
	}
}