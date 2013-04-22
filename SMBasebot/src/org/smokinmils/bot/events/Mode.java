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
import org.pircbotx.hooks.events.ModeEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Used when the mode of a channel is set.
 * 
 * You may find it more convenient to decode the meaning of the mode string by using instead OpEvent, VoiceEvent, SetChannelKeyEvent, RemoveChannelKeyEvent, SetChannelLimitEvent, RemoveChannelLimitEvent, SetChannelBanEvent or RemoveChannelBanEvent as appropriate.
 * 
 * @author Jamie
 */
public class Mode extends ModeEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel The channel that the mode operation applies to.
	 * @param user The user that set the mode.
	 * @param mode The mode that has been set.
	 */
	public Mode(IrcBot bot, Channel channel, User user, String mode) {
		super(bot, channel, user, mode);
	}

}
