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
import org.pircbotx.hooks.events.MessageEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Used whenever a message is sent to a channel.
 * 
 * @author Jamie
 */
public class Message extends MessageEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 *
	 * @param bot
	 * @param channel The channel to which the message was sent.
	 * @param user The user who sent the private message.
	 * @param message The actual message.
	 */
	public Message(IrcBot bot, Channel channel, User user, String message) {
		super(bot, channel, user, message);
	}

}
