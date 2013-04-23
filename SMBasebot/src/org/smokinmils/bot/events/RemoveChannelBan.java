/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.RemoveChannelBanEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a hostmask ban is removed from a channel.
 * 
 * This is a type of mode change and therefor is also dispatched in a ModeEvent
 *
 * @author Jamie
 */
public class RemoveChannelBan extends RemoveChannelBanEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel The channel in which the mode change took place.
	 * @param user The user that performed the mode change.
	 * @param hostmask The ban hostmask removed.
	 */
	public RemoveChannelBan(RemoveChannelBanEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser(), event.getHostmask());
	}

}
