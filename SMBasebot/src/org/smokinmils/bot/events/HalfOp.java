/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.HalfOpEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a user (possibly us) gets halfop status granted in a channel. Note that this isn't supported on all servers or may be used for something else
 * 
 * This is a type of mode change and therefor is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class HalfOp extends HalfOpEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel The channel in which the mode change took place.
	 * @param source The user that performed the mode change.
	 * @param recipient The nick of the user that got owner status.
	 * @param isHalfOp
	 */
	public HalfOp(HalfOpEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getSource(),
				event.getRecipient(), event.isHalfOp()); 
	}

}
