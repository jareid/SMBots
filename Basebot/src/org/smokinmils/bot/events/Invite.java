/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.InviteEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when we are invited to a channel by a user.
 * 
 * @author Jamie
 */
public class Invite extends InviteEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically
	 * set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the InviteEvent
	 * 
	 * @see org.pircbotx.hooks.type.InviteEvent
	 */
	public Invite(final InviteEvent<IrcBot> event) {
		super(event.getBot(), event.getUser(), event.getChannel());
	}
}
