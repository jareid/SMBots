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
import org.pircbotx.hooks.events.SetNoExternalMessagesEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel is set to only allow messages from users that are in the channel.
 * 
 * This is a type of mode change and therefor is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class SetNoExternalMessages extends SetNoExternalMessagesEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel The channel in which the mode change took place.
	 * @param user The user that performed the mode change.
	 */
	public SetNoExternalMessages(IrcBot bot, Channel channel, User user) {
		super(bot, channel, user);
	}
}
