/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.UserModeEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when the mode of a user is set.
 * 
 * @author Jamie
 */
public class UserMode extends UserModeEvent<IrcBot> {

	/**
	 * Default constructor to setup object.
	 * 
	 * Timestamp is automatically set to current time as reported
	 * by System.currentTimeMillis()
	 * 
	 * @param event the UserModeEvent
     * 
     * @see org.pircbotx.hooks.type.UserModeEvent
	 */
	public UserMode(final UserModeEvent<IrcBot> event) {
		super(event.getBot(), event.getTarget(),
		      event.getSource(), event.getMode());
	}
}
