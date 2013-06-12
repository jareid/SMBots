/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.VoiceEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a user (possibly us) gets voice status granted in a channel.
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class Voice extends VoiceEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 *
	 * @param event The VoiceEvent
     * 
     * @see org.pircbotx.hooks.type.VoiceEvent
	 */
	public Voice(final VoiceEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(),
		      event.getSource(), event.getRecipient(),
		      event.hasVoice());
	}

}
