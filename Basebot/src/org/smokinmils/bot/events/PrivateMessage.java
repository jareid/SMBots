/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever a private message is sent to us.
 * 
 * @author Jamie
 */

public class PrivateMessage extends PrivateMessageEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 *
	 * @param event the PrivateMessageEvent
	 * 
	 * @see org.pircbotx.hooks.type.PrivateMessageEvent
	 */
	public PrivateMessage(final PrivateMessageEvent<IrcBot> event) {
		super(event.getBot(), event.getUser(), event.getMessage());
	}

}
