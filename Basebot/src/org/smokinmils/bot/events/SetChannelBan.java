/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.SetChannelBanEvent;
import org.smokinmils.bot.IrcBot;

/**
 * Called when a user (possibly us) gets banned from a channel. Being banned
 * from a channel prevents any user with a matching hostmask from joining the
 * channel. For this reason, most bans are usually directly followed by the
 * user being kicked.
 *
 * This is a type of mode change and therefore is also dispatched in a ModeEvent
 * 
 * @author Jamie
 */
public class SetChannelBan extends SetChannelBanEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the SetChannelBanEvent
     * 
     * @see org.pircbotx.hooks.type.SetChannelBanEvent
	 */
	public SetChannelBan(final SetChannelBanEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(),
		        event.getUser(), event.getHostmask());
	}
}
