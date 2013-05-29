/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.SetInviteOnlyEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel is set to 'invite only' mode. A user may only join the channel if they are invited by someone who is already in the channel.
 * 
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class SetInviteOnly extends SetInviteOnlyEvent<IrcBot>{
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel The channel in which the mode change took place.
	 * @param user The user that performed the mode change.
	 */
	public SetInviteOnly(SetInviteOnlyEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}

}
