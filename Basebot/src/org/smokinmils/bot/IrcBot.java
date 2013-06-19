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
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.smokinmils.BaseBot;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.settings.Variables;

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
	
	/** The thread used to check identification. */
	private CheckIdentified identCheck;
	
	/**
	 * Constructor.
	 * 
	 * @param config The configuration object.
	 * 
	 * @see org.pircbotx.PircBotX
	 */
	public IrcBot(final Configuration config) {
		super(config);
    	identifiedUsers = new ArrayList<String>();
    	validChannels = new ArrayList<String>();
    	identCheck = null;
	}
	
	/**
	 * Returns the server this bot is connected to.
	 * 
	 * @return The name of the server
	 */
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
    public final void sendIRCNotice(final String target, final String in) {
        for (String line: replaceIRCVariables(in).split("\n")) {
            sendRaw().rawLine("NOTICE " + target + " " + line);
        }
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
        for (String line: replaceIRCVariables(in).split("\n")) {
            target.send().notice(line);
        }
	}
	
	/**
     * Used to send a notice to the target replacing formatting variables
     * correctly.
     * Also allows the sending of multiple lines separate by \n character.
     * 
     * @param target The place where the message is being sent
     * @param in The message to send with formatting variables
     */ 
	public final void sendIRCNotice(final User target, final String in) {
        for (String line: replaceIRCVariables(in).split("\n")) {
            target.send().notice(line);
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
    public final void sendIRCMessage(final String target, final String in) {
        for (String line: replaceIRCVariables(in).split("\n")) {
            sendRaw().rawLine("PRIVMSG " + target + " " + line);
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
        for (String line: replaceIRCVariables(in).split("\n")) {
            target.send().message(line);
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
    public final void sendIRCMessage(final User target, final String in) {
		for (String line: replaceIRCVariables(in).split("\n")) {
			target.send().message(line);
		}
	}
    
    /**
     * Replace the IRC output.
     * 
     * @param in    The message.
     * @return  The replaced message.
     */
    private static String replaceIRCVariables(final String in) {
        String out = in;

        out = out.replaceAll("%newline", "\n");
        out = out.replaceAll("%c", "\u0003");
        out = out.replaceAll("%b", Colors.BOLD);
        out = out.replaceAll("%i", Colors.REVERSE);
        out = out.replaceAll("%u", Colors.UNDERLINE);
        out = out.replaceAll("%n", Colors.NORMAL);
        
        return out;
    }
	
	/**
	 * Outputs a notice to a user informing them they don't have enough chips.
	 * 
	 * @param user		The user
	 * @param amount	The amount they tried to use
	 * @param profile	The profile they tried to use
	 */
	public final void noChips(final User user,
	                          final int amount,
	                          final ProfileType profile) {
		String out = NO_CHIPS_MSG.replaceAll("%chips",
		                                     Integer.toString(amount));
		out = out.replaceAll("%profile", profile.toString());
		sendIRCNotice(user, out);
	}
	
    /**
     * Sends a request to NickServ to check a user's status with the server
     * and waits for the response.
     * 
     * @param user the username
     * 
     * @return true if the user meets the required status
     */
    public final boolean manualStatusRequest(final String user) {
        boolean ret = false;
        User usr = this.getUserChannelDao().getUser(user);
        if (identCheck != null && user != null) {
            identCheck.manualStatusRequest(usr);
        }
        return ret;
    }
    
    /**
     * Sets the ident check thread.
     * 
     * @param cithread the thread.
     */
    public final void setIdentCheck(final CheckIdentified cithread) {
        identCheck = cithread;
    }
	
    /**
     * Sends the invalid argument message.
     * 
     * @param who		The user to send to
     * @param format	The command format
     */
    public final void invalidArguments(final User who, final String format) {
		sendIRCNotice(who, INVALID_ARGS);
		sendIRCNotice(who, format);		
	}
    
    /**
     * Sends the maximum bet message.
     * 
     * @param who     The user to send to.
     * @param chan    The channel to send to.
     * @param size    The maximum bet.
     */
    public final void maxBet(final User who,
                             final Channel chan,
                             final int size) {
        String out = Variables.MAXBETMSG.replaceAll("%sender", who.getNick());
        out = out.replaceAll("%amount", Integer.toString(size));
        sendIRCMessage(chan, out);
    }
	
	/**
	 * Checks if a user is identified with NickServ.
	 * 
	 * @param user	The user to check
	 * @return	true if the user is identified, false otherwise.
	 */
	public final boolean userIsIdentified(final User user) {
		return identifiedUsers.contains(user.getNick().toLowerCase());
	}
	
	/**
     * Checks if a user is identified with NickServ.
     * 
     * @param nick  The nickname to check
     * @return  true if the user is identified, false otherwise.
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
	 * @param user	The nickname to check
	 */
	public final void addIdentifiedUser(final User user) {
        synchronized (identifiedUsers) {
            identifiedUsers.add(user.getNick().toLowerCase());
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
        for (Channel opchans: user.getChannelsHalfOpIn()) {
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
