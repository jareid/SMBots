/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.NoticeEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched whenever we receive a notice.
 * 
 * @author Jamie
 */

public class Notice extends NoticeEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set
	 * to current time as reported by System.currentTimeMillis()
	 *
	 * @param event the NoticeEvent
	 * 
	 * @see org.pircbotx.hooks.type.NoticeEvent
	 */
	public Notice(final NoticeEvent<IrcBot> event) {
		super(event.getBot(), event.getUser(),
		        event.getChannel(), event.getNotice());
	}
}
