/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier.commands;

import org.pircbotx.Channel;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.BetterInfo;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to give a user some chips
 * 
 * @author Jamie
 */
public class CompPosition extends Event {
	public static final String Command = "!position";
	public static final String Description = "%b%c12Returns your position for this week's competitiong for all profiles or a single profile";
	public static final String Format = "%b%c12" + Command + " <profile> <user>";
	
	private static final String Position = "%b%c04%sender:%c12 %c04%who%c12 is currently in position %c04%position%c12 for the %c04%profile%c12 competition with %c04%chips%c12 chips bet";
	private static final String NotRanked = "%b%c04%sender:%c12 %c04%who%c12 is currently in %c04unranked%c12 for the %c04%profile%c12 competition";
	
	private static final String Last30Days = "%b%c04(%c12Last 30 days on the %c04%profile%c12 profile%c04)%c12 " +
	 										 "%c04%who%c12 highest bet was %c04%hb_chips%c12 on %c04%hb_game%c12 | " +
	 										 "%c04%who%c12 bet total is %c04%hbt_chips%c12";
	private static final String Last30Days_NoData = "%b%c04(%c12Last 30 days on the %c04%profile%c12 profile%c04)%c12 There is no data for %c04%who%c12 on this profile.";
	
	private static final String NoCompetition = "%b%c04%sender:%c12 There is no competition running for the %c04%profile%c12 profile";
	
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
			if (msg.length == 2 || msg.length == 3) {
				String who = (msg.length == 2 ? sender : msg[2] );
				ProfileType profile = ProfileType.fromString(msg[1]);
				if (profile == null) {
                    bot.sendIRCMessage(chan.getName(), IrcBot.ValidProfiles);
				} else if (!profile.hasComps()) {
                    String out = NoCompetition.replaceAll("%sender", sender);
                    out = out.replaceAll("%profile", profile.toString());
                    bot.sendIRCMessage(chan.getName(), out);
				} else {
					BetterInfo better = null;
					try {
						better = DB.getInstance().competitionPosition(profile, who);
					} catch (Exception e) {
						EventLog.log(e, "CompPosition", "message");
					}
					
					String out = "";
					if (better.Position == -1) {
						out = NotRanked.replaceAll("%profile", profile.toString());
					} else {
						out = Position.replaceAll("%profile", profile.toString());
						out = out.replaceAll("%position", Integer.toString(better.Position));
						out = out.replaceAll("%chips", Long.toString(better.Amount));
					}

					out = out.replaceAll("%sender", sender);
					out = out.replaceAll("%who", who);
					
					bot.sendIRCMessage(chan.getName(), out);
					
					DB db = DB.getInstance();
					for (ProfileType prof: ProfileType.values()) {
					    if (profile.hasComps()) {
    						try {
    							BetterInfo high_bet = db.getHighestBet(prof, sender);
    							BetterInfo top_better = db.getTopBetter(prof, sender);
    							
    							if (high_bet.User == null || top_better.User == null) {		
    								out = Last30Days_NoData;
    							} else {		
    								out = Last30Days.replaceAll("%hb_game", high_bet.Game.toString());
    								out = out.replaceAll("%hb_chips", Long.toString(high_bet.Amount));
    								out = out.replaceAll("%hbt_chips", Long.toString(top_better.Amount));
    			                    out = out.replaceAll("%who", high_bet.User);
    							}
    							out = out.replaceAll("%profile", prof.toString() );
    
    							bot.sendIRCNotice(sender, out);
    						} catch (Exception e) {
    							EventLog.log(e, "BetDetails", "run");
    						}
					    }
					}
				}
			} else {
				bot.invalidArguments( sender, Format );
			}
		}
	}
	
	
}
