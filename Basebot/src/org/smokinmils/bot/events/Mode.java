/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.ModeEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Used when the mode of a channel is set.
 * 
 * You may find it more convenient to decode the meaning of the mode string
 * by using instead OpEvent, VoiceEvent, SetChannelKeyEvent,
 * RemoveChannelKeyEvent, SetChannelLimitEvent, RemoveChannelLimitEvent,
 * SetChannelBanEvent or RemoveChannelBanEvent as appropriate.
 * 
 * @author Jamie
 */
public class Mode extends ModeEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set
	 * to current time as reported by System.currentTimeMillis()
	 * 
	 * @param event ModeEvent
	 * 
	 * @see org.pircbotx.hooks.type.ModeEvent
	 */
	public Mode(final ModeEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(),
		      event.getUser(), event.getMode());
	}
}
