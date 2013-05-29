/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.RemoveChannelLimitEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when the user limit is removed for a channel.
 * 
 * This is a type of mode change and therefor is also dispatched in a ModeEvent
 *
 * @author Jamie
 */
public class RemoveChannelLimit extends RemoveChannelLimitEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel The channel in which the mode change took place.
	 * @param user The user that performed the mode change.
	 */
	public RemoveChannelLimit(RemoveChannelLimitEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}

}
