/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.MessageEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Used whenever a message is sent to a channel.
 * 
 * @author Jamie
 */
public class Message extends MessageEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set
	 * to current time as reported by System.currentTimeMillis()
	 *
	 * @param event the MessageEvent
	 * @see org.pircbotx.hooks.type.MessageEvent
	 */
	public Message(final MessageEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(),
		        event.getUser(), event.getMessage());
	}
}
