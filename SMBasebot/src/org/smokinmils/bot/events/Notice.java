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
import org.pircbotx.hooks.events.NoticeEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever we receive a notice.
 * 
 * @author Jamie
 */

public class Notice extends NoticeEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 *
	 * @param bot
	 * @param user The user who sent the notice.
	 * @param notice The actual message.
	 */
	public Notice(IrcBot bot, User user, Channel channel, String notice) {
		super(bot, user, channel, notice);
	}

}
