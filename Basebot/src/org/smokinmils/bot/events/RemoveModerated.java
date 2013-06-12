/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.RemoveModeratedEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel has 'moderated' mode removed.
 * 
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class RemoveModerated extends RemoveModeratedEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set 
	 * to current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the RemoveModeratedEvent
	 * 
	 * @see org.pircbotx.hooks.type.RemoveModeratedEvent
	 */
	public RemoveModerated(final RemoveModeratedEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}

}