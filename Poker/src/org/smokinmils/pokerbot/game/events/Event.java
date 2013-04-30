/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.game.events;

import org.smokinmils.pokerbot.enums.EventType;

/**
 * The base class for the poker/irc events
 * 
 * @author Jamie Reid
 */
public class Event {
	/** The nick of the person who caused the event. */
	public String Sender;
	
	/** The login/ident of the person who caused the event. */
	public String Login;
	
	/** The hostname of the person who caused the event. */
	public String Hostname;
	
	/** The additional details for this event */
	public String Extra;
	
	/** The type of event*/
	public EventType Type;	

	/**
	 * Constructor
	 * 
     * @param sender	The nick of the person who caused the event.
     * @param login		The login of the person who caused the event.
     * @param host		The hostname of the person who caused the event.
     * @param extra		The additional details for this event
     * @param event		The type of event to add.
	 */
	public Event(String sender, String login, 
				 String host, String extra,
				 EventType event) {
		Sender = sender;
		Login = login;
		Hostname = host;
		Extra = extra;
		Type = event;
	}
}
