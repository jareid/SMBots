/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.RemoveNoExternalMessagesEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel is set to allow messages from any user, even if they
 * are not actually in the channel.
 * 
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class RemoveNoExternalMessages
    extends RemoveNoExternalMessagesEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the RemoveNoExternalMessagesEvent
	 * 
	 * @see org.pircbotx.hooks.type.RemoveNoExternalMessagesEvent
	 */
	public RemoveNoExternalMessages(
	        final RemoveNoExternalMessagesEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}

}