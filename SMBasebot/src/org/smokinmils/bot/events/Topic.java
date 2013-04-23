/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.TopicEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever a user sets the topic, or when we join a new channel and discovers its topic.
 * 
 * @author Jamie
 */
public class Topic extends TopicEvent<IrcBot> {

	/**
	 * Default constructor to setup object.
	 * Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel - The channel that the topic belongs to.
	 * @param topic - The topic for the channel.
	 * @param user - The user that set the topic.
	 * @param date - When the topic was set (milliseconds since the epoch).
	 * @param changed - True if the topic has just been changed, false if the topic was already there.
	 */
	public Topic(TopicEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getTopic(),
				event.getUser(), event.getDate(), event.isChanged());
	}

}
