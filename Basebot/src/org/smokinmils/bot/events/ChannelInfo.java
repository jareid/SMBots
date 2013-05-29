/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot.events;

import org.pircbotx.hooks.events.ChannelInfoEvent;
import org.smokinmils.bot.IrcBot;

/**
 * After calling the listChannels() method in PircBotX, the server will start to send us information about each channel on the server. You may listen for this event in order to receive the information about each channel as soon as it is received.
 * 
 * Note that certain channels, such as those marked as hidden, may not appear in channel listings.
 * 
 * @author Jamie
 */
public class ChannelInfo extends ChannelInfoEvent<IrcBot> {
	/**
	 * After calling the listChannels() method in PircBotX, the server will start to send us information about each channel on the server. You may listen for this event in order to receive the information about each channel as soon as it is received.
	 * 
	 * Note that certain channels, such as those marked as hidden, may not appear in channel listings.
	 * 
	 * @param bot
	 * @param list  A list of ChannelList Entries
	 */
	public ChannelInfo(ChannelInfoEvent<IrcBot> event) {
		super(event.getBot(), event.getList());
	}
}
