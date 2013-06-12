/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.SetChannelLimitEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a user limit is set for a channel. The number of users in the
 * channel cannot exceed this limit.
 *
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class SetChannelLimit extends SetChannelLimitEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the SetChannelLimitEvent
     * 
     * @see org.pircbotx.hooks.type.SetChannelLimitEvent
	 */
	public SetChannelLimit(final SetChannelLimitEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(),
		        event.getUser(), event.getLimit());
	}
}
