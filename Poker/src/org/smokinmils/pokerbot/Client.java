/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.Database;
import org.smokinmils.SMBaseBot;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Action;
import org.smokinmils.bot.events.Connect;
import org.smokinmils.bot.events.Disconnect;
import org.smokinmils.bot.events.Invite;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.NickChange;
import org.smokinmils.bot.events.Notice;
import org.smokinmils.bot.events.Op;
import org.smokinmils.bot.events.Part;
import org.smokinmils.bot.events.Quit;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

import org.smokinmils.pokerbot.enums.EventType;
import org.smokinmils.pokerbot.game.rooms.Lobby;
import org.smokinmils.pokerbot.game.rooms.Room;
import org.smokinmils.pokerbot.game.rooms.Table;
import org.smokinmils.pokerbot.settings.Strings;
import org.smokinmils.pokerbot.settings.Variables;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An IRC client based on PircBot that provides a poker playing system
 * 
 * Extends the PircBot class. This class is used to pull in IRC functionality 
 * 
 * @author Jamie Reid
 */
public class Client extends Event {
	/** The name of the server this for */
	private String ServerName;
	
	/** The bot object for this server */
	private IrcBot Bot;
	
	/** A mapping of channel to Room objects */
	private Map<String, Room> validChannels;
	
	/** A mapping of table ID to table channel   */
	private Map<Integer, String> validTables;
	
	/** Main channel for the poker bot */
	private String lobbyChan;
	
	
	public Client( String server, String lobby ) {
		ServerName = server;
		Bot = SMBaseBot.getInstance().getBot(ServerName);
    	validChannels = new HashMap<String, Room>();
    	validTables = new HashMap<Integer, String>();
    	lobbyChan = lobby;
	}
	
	/**
	 * Joins the correct channels
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onConnect()
	 */
	public void connect(Connect event) {
		// At a minimum, we should exist in a lobby
		if ( validChannels.isEmpty() ) {
			event.getBot().joinChannel( lobbyChan );
			Lobby lobby = new Lobby( Bot.getChannel(lobbyChan), this );
			lobby.start();
			validChannels.put( lobbyChan.toLowerCase(), lobby );
		}
		
		// Request invites from Chanserv and attempt to join all channels
		for (Entry<String, Room> entry : validChannels.entrySet() ) {
			event.getBot().sendMessage("ChanServ", "INVITE " + entry.getKey() );
			event.getBot().joinChannel( entry.getKey() );
		}
	}
	
	/**
	 * Automatically reconnect
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onDisconnect()
	 */
	public void disconnect(Disconnect event) {		
		EventLog.info("Disconnected, cancelling all table hands", "Client", "onDisconnect");
		for (Entry<String,Room> entry: validChannels.entrySet()) {
			if (validTables.containsValue(entry.getKey().toLowerCase())) {
				Table table = (Table)entry.getValue();
				table.cancelHand();
			}
		}
	}
	
	/**
	 * Ensures that the bot will join channels when invited by ChanServ
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onInvite(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void invite(Invite event) {
		String chan = event.getChannel();
		if (validChannels.containsKey( chan ) ) {
			event.getBot().joinChannel(chan);
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
	public void join(Join event) {
		String channel = event.getChannel().getName();
		String joinee = event.getUser().getNick();
		// (Re-)Check the user's status with NickServ
		if (joinee.compareToIgnoreCase( event.getBot().getNick() ) == 0 &&
			channel.compareToIgnoreCase(lobbyChan) == 0) {
			Integer[] blinds = {2,6,10};
			ProfileType profiles[] = ProfileType.values();
			for (Integer x: blinds) {
				for (int y = 0; y < profiles.length; y++) {
					newTable(x, 8, profiles[y], false);
				}
			}
		}
		
		// Notify the correct room if required
		Room room = validChannels.get( channel.toLowerCase() );
		if ( room != null ) {
			room.addEvent( joinee, event.getUser().getLogin(),
							event.getUser().getHostmask(), "", EventType.JOIN );
		}
	}
	
	/**
	 * Send the part to the correct Room's events system
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onPart(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void part(Part event) {
		// Notify the correct room if required
		Room room = validChannels.get( event.getChannel().getName().toLowerCase() );
		if ( room != null ) {
			room.addEvent( event.getUser().getNick(), event.getUser().getLogin(),
					       event.getUser().getHostmask(), "", EventType.PART );
		}
	}
	
	/**
	 * Send the quit as a part event to all the Rooms this user is in.
	 * 
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onQuit(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void quit(Quit event) {
		String nick = event.getUser().getNick();
		if ( nick.compareToIgnoreCase( event.getBot().getNick() ) != 0 ) {
			// Notify the correct room if required
			for (Room room: validChannels.values()) {
				room.addEvent( nick, event.getUser().getLogin(),
							   event.getUser().getHostmask(), "", EventType.PART );
			}
		}
	}
	
	/**
	 * Send the quit as a nick change event to all the Rooms this user is in.
	 * (non-Javadoc)
	 * @see org.jibble.pircbot.PircBot#onNickChange(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public void nickChange(NickChange event) {
		// Notify the correct rooms
		for ( Entry<String, Room> room: validChannels.entrySet() )  {
			room.getValue().addEvent( event.getOldNick(), event.getUser().getLogin(),
					   				  event.getUser().getHostmask(), event.getNewNick(),
					   				  EventType.NICKCHANGE );
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
	public void message(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		User user = event.getUser();
		String sender = user.getNick();
		Channel chan = event.getChannel();

    	// Get the first character and check if it is a command
    	char fChar = message.charAt(0);
    	if (fChar == Strings.CommandChar) {
    		if ( bot.userIsIdentified( sender ) ) {				
	    		// Notify the correct room if required
	    		Room room = validChannels.get( chan.getName().toLowerCase() );
	    		if ( room != null ) {
					room.addEvent( sender, user.getLogin(), user.getHostmask(),
								   message, EventType.MESSAGE );
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
	public void action(Action event) {
		// Notify the correct room if required
		Room room = validChannels.get( event.getChannel().getName().toLowerCase() );
		if ( room != null ) {
			room.addEvent( event.getUser().getNick(), event.getUser().getLogin(),
					   	   event.getUser().getHostmask(), event.getAction(), EventType.ACTION );
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
	public void notice(Notice event)  {		
		// Notify the correct room if required
		Room room = validChannels.get( event.getChannel() );
		if ( room != null ) {
			room.addEvent( event.getUser().getNick(), event.getUser().getLogin(),
						   event.getUser().getHostmask(), event.getNotice(), EventType.NOTICE );
		}
    }
	
    /**
     * Processes Op events
     * Passes onto the correct room as an event
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onNotice(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public void op(Op event) {
		// Notify the correct room if required
		Room room = validChannels.get( event.getChannel().getName().toLowerCase() );
		if ( room != null ) {
			room.addEvent( event.getSource().getNick(), event.getSource().getLogin(),
						   event.getSource().getHostmask(), event.getRecipient().getNick(),
						   EventType.OP );
		}
    }
    
    /**
     * Returns the bot for this lobby
     * 
     * @return the bot object
     */
    public IrcBot getBot() { return Bot; }
    
    /**
     * Returns an IRC channel object for a string
     * 
     * @param chan The channel name
     * 
     * @return the channel object
     */
    public Channel getChannel(String chan) { return Bot.getChannel(chan); }
    
    /**
     * Sends a request to NickServ to check a user's status with the server
     * 
     * @param user the username
     */
    private void sendStatusRequest(String user) {
		Bot.sendRawLine( "PRIVMSG NickServ STATUS " + user );
    }

    /**
     * Creates a new poker table
     * 
     * @param stake the big blind for this table
     * @param players the maximum number of players for this table
     */
	public int newTable(int stake, int players, ProfileType profile, boolean manual) {
		EventLog.info("Creating new table...", "Client", "newTable");
		
		Integer tableid = Table.getNextID();
		String chan = Variables.TableChan + tableid;
		Bot.joinChannel( chan );
		Bot.sendMessage("ChanServ", "INVITE " + chan );
		Table table = new Table(Bot.getChannel(chan), this, tableid, stake, players, profile, manual);
		validChannels.put( chan.toLowerCase(), table );
		validTables.put( tableid, chan.toLowerCase() );
		table.start();
		
		if (manual) {
			sendIRCMessage( lobbyChan, table.formatTableInfo(Strings.NewTable) );
		}
		
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
	public void newPlayer(User sender, int id, Integer buy_in) {
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
			Bot.sendInvite( sender, chan );
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
		int usercred = 0;
		try {
			usercred = Database.getInstance().checkCredits( username );
		} catch (Exception e) {
			EventLog.log(e, "Client", "userHasCredits");
		}
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
	public boolean userHasCredits( String username, int credits, ProfileType profile ) {
		int usercred = 0;
		try {
			usercred = Database.getInstance().checkCredits( username, profile );
		} catch (Exception e) {
			EventLog.log(e, "Client", "userHasCredits");
		}
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
		Bot.sendIRCNotice(target, in);
	}
	
	/**
	 * Used to send a message to the target replacing formatting variables correctly
	 * Also allows the sending of multiple lines separate by \n character
	 * 
	 * @param target The place where the message is being sent
	 * @param in The message to send with formatting variables
	 */	
	public void sendIRCMessage(String target, String in) {
		Bot.sendIRCMessage(target, in);
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
		Bot.sendIRCMessage(lobbyChan, in);
	}

	/**
	 * Method to shut down a table when all players leave
	 * @param table
	 */
	public void closeTable(Table table) {
		int found = -1;
		// leave the channel
		Bot.partChannel( table.getChannel() , "No players" );
		
		// Remove from valid tables
		String tblchan = table.getChannel().getName();
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
}
