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
 * This event is dispatched whenever a user sets the topic, or when we join a
 * new channel and discovers its topic.
 * 
 * @author Jamie
 */
public class Topic extends TopicEvent<IrcBot> {

	/**
	 * Default constructor to setup object.
	 * Timestamp is automatically set to current time as reported
	 * by System.currentTimeMillis()
	 * 
	 * @param event the TopicEvent
     * 
     * @see org.pircbotx.hooks.type.TopicEvent
	 */
	public Topic(final TopicEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getOldTopic(),
		      event.getTopic(), event.getUser(),
		      event.getDate(), event.isChanged());
	}

}
