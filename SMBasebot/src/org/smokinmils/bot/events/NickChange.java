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
import org.pircbotx.hooks.events.NickChangeEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever someone (possibly us) changes nick on any of the channels that we are on.
 * 
 * @author Jamie
 */
public class NickChange extends NickChangeEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param oldNick - The old nick.
	 * @param newNick - The new nick.
	 * @param user - The user that changed their nick
	 */
	public NickChange(NickChangeEvent<IrcBot> event) {
		super(event.getBot(), event.getOldNick(), event.getNewNick(), event.getUser());
	}
}
