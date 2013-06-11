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

/**
 * Provides the functionality to check a user's chips
 * 
 * @author Jamie
 */
public class Profiles extends Event {
	public static final String Command = "!profiles";
	public static final String Description = "%b%c12Lists the available profiles";
	public static final String Format = "%b%c12" + Command + "";
	
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
			bot.sendIRCMessage(chan, IrcBot.ValidProfiles);		
			bot.sendIRCNotice(sender, IrcBot.ValidProfiles);
		}
	}
}
