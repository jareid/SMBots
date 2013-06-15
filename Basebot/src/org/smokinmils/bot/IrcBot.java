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
import org.smokinmils.BaseBot;
import org.smokinmils.database.types.ProfileType;

/**
 * Provides the IRC functionality.
 * 
 * @author Jamie
 */
public class IrcBot extends PircBotX {
    /** Output message when a command is used with incorrect arguments. */
	public static final String INVALID_ARGS = "%b%c12You provided invalid "
	        + "arguments for the command. The format is:";
	
	/** Output message that lists the valid profiles. */
	public static final String VALID_PROFILES = "%b%c12Valid profiles are: %c04"
	        + Arrays.asList(ProfileType.values()).toString();
	
	/**
	 * This string is used when a user doesn't have enough chips.
	 * 
	 * %chips - The amount the user tried to spend
	 */
	public static final String NO_CHIPS_MSG = "%b%c12Sorry, you do not have "
	       + "%c04%chips%c12 chips available for the %c04%profile%c12 profile.";
	
	/** List of users identified with NickServ. */
	private final List<String> identifiedUsers;
	
	/** List of channels. */
	private final List<String> validChannels;
	
	/**
	 * Constructor.
	 * 
	 * @see org.pircbotx.PircBotX
	 */
	public IrcBot() {
		super();
    	identifiedUsers = new ArrayList<String>();
    	validChannels = new ArrayList<String>();
	}
	
	/**
	 * Returns the server this bot is connected to.
	 * 
	 * @return The name of the server
	 */
	@Override
    public final String getServer() {
		return BaseBot.getInstance().getServer(this);
	}
	
	/**
	 * Used to send a notice to the target replacing formatting variables
	 * correctly.
	 * Also allows the sending of multiple lines separate by \n character.
	 * 
	 * @param target The place where the message is being sent
	 * @param in The message to send with formatting variables
	 */	
	public final void sendIRCNotice(final Channel target, final String in) {
	    sendIRCNotice(target.getName(), in);
	}
	
	/**
     * Used to send a notice to the target replacing formatting variables
     * correctly.
     * Also allows the sending of multiple lines separate by \n character.
     * 
     * @param target The place where the message is being sent
     * @param in The message to send with formatting variables
     */ 
	public final void sendIRCNotice(final String target, final String in) {
		String out = in;

		out = out.replaceAll("%newline", "\n");
		out = out.replaceAll("%c", "\u0003");
		out = out.replaceAll("%b", Colors.BOLD);
		out = out.replaceAll("%i", Colors.REVERSE);
		out = out.replaceAll("%u", Colors.UNDERLINE);
		out = out.replaceAll("%n", Colors.NORMAL);

		for (String line: out.split("\n")) {
			this.sendRawLineNow("NOTICE " + target + " " + line);
		}
	}
	
	/**
	 * Used to send a message to the target replacing formatting variables
	 * correctly.
	 * Also allows the sending of multiple lines separate by \n character.
	 * 
	 * @param target The place where the message is being sent
	 * @param in The message to send with formatting variables
	 */
	public final void sendIRCMessage(final Channel target, final String in) {
	    sendIRCMessage(target.getName(), in);
	}
	
	/**
     * Used to send a message to the target replacing formatting variables
     * correctly.
     * Also allows the sending of multiple lines separate by \n character.
     * 
     * @param target The place where the message is being sent
     * @param in The message to send with formatting variables
     */
    public final void sendIRCMessage(final String target, final String in) {
		String out = in;

		out = out.replaceAll("%newline", "\n");
		out = out.replaceAll("%c", "\u0003");
		out = out.replaceAll("%b", Colors.BOLD);
		out = out.replaceAll("%i", Colors.REVERSE);
		out = out.replaceAll("%u", Colors.UNDERLINE);
		out = out.replaceAll("%n", Colors.NORMAL);

		for (String line: out.split("\n")) {
			this.sendRawLineNow("PRIVMSG " + target + " " + line);
		}
	}
	
	/**
	 * Outputs a notice to a user informing them they don't have enough chips.
	 * 
	 * @param user		The user
	 * @param amount	The amount they tried to use
	 * @param profile	The profile they tried to use
	 */
	public final void noChips(final String user,
	                          final int amount,
	                          final ProfileType profile) {
		String out = NO_CHIPS_MSG.replaceAll("%chips",
		                                     Integer.toString(amount));
		out = out.replaceAll("%profile", profile.toString());
		sendIRCNotice(user, out);
	}
	
    /**
     * Sends the invalid argument message.
     * 
     * @param who		The user to send to
     * @param format	The command format
     */
    public final void invalidArguments(final String who, final String format) {
		sendIRCNotice(who, INVALID_ARGS);
		sendIRCNotice(who, format);		
	}
	
	/**
	 * Checks if a user is identified with NickServ.
	 * 
	 * @param nick	The nickname to check
	 * @return	true if the user is identified, false otherwise.
	 */
	public final boolean userIsIdentified(final String nick) {
		return identifiedUsers.contains(nick.toLowerCase());
	}
	
	/**
	 * Removes an identified user from the bot.
	 * 
	 * @param nick	The nickname to check
	 */
	public final void removeIdentifiedUser(final String nick) {
	    synchronized (identifiedUsers) {
	        identifiedUsers.remove(nick.toLowerCase());
	    }
	}
	
	/**
	 * Adds an identified user to the bot.
	 * 
	 * @param nick	The nickname to check
	 */
	public final void addIdentifiedUser(final String nick) {
        synchronized (identifiedUsers) {
            identifiedUsers.add(nick.toLowerCase());
        }
	}
	
	/**
	 * Checks if a user is an op for a channel.
	 * 
	 * @param user The user to check
	 * @param chan The channel to check the user on
	 * 
	 * @return true if they are an op, false otherwise.
	 */
	public final boolean userIsOp(final User user, final String chan) {
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
     * Checks if a user is an op for a channel.
     * 
     * @param user The user to check
     * @param chan The channel to check the user on
     * 
     * @return true if they are an half op, false otherwise.
     */
    public final boolean userIsHalfOp(final User user, final String chan) {
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
	 * Checks if a user is an op for a channel.
     * 
     * @param user The user to check
     * @param chan The channel to check the user on
     * 
     * @return true if they are a host, false otherwise.
	 */
	public final boolean userIsHost(final User user, final String chan) {
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

	/**
	 * 
	 * @param channel The channel to add as a valid channel for this server.
	 */
	public final void addValidChannel(final String channel) {
		validChannels.add(channel.toLowerCase());
	}

	/** 
	 * Provides the list of channels for this IRC server.
	 * 
	 * @return the channel list
	 */
	public final List<String> getValidChannels() {
		return validChannels;
	}
 }
