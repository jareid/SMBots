/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.SetNoExternalMessagesEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel is set to only allow messages from users that are in
 * the channel.
 * 
 * This is a type of mode change and therefor is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class SetNoExternalMessages extends SetNoExternalMessagesEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the SetNoExternalMessagesEvent
     * 
     * @see org.pircbotx.hooks.type.SetNoExternalMessagesEvent
	 */
	public SetNoExternalMessages(
	        final SetNoExternalMessagesEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}
}
