/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier.commands;

import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to check a user's chips
 * 
 * @author Jamie
 */
public class Profile extends Event {
	public static final String Command = "!profile";
	public static final String Description = "%b%c12Changes the active profile for you";
	public static final String Format = "%b%c12" + Command + " <profile>";
	
	public static final String ProfileChanged = "%b%c04%user %c12is now using the %c04%profile%c12 game profile";
	public static final String ProfileChangeFail = "%b%c04%user %c12tried to change to the %c04%profile%c12 game profile and it failed. Please try again!";
	
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
		String chan = event.getChannel().getName();
		
		if ( isValidChannel( event.getChannel().getName() ) &&
				bot.userIsIdentified( sender ) &&
				Utils.startsWith(message, Command ) ) {			
			String[] msg = message.split(" ");
			if (msg.length == 2) {
				ProfileType profile = ProfileType.fromString(msg[1]);
				if (profile != null) {
					boolean success = false;
					try {
						success = DB.getInstance().updateActiveProfile(sender,profile);
					} catch (Exception e) {
						EventLog.log(e, "Profile", "message");
					}
					if (success) {
						String out = ProfileChanged.replaceAll("%user", sender);
						out = out.replaceAll("%profile", profile.toString());
						bot.sendIRCMessage(chan, out);
					} else {
						String out = ProfileChangeFail.replaceAll("%user", sender);
						out = out.replaceAll("%profile", profile.toString());
						bot.sendIRCMessage(chan, out);
						EventLog.log(out, "Profile", "message");
					}				
				} else {
					bot.sendIRCMessage( chan,
							IrcBot.ValidProfiles.replaceAll("%profiles", ProfileType.values().toString()) );
				}
			} else {
				bot.invalidArguments( sender, Format );
			}			
		}
	}
}
