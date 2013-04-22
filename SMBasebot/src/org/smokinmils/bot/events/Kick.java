/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.KickEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever someone (possibly us) is kicked from any of the channels that we are in.
 * 
 * @author Jamie
 */
public class Kick extends KickEvent<IrcBot> {

	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 *
	 * @param bot
	 * @param channel The channel from which the recipient was kicked.
	 * @param source The user who performed the kick.
	 * @param recipient The unfortunate recipient of the kick.
	 * @param reason The reason given by the user who performed the kick.
	 */
	public Kick(IrcBot bot, Channel channel,
			    User source, User recipient,
			    String reason) {
		super(bot, channel, source, recipient, reason);
	}

}
