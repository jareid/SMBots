/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.DisconnectEvent;
import org.smokinmils.bot.IrcBot;

/**
 * This event is dispatched when we get disconnected. It is meant for the bot
 * to carry out actions upon disconnection. This may happen if the PircBotX
 * quits from the server, or if the connection is unexpectedly lost.
 * 
 * Disconnection from the IRC server is detected immediately if either we or
 * the server close the connection normally. If the connection to the server
 * is lost, but neither we nor the server have explicitly closed the connection,
 * then it may take a few minutes to detect (this is commonly referred to as a
 * "ping timeout").
 * 
 * If you wish to get your IRC bot to automatically rejoin a server after the
 * connection has been lost, then this is probably the ideal event listen for to
 * implement such functionality.
 * 
 * @author Jamie
 */
public class Disconnect extends DisconnectEvent<IrcBot> {
	/**
	 * Default constructor to setup object. Timestamp is automatically set to
	 * current time as reported by System.currentTimeMillis()
	 * 
	 * @param event the DisconnectEvent
	 * 
	 * @see org.pircbotx.hooks.type.DisconnectEvent
	 */
	public Disconnect(final DisconnectEvent<IrcBot> event) {
		super(event.getBot());
	}
}
