/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.SetChannelKeyEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel key is set. When the channel key has been set,
 * other users may only join that channel if they know the key. Channel keys
 * are sometimes referred to as passwords.
 *
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class SetChannelKey extends SetChannelKeyEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the SetChannelKeyEvent
     * 
     * @see org.pircbotx.hooks.type.SetChannelKeyEvent
	 */
	public SetChannelKey(final SetChannelKeyEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(),
		        event.getUser(), event.getKey());
	}
	
}
