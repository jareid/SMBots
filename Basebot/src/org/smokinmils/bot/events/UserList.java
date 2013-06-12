/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.UserListEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched when we receive a user list from the server after
 * joining a channel.
 * 
 * Shortly after joining a channel, the IRC server sends a list of all users
 * in that channel. The PircBotX collects this information and dispatched this
 * event as soon as it has the full list.
 * 
 * To obtain the nick of each user in the channel, call the User.getNick()
 * method on each User object in the Set.
 * 
 * At a later time, you may call IrcBot.getUsers(org.pircbotx.Channel) to
 * obtain an up to date list of the users in the channel.
 * 
 * @author Jamie
 */
public class UserList extends UserListEvent<IrcBot> {

	/**
	 * Default constructor to setup object. Timestamp is automatically set
	 * to current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the UserListEvent
     * 
     * @see org.pircbotx.hooks.type.UserListEvent
	 */
	public UserList(final UserListEvent<IrcBot> event) {
		super(event.getBot(), event.getChannel(), event.getUsers());
	}

}
