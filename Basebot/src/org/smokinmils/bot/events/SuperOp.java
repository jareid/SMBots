/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.SuperOpEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a user (possibly us) gets superop status granted in a channel.
 * Note that this isn't supported on all servers or may be used for something
 * else.
 * 
 * This is a type of mode change and therefore is also dispatched in a
 * ModeEvent.
 * 
 * @author Jamie
 */
public class SuperOp extends SuperOpEvent<IrcBot> {

	/**
	 * Default constructor to setup object. Timestamp is automatically set
	 * to current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the SuperOpEvent
     * 
     * @see org.pircbotx.hooks.type.SuperOpEvent
	 */
	public SuperOp(final SuperOpEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(),
		      event.getSource(), event.getRecipient(),
		      event.isSuperOp());
	}

}
