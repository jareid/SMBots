/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot;

import org.smokinmils.pokerbot.enums.EventType;
import org.smokinmils.pokerbot.game.rooms.Lobby;
import org.smokinmils.pokerbot.game.rooms.Room;
import org.smokinmils.pokerbot.game.rooms.Table;
import org.smokinmils.pokerbot.logging.EventLog;
import org.smokinmils.pokerbot.settings.Strings;
import org.smokinmils.pokerbot.settings.Variables;
import org.smokinmils.pokerbot.tasks.Reconnect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;

import org.jibble.pircbot.*;

/**
 * An IRC client based on PircBot that provides a poker playing system
 * 
 * Extends the PircBot class. This class is used to pull in IRC functionality 
 * 
 * @author Jamie Reid
 */
public class Client extends PircBot {	
	/** A list of users who are identified successfully with NickServ */
	private List<String> identifiedUsers;
	
	/** A list of users already sent a no identification message */
	private List<String> sentNoIdent;
	
	/** A string to store the response from a nickserv status command */
	private static final String NickServStatus = "STATUS";
	
	/** A mapping of channel to Room objects */
	private Map<String, Room> validChannels;
	
	/** A mapping of table ID to table channel   */
	private Map<Integer, String> validTables;
	
	/** Main channel for the poker bot */
	private String lobbyChan;
	
	/** A timer used for reconnecting after connection drops */
	private Timer reconnectTimer;
	
	/** The current version of the bot */
	private static String Version = "0.1a";
	
	
	public Client( String lobby, boolean debug ) {
    	identifiedUsers = new ArrayList<String>();
    	sentNoIdent = new ArrayList<String>();
    	validChannels = new HashMap<String, Room>();
    	validTables = new HashMap<Integer, String>();
    	lobbyChan = lobby;
    	
    	setLogin( Variables.Login );
    	setName( Variables.Nick );
    	setFinger( Variables.FingerMsg );
    	
    	startIdentServer();
    	
    	setVerbose(debug); 
    	setVersion(Version);    
    	
    	setMessageDelay( Variables.MessageDelayMS );
	}
	
	/**
	 * Joins the correct channels
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onConnect()
	 */
	protected void onConnect() {
		if (reconnectTimer != null) reconnectTimer.cancel();
		
		// At a minimum, we should exist in a lobby
		if ( validChannels.isEmpty() ) {
			Lobby lobby = new Lobby( lobbyChan.toLowerCase(), this );
			lobby.start();
			validChannels.put( lobbyChan.toLowerCase(), lobby );
		}
		
		// Request invites from Chanserv and attempt to join all channels
		for (Entry<String, Room> entry : validChannels.entrySet() ) {
			this.sendMessage("ChanServ", "INVITE " + entry.getKey() );
			this.joinChannel( entry.getKey() );
		}
		   
		 this.identify(Variables.NickServPassword);
	}
	
	/**
	 * Automatically reconnect
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onDisconnect()
	 */
	protected void onDisconnect() {		
		EventLog.info("Disconnected, cancelling all table hands", "Client", "onDisconnect");
		for (Entry<String,Room> entry: validChannels.entrySet()) {
			if (validTables.containsValue(entry.getKey().toLowerCase())) {
				Table table = (Table)entry.getValue();
				table.cancelHand();
			}
		}

		EventLog.info("Disconnected, attempting to reconnect", "Client", "onDisconnect");
		reconnectTimer = new Timer();
		reconnectTimer.schedule(new Reconnect( this ), Variables.ReconnectMS);
	}
	
	/**
	 * Ensures that the bot will join channels when invited by ChanServ
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onInvite(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	protected void onInvite(String targetNick, String sourceNick, 
							String sourceLogin, String sourceHostname,
							String channel) {
		if (validChannels.containsKey(channel)) {
			this.joinChannel(channel);
		}		
	}
	
	/**
	 * Keep a track of new users joining so we can:
	 * - check if they are identified
	 * - send the event to the channel's (room's) event system.
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onJoin(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	protected void onJoin(String channel, String joinee, 
						String login, String hostname) {
		// (Re-)Check the user's status with NickServ
		if (joinee.compareToIgnoreCase( getNick() ) != 0) {
			sendStatusRequest( joinee );
		} else {
			if (channel.compareToIgnoreCase(lobbyChan) == 0) {
				/* TODO: change to loops
				newTable(2, 8, 1, false);
				newTable(6, 8, 1, false);
				newTable(10, 8, 1, false);
				newTable(2, 8, 2, false);
				newTable(6, 8, 2, false);
				newTable(10, 8, 2, false);*/
				newTable(2, 8, 3, false);
				newTable(6, 8, 3, false);
				newTable(10, 8, 3, false);
			}
		}
		
		// Notify the correct room if required
		Room room = validChannels.get( channel.toLowerCase() );
		if ( room != null ) {
			room.addEvent( joinee, login, hostname, "", EventType.JOIN );
		}
	}
	
	/**
	 * Send the part to the correct Room's events system
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onPart(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	protected void onPart(String channel, String parter, 
					   String login, String hostname) {
		// Notify the correct room if required
		Room room = validChannels.get( channel.toLowerCase() );
		if ( room != null ) {
			room.addEvent( parter, login, hostname, "", EventType.PART );
		}
	}
	
	/**
	 * Send the quit as a part event to all the Rooms this user is in.
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onQuit(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	protected void onQuit(String channel, String quiter, 
					   String login, String hostname) {
		if ( quiter.compareToIgnoreCase( this.getNick() ) == 0 ) {
			try {
				this.reconnect();
			} catch (NickAlreadyInUseException e) {
				EventLog.fatal(e, "Client", "onQuit");
				System.exit(1);
			} catch (IOException | IrcException e) {
				EventLog.fatal(e, "Client", "onQuit");
				System.exit(1);
			}
		} else {
			// Notify the correct room if required
			Room room = validChannels.get( channel.toLowerCase() );
			if ( room != null ) {
				room.addEvent( quiter, login, hostname, "", EventType.PART );
			}
		}
	}
	
	/**
	 * Send the quit as a nick change event to all the Rooms this user is in.
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onNickChange(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	protected void onNickChange(String oldNick, String login,
				 			 	String hostname, String newNick) {
		// Notify the correct rooms
		for ( Entry<String, Room> room: validChannels.entrySet() )  {
			room.getValue().addEvent( oldNick, login, hostname, newNick, EventType.NICKCHANGE );
		}
	}
    
	/**
	 * We joined a channel so beginning checking users' status 
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onUserList(java.lang.String, org.jibble.pircbot.User[])
	 */
	protected void onUserList(String channel, User[] users) {
		for (User usr: users) {
			sendStatusRequest( usr.getNick() );
		}
		
		channel = channel.toLowerCase();
		if (validTables.containsValue(channel))  {
			Table table = (Table)validChannels.get(channel);
			table.joinedChannel(users);
		}
	}
	
	
	/**
	 * Pass the message to the correct Room
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onMessage(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	protected void onMessage(String channel, String sender,
                       		String login, String hostname, String message) {
    	// Get the first character and check if it is a command
    	char fChar = message.charAt(0);
    	if (fChar == Strings.CommandChar) {
    		
    		// Check the user meets the NickServ status requirement
    		if ( identifiedUsers.contains( sender ) ) {
    			// Add hostmask for the user
				Database.getInstance().addHostmask(sender, "*!*@" + hostname);
				
	    		// Notify the correct room if required
	    		Room room = validChannels.get( channel.toLowerCase() );
	    		if ( room != null ) {
					room.addEvent( sender, login, hostname, message, EventType.MESSAGE );
	    		}
    		} else {
    			sendStatusRequest( sender );

    			// If we already told this user, don't tell them again
    			if ( !sentNoIdent.contains( sender ) ) {
        			this.sendIRCMessage( sender, Strings.NotIdentifiedMsg );
        			sentNoIdent.add( sender );
    			}
    		}
    	}
    }
	
	/**
	 * Pass the action to the correct Room
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onMessage(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	protected void onAction(String channel, String sender,
                       		String login, String hostname, String action) {
		// Notify the correct room if required
		Room room = validChannels.get( channel.toLowerCase() );
		if ( room != null ) {
			room.addEvent( sender, login, hostname, action, EventType.ACTION );
		}
    }
	

    /**
     * Processes NickServ responses. Currently uses
     * - STATUS
     * Passes onto the correct room as an event
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onNotice(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
	protected void onNotice(String sender, String login,
    					 String hostname, String target, String notice)  {
    	if ( sender.compareToIgnoreCase( "NickServ" ) == 0 ) {
    		String[] msg = notice.split(" ");
    		
    		if ( msg[0].compareTo( NickServStatus ) == 0 && msg.length == 3)
    		{
    			String user = msg[1];
    			Integer code = Utils.tryParse( msg[2] );
    			
    			// Only add users with the correct levels
    			if (code >= Variables.RequiredStatus) {
    				identifiedUsers.add( user );		
    				sentNoIdent.remove( user );
    			}
    		}
    	} else {    		
    		// Notify the correct room if required
    		Room room = validChannels.get( target );
    		if ( room != null ) {
				room.addEvent( sender, login, hostname, notice, EventType.NOTICE );
    		}
    	}
    }
	
    /**
     * Processes NickServ responses. Currently uses
     * - STATUS
     * Passes onto the correct room as an event
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onNotice(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    protected void onOp(String channel, String sourceNick, String sourceLogin,
    				   String sourceHostname, String recipient) {
		// Notify the correct room if required
		Room room = validChannels.get( channel.toLowerCase() );
		if ( room != null ) {
			room.addEvent( sourceNick, sourceLogin, sourceHostname, recipient, EventType.OP );
		}
    }
    
    /**
     * Sends a request to NickServ to check a user's status with the server
     * 
     * @param user the username
     */
    private void sendStatusRequest(String user) {
		this.sendRawLine( "PRIVMSG NickServ STATUS " + user );
    }

    /**
     * Creates a new poker table
     * 
     * @param stake the big blind for this table
     * @param players the maximum number of players for this table
     */
	public int newTable(int stake, int players, int profile_id, boolean manual) {
		EventLog.info("Creating new table...", "Client", "newTable");
		
		Integer tableid = Table.getNextID();
		String chan = Variables.TableChan + tableid;
		Table table = new Table(chan.toLowerCase(), this, tableid, stake, players, profile_id, manual);
		table.start();
		validChannels.put( chan.toLowerCase(), table );
		validTables.put( tableid, chan.toLowerCase() );
		this.sendMessage("ChanServ", "INVITE " + chan );
		this.joinChannel( chan );
		
		sendIRCMessage( lobbyChan, table.formatTableInfo(Strings.NewTable) );
		
		return tableid;
	}
	
	/**
	 * Gets the Table with a specified ID
	 * 
	 * @param id the table id
	 * 
	 * @return the table that matches or null
	 */
	public Table getTable(int id) {
		String chan = validTables.get(id).toLowerCase();
		return ((Table)validChannels.get(chan));
	}
	
    /**
     * Creates a new player on a table
     * 
     * @param sender  The user
     * @param id	  The table ID
     * @param buy_in  The initial buy in
     */
	public void newPlayer(String sender, int id, Integer buy_in) {
		EventLog.info("Adding new player...", "Client", "newTable");
		// Get channel for the table id
		String chan = validTables.get(id);
		if ( chan != null ) {		
			// Add player to table
			Table tbl = (Table) validChannels.get(chan.toLowerCase());
			if (tbl != null) {
				tbl.playerJoins(sender, buy_in);
			} else {
				EventLog.log(sender + "tried to join " + Integer.toString(id) + "but couldn't find the table",
								"Client", "newPlayer");
				sendIRCMessage(lobbyChan, "Something went wrong when the bot tried to add you to the table, please inform staff");
			}
		} else {
			EventLog.log(sender + "tried to join " + Integer.toString(id) + "but couldn't find the table's channel",
					"Client", "newPLayer");
			sendIRCMessage(lobbyChan, "Something went wrong when the bot tried to add you to the table, please inform staff");
		}
	}
	
    /**
     * Allows a new user to join the channel to watch the game play
     * 
     * @param sender  The user
     * @param id	  The table ID
     */
	public void newObserver(String sender, int id) {
		// Get channel for the table id
		String chan = validTables.get(id);
		if ( chan != null ) {		
			// Invite the player to join channel
			this.sendInvite( sender, chan );
		} else {
			EventLog.log(sender + "tried to join " + Integer.toString(id) + "but couldn't find the table's channel",
					"Client", "newPLayer");
			sendIRCMessage(lobbyChan, "Something went wrong when the bot tried to add you to the table, please inform staff");
		}
	}
	
	/**
	 * Used to check whether a table is full
	 * 
	 * @param id The ID of the table to be checked
	 * 
	 * @return boolean true if the table has no seats left
	 */
	public boolean tableIsFull(int id) {
		boolean full = false;
		// Get table for the table id
		String chan = validTables.get(id);
		Table tbl = (Table) validChannels.get( chan );
		if (tbl != null) {
			full = tbl.isFull();
		}
		return full;
	}
	
	/**
	 * Used to check whether a user has credits
	 * 
	 * @param username The username who's credit level we need to check
	 * @param credits The amount of credits they need to have
	 * 
	 * @return boolean true if the user has sufficient credits.
	 */
	public boolean userHasCredits( String username, int credits ) {
		int usercred = Database.getInstance().checkCredits( username );
		return ((usercred - credits) >= 0);
	}
	
	/**
	 * Used to check whether a user has credits for a certain profile
	 * 
	 * @param username The username who's credit level we need to check
	 * @param credits The amount of credits they need to have
	 * 
	 * @return boolean true if the user has sufficient credits.
	 */
	public boolean userHasCredits( String username, int credits, String profile ) {
		int usercred = Database.getInstance().checkCredits( username, profile );
		return ((usercred - credits) >= 0);
	}
	
	/**
	 * Used to check whether a user has credits for a certain profile
	 * 
	 * @param username The username who's credit level we need to check
	 * @param credits The amount of credits they need to have
	 * 
	 * @return boolean true if the user has sufficient credits.
	 */
	public boolean userHasCredits( String username, int credits, int profile_id ) {
		int usercred = Database.getInstance().checkCredits( username, profile_id );
		return ((usercred - credits) >= 0);
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
	 * Used to send a message to the target replacing formatting variables correctly
	 * Also allows the sending of multiple lines separate by \n character
	 * 
	 * Sends to the lobby
	 * 
	 * @param target The place where the message is being sent
	 * @param in The message to send with formatting variables
	 */	
	public void sendIRCMessage(String in) {
		String out = in;
		
		out = out.replaceAll("%c", "\u0003");
		out = out.replaceAll("%b", Colors.BOLD);
		out = out.replaceAll("%i", Colors.REVERSE);
		out = out.replaceAll("%u", Colors.UNDERLINE);
		out = out.replaceAll("%n", Colors.NORMAL);

		for (String line: out.split("\n")) {
			this.sendMessage(lobbyChan, line);
		}
	}
	
	/**
	 * Retrieves the reconnection timer
	 * 
	 * @return the timer
	 */
	public Timer getReconnectTimer() { return reconnectTimer; }

	/**
	 * Method to shut down a table when all players leave
	 * @param table
	 */
	public void closeTable(Table table) {
		int found = -1;
		// leave the channel
		this.partChannel( table.getChannel(), "No players" );
		
		// Remove from valid tables
		String tblchan = table.getChannel();
		validChannels.remove( tblchan );
		for (Entry<Integer, String> tbl: validTables.entrySet()) {
			if (tblchan.compareToIgnoreCase( tbl.getValue() ) == 0) {
				found = tbl.getKey();
				break;
			}
		}
		
		if (found != -1) {
			sendIRCMessage( lobbyChan, Strings.TableClosed.replaceAll("%id", Integer.toString(found)) );
			EventLog.info("Table " +  Integer.toString(found) + " closed", "Client", "closeTable");
			validTables.remove(found);			
		} else {
			EventLog.log("Tried to close " + tblchan + " but not ID found" , "Client", "closeTable");
		}
	}
	
	public boolean isHost(String username, String chan) {
		boolean result = false;
		List<User> userList = Arrays.asList(this.getUsers(chan));
		for (User u: userList) {
			if (u.getNick().compareToIgnoreCase(username) == 0) {
				if (u.hasVoice() || u.isOp()) {
					result = true;
					break;
				}
			}
		}
		return result;
	}
}
