/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.RemoveSecretEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel has 'secret' mode removed.
 * 
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class RemoveSecret extends RemoveSecretEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the RemoveSecretEvent
     * 
     * @see org.pircbotx.hooks.type.RemoveSecretEvent
	 */
	public RemoveSecret(final RemoveSecretEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}

}
