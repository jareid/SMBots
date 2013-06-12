/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.SetModeratedEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a channel is set to 'moderated' mode. If a channel is moderated,
 * then only users who have been 'voiced' or 'opped' may speak or change their
 * nicks.
 * 
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class SetModerated extends SetModeratedEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the SetModeratedEvent
     * 
     * @see org.pircbotx.hooks.type.SetModeratedEvent
	 */
	public SetModerated(final SetModeratedEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUser());
	}
}
