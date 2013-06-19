/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.OpEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a user (possibly us) gets granted operator status for a channel.
 *
 * This is a type of mode change and therefor is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class Op extends OpEvent<IrcBot> {
	/**
	 * Default constructor to setup object.
	 * 
	 * Timestamp is automatically set to current time as reported by
	 * System.currentTimeMillis()
	 * 
	 * @param event the OpEvent
	 * 
	 * @see org.pircbotx.hooks.type.OpEvent
	 */
	public Op(final OpEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser(),
				event.getRecipient(), event.isOp()); 
	}

}
