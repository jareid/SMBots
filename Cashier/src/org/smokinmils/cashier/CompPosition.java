/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier;

import org.pircbotx.Channel;
import org.smokinmils.Database;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
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
				message.startsWith( Command ) ) {			
			String[] msg = message.split(" ");
			if (msg.length == 2 || msg.length == 3) {
				String who = (msg.length == 2 ? sender : msg[2] );
				ProfileType profile = ProfileType.fromString(msg[1]);
				if (profile != null) {
					BetterInfo better = null;
					try {
						better = Database.getInstance().competitionPosition(profile, who);
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
				} else {
					bot.sendIRCMessage(chan.getName(), IrcBot.ValidProfiles);
				}
			} else {
				bot.invalidArguments( sender, Format );
			}
		}
	}
	
	
}
