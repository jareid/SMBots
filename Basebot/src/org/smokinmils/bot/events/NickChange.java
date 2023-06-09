/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.NickChangeEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever someone (possibly us) changes nick on any
 * of the channels that we are on.
 * 
 * @author Jamie
 */
public class NickChange extends NickChangeEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the NickChangeEvent
	 * 
	 * @see org.pircbotx.hooks.type.NickChangeEvent
	 */
	public NickChange(final NickChangeEvent<IrcBot> event) {
		super(event.getBot(), event.getOldNick(),
		        event.getNewNick(), event.getUser());
	}
}
