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
import java.util.Arrays;
import java.util.List;

import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.smokinmils.SMBaseBot;
import org.smokinmils.database.types.ProfileType;

/**
 * Provides the IRC functionality
 * 
 * @author Jamie
 */
public class IrcBot extends PircBotX {
	public static final String InvalidArgs = "%b%c12You provided invalid arguments for the command. The format is:";
	public static final String ValidProfiles = "%b%c12Valid profiles are: %c04" + Arrays.asList(ProfileType.values()).toString();
	
	/**
	 * This string is used when a user doesn't have enough chips
	 * 
	 * %chips - The amount the user tried to spend
	 */
	public static final String NoChipsMsg = "%b%c12Sorry, you do not have %c04%chips%c12 chips available for the %c04%profile%c12 profile.";
	
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
	public void sendIRCNotice(Channel target, String in) { sendIRCNotice(target.getName(), in); }
	public void sendIRCNotice(String target, String in) {
		String out = in;

		out = out.replaceAll("%newline", "\n");
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
	public void sendIRCMessage(Channel target, String in) { sendIRCMessage(target.getName(), in); }
	public void sendIRCMessage(String target, String in) {
		String out = in;

		out = out.replaceAll("%newline", "\n");
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
	 * Outputs a notice to a user informing them they don't have enough chips
	 * 
	 * @param user		The user
	 * @param amount	The amount they tried to use
	 * @param profile	The profile they tried to use
	 */
	public void NoChips(String user, int amount, ProfileType profile) {
		String out = NoChipsMsg.replaceAll( "%chips", Integer.toString(amount));
		out = out.replaceAll( "%profile", profile.toString() );
		sendIRCNotice(user, out);
	}
	
    /**
     * Sends the invalid argument message 
     * 
     * @param who		The user to send to
     * @param format	The command format
     */
    public void invalidArguments(String who, String format) {
		sendIRCNotice(who, InvalidArgs);
		sendIRCNotice(who, format);		
	}
	
	/**
	 * Checks if a user is identified with NickServ
	 * 
	 * @param nick	The nickname to check
	 * @return	true if the user is identified, false otherwise.
	 */
	public boolean userIsIdentified(String nick) {
		return IdentifiedUsers.contains(nick.toLowerCase());
	}
	
	/**
	 * Removes an identified user from the bot.
	 * 
	 * @param nick	The nickname to check
	 */
	public void removeIdentifiedUser(String nick) {
		IdentifiedUsers.remove(nick.toLowerCase());
	}
	
	/**
	 * Adds an identified user to the bot.
	 * 
	 * @param nick	The nickname to check
	 */
	public void addIdentifiedUser(String nick) {
		IdentifiedUsers.add(nick.toLowerCase());
	}
	
	/**
	 * Checks if a user is an op for a channel
	 * @param user
	 * @param chan
	 */
	public boolean userIsOp(User user, String chan) {
		boolean ret = false;
		for (Channel opchans: user.getChannelsOpIn()) {
			if (chan.equalsIgnoreCase(opchans.getName())) {
				ret = true;
				break;
			}
		}
		return ret;
	}
	/**
	 * Checks if a user is an op for a channel
	 * @param user
	 * @param chan
	 */
	public boolean userIsHost(User user, String chan) {
		boolean ret = false;
		for (Channel opchans: user.getChannelsOwnerIn()) {
			if (chan.equalsIgnoreCase(opchans.getName())) {
				ret = true;
				break;
			}
		}
		
		if (!ret) {
			for (Channel opchans: user.getChannelsSuperOpIn()) {
				if (chan.equalsIgnoreCase(opchans.getName())) {
					ret = true;
					break;
				}
			}
		}
		
		if (!ret) {
			for (Channel opchans: user.getChannelsOpIn()) {
				if (chan.equalsIgnoreCase(opchans.getName())) {
					ret = true;
					break;
				}
			}
		}
		
		if (!ret) {
			for (Channel opchans: user.getChannelsHalfOpIn()) {
				if (chan.equalsIgnoreCase(opchans.getName())) {
					ret = true;
					break;
				}
			}
		}
		
		if (!ret) {
			for (Channel opchans: user.getChannelsVoiceIn()) {
				if (chan.equalsIgnoreCase(opchans.getName())) {
					ret = true;
					break;
				}
			}
		}
		
		return ret;
	}
 }
