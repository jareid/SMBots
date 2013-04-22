/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.smokinmils.SMBaseBot;

/**
 * Provides the IRC functionality
 * 
 * @author Jamie
 */
public class IrcBot extends PircBotX {
	/** List of users identified with NickServ */
	private List<String> IdentifiedUsers;
	
	/**
	 * Constructor
	 */
	public IrcBot() {
		super();
    	IdentifiedUsers = new ArrayList<String>();
	}
	
	/**
	 * Returns the server this bot is connected to
	 * 
	 * @return The name of the server
	 */
	public String getServer() {
		return SMBaseBot.getInstance().getServer(this);
	}
	
	/**
	 * Used to send a notice to the target replacing formatting variables correctly
	 * Also allows the sending of multiple lines separate by \n character
	 * 
	 * @param target The place where the message is being sent
	 * @param in The message to send with formatting variables
	 */	
	public void sendIRCNotice(String target, String in) {
		String out = in;
		
		out = out.replaceAll("%c", "\u0003");
		out = out.replaceAll("%b", Colors.BOLD);
		out = out.replaceAll("%i", Colors.REVERSE);
		out = out.replaceAll("%u", Colors.UNDERLINE);
		out = out.replaceAll("%n", Colors.NORMAL);

		for (String line: out.split("\n")) {
			this.sendNotice(target, line);
		}
	}
	
	/**
	 * Used to send a message to the target replacing formatting variables correctly
	 * Also allows the sending of multiple lines separate by \n character
	 * 
	 * @param target The place where the message is being sent
	 * @param in The message to send with formatting variables
	 */	
	public void sendIRCMessage(String target, String in) {
		String out = in;
		
		out = out.replaceAll("%c", "\u0003");
		out = out.replaceAll("%b", Colors.BOLD);
		out = out.replaceAll("%i", Colors.REVERSE);
		out = out.replaceAll("%u", Colors.UNDERLINE);
		out = out.replaceAll("%n", Colors.NORMAL);

		for (String line: out.split("\n")) {
			this.sendMessage(target, line);
		}
	}
	
	/**
	 * Checks if a user is identified with NickServ
	 * 
	 * @param nick	The nickname to check
	 * @return	true if the user is identified, false otherwise.
	 */
	public boolean userIsIdentified(String nick) {
		return IdentifiedUsers.contains(nick);
	}
	
	/**
	 * Removes an identified user from the bot.
	 * 
	 * @param nick	The nickname to check
	 */
	public void removeIdentifiedUser(String nick) {
		IdentifiedUsers.remove(nick);
	}
	
	/**
	 * Adds an identified user to the bot.
	 * 
	 * @param nick	The nickname to check
	 */
	public void addIdentifiedUser(String nick) {
		IdentifiedUsers.add(nick);
	}
}
