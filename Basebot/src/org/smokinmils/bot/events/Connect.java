/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.ConnectEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched once we successfully connected to the IRC server.
 * 
 * @author Jamie
 */
public class Connect extends ConnectEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 */
	public Connect(ConnectEvent<IrcBot> event) {
		super(event.getBot());
	}

}
