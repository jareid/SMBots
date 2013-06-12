/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.SetSecretEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel is set to be in 'secret' mode. Such channels typically
 * do not appear on a server's channel listing.
 * 
 * This is a type of mode change and therefore is also dispatched in a
 * ModeEvent.
 * 
 * @author Jamie
 */

public class SetSecret extends SetSecretEvent<IrcBot> {

	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event SetSecretEvent
     * 
     * @see org.pircbotx.hooks.type.SetSecretEvent
	 */
	public SetSecret(final SetSecretEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}

}
