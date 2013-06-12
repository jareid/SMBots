/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.KickEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever someone (possibly us) is kicked from any
 * of the channels that we are in.
 * 
 * @author Jamie
 */
public class Kick extends KickEvent<IrcBot> {

	/**
	 * Default constructor to setup object. Timestamp is automatically set
	 * to current time as reported by System.currentTimeMillis()
	 *
	 * @param event the KickEvent
	 * 
	 * @see org.pircbotx.hooks.type.KickEvent
	 */
	public Kick(final KickEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getSource(),
				event.getRecipient(), event.getReason());
	}

}
