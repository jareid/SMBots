/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import java.util.Set;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.events.UserListEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched when we receive a user list from the server after joining a channel.
 * 
 * Shortly after joining a channel, the IRC server sends a list of all users in that channel. The PircBotX collects this information and dispatched this event as soon as it has the full list.
 * 
 * To obtain the nick of each user in the channel, call the User.getNick() method on each User object in the Set.
 * 
 * At a later time, you may call IrcBot.getUsers(org.pircbotx.Channel) to obtain an up to date list of the users in the channel.
 * 
 * @author Jamie
 */
public class UserList extends UserListEvent<IrcBot> {

	/**
	 * Default constructor to setup object. Timestamp is automatically set to current time as reported by System.currentTimeMillis()
	 * 
	 * @param bot
	 * @param channel - The channel that the user list is from.
	 * @param users - An immutable Set of Users belonging to this channel.
	 */
	public UserList(IrcBot bot, Channel channel, Set<User> users) {
		super(bot, channel, users);
		// TODO Auto-generated constructor stub
	}

}
