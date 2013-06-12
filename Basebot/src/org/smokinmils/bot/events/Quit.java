/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.QuitEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever someone (possibly us) quits from the
 * server. We will only observe this if the user was in one of the channels
 * to which we are connected.
 * 
 * @author Jamie
 */
public class Quit extends QuitEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 *
	 * @param event the QuitEvent
	 * 
	 * @see org.pircbotx.hooks.type.QuitEvent
	 */
	public Quit(final QuitEvent<IrcBot> event) {
		super(event.getBot(), event.getUser(), event.getReason());
	}
	

}
