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
import org.pircbotx.hooks.events.JoinEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever someone (possibly us) joins a channel which we are on.
 * 
 * @author Jamie
 */
public class Join extends JoinEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * @param bot
	 * @param channel The channel which somebody joined.
	 * @param user The user who joined the channel.
	 */
	public Join(IrcBot bot, Channel channel, User user) {
		super(bot, channel, user);
	}
}
