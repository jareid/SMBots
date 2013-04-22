/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.VoiceEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a user (possibly us) gets voice status granted in a channel.
 * This is a type of mode change and therefor is also dispatched in a ModeEvent
 * @author Jamie
 */
public class Voice extends VoiceEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 *
	 * @param bot
	 * @param channel - The channel in which the mode change took place.
	 * @param source - The user that performed the mode change.
	 * @param recipient - The nick of the user that got 'voiced'.
	 * @param isVoice
	 */
	public Voice(IrcBot bot, Channel channel, User source, User recipient,
			boolean isVoice) {
		super(bot, channel, source, recipient, isVoice);
		// TODO Auto-generated constructor stub
	}

}
