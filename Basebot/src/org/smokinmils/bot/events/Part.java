/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.PartEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever someone (possibly us) parts a channel which we are on.
 * 
 * @author Jamie
 */
public class Part extends PartEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 *
	 * @param bot
	 * @param user The user that quit from the server in snapshot form
	 * @param reason The reason given for quitting the server.
	 */
	public Part(PartEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser(), event.getReason());
	}

}
