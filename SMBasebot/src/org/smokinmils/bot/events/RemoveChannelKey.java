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
import org.pircbotx.hooks.events.RemoveChannelKeyEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel key is removed.
 * 
 * This is a type of mode change and therefor is also dispatched in a ModeEvent
 *
 * @author Jamie
 */
public class RemoveChannelKey extends RemoveChannelKeyEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel The channel in which the mode change took place.
	 * @param user The user that performed the mode change.
	 * @param key The key that was in use before the channel key was removed.
	 */
	public RemoveChannelKey(IrcBot bot, Channel channel, User user, String key) {
		super(bot, channel, user, key);
	}

}
