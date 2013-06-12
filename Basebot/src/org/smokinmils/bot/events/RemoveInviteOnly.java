/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.RemoveInviteOnlyEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel has 'invite only' mode removed.
 * 
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class RemoveInviteOnly extends RemoveInviteOnlyEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the RemoveInviteOnlyEvent
	 * 
	 * @see org.pircbotx.hooks.type.RemoveInviteOnlyEvent
	 */
	public RemoveInviteOnly(final RemoveInviteOnlyEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}

}