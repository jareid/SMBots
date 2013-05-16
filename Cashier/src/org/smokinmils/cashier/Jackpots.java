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
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;

/**
 * Provides the functionality to give a user some chips
 * 
 * @author Jamie
 */
public class Jackpots extends Event {
	public static final String Command = "!jackpots";
	public static final String Description = "b%c12Lists all the jackpot totals for each profile. Each poker hand and bet has a chance of winning!";
	public static final String Format = "%b%c12" + Command + "";
	
	public static final String JackpotInfo = "%b%c12The current jackpot sizes are: %jackpots. Every poker hand and bet has a chance to win the jackpot.";
	
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
			bot.sendIRCMessage(chan, Rake.getAnnounceString());
		}
	}
}
