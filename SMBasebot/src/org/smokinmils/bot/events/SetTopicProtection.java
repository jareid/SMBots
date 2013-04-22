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
import org.pircbotx.hooks.events.SetTopicProtectionEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when topic protection is enabled for a channel. Topic protection means that only operators in a channel may change the topic.
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * @author Jamie
 */
public class SetTopicProtection extends SetTopicProtectionEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel The channel in which the mode change took place.
	 * @param user The user that performed the mode change.
	 */
	public SetTopicProtection(IrcBot bot, Channel channel, User user) {
		super(bot, channel, user);
		// TODO Auto-generated constructor stub
	}

}
