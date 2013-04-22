/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.User;
import org.pircbotx.hooks.events.UserModeEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when the mode of a user is set.
 * @author Jamie
 */
public class UserMode extends UserModeEvent<IrcBot> {

	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param target - The user that the mode operation applies to.
	 * @param source - The user that set the mode.
	 * @param mode - The mode that has been set.
	 */
	public UserMode(IrcBot bot, User target, User source, String mode) {
		super(bot, target, source, mode);
	}
}
