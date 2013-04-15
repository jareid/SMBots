/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.game.rooms;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;

import org.jibble.pircbot.Colors;
import org.smokinmils.pokerbot.Client;
import org.smokinmils.pokerbot.enums.CommandType;
import org.smokinmils.pokerbot.enums.EventType;
import org.smokinmils.pokerbot.enums.RoomType;
import org.smokinmils.pokerbot.game.events.*;
import org.smokinmils.pokerbot.logging.EventLog;
import org.smokinmils.pokerbot.settings.Strings;

/**
 * Provides the base class for the lobby and tables, provides the main run method
 * 
 * Implements Thread
 * 
 * @author Jamie Reid
 */
public class Room extends Thread {
	/** The IRC client */
	protected Client ircClient;

	/** The channel this bot is running on */
	protected String ircChannel;
	
	/** The type of Room this is */
	private RoomType roomType;
	
	/** The topic for the IRC channel */
	protected String roomTopic;
	
	/** A queue of events this room needs to process */
	protected Deque<Event> Events;

    /**
     * Constructor.
     * 
     * @param channel  The channel this bot is running on
     * @param irc	   The IRC client
     * @param rt	   The type of Room this is
     */
	public Room( String channel, Client irc, RoomType rt ) {
		Events = new ArrayDeque<Event>();
		
		ircClient = irc;
		ircChannel = channel;
		roomType = rt;
	}
	
	/**
     * Returns the room type.
     * 
     * @return The room type
     */
	public final RoomType getRoomType() { return roomType; }

	/**
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		boolean interuptted = false;
    	while ( !(Thread.interrupted() || interuptted) ) { 
        	if ( !Events.isEmpty() ) {
        		EventLog.debug("Processing next event", "Room", "run");
                Event event = Events.removeFirst();
                try {
                switch (event.Type) {
	                case ACTION:
	                	this.onAction( event.Sender, event.Login,
	                				   event.Hostname, event.Extra );
	                	break;
	                case JOIN:
	                	this.onJoin( event.Sender, event.Login, event.Hostname );
	                	break;
	                case MESSAGE:
	                	this.onMessage( event.Sender, event.Login,
	                					event.Hostname, event.Extra );
	                	break;
	                case NICKCHANGE:
	                	this.onMessage( event.Sender, event.Login,
	                					event.Hostname, event.Extra );
	                	break;
	                case NOTICE:
	                	this.onNotice( event.Sender, event.Login,
	                				   event.Hostname, event.Extra );
	                	break;
	                case PART:
	                	this.onPart( event.Sender, event.Login, event.Hostname );
	                	break;
	                case OP:
	                	this.onOp( event.Sender, event.Login, event.Hostname, event.Extra );
	                	break;
	                case TIMER:
	                	this.onTimer( event.Sender );
	                default:
	                	break;                	
	                }
            	} catch (Exception e) {
            		ircClient.sendIRCMessage("Something caused the bot to crash... please notify the staff.");
            		EventLog.fatal(e, "Room", "run");
            		StringWriter sw = new StringWriter();
            		PrintWriter pw = new PrintWriter(sw);
            		e.printStackTrace(pw);
            		EventLog.log(sw.toString(), "Room", "run");
            		System.exit(1);
            	}
            }
        	try {
        		Thread.sleep(50);
        	} catch (InterruptedException e) {
        		interuptted = true;
        	}
    	}
    	return;
	}	
	
	/**
	 * Adds an event to be handled by this Room's thread
	 * 
     * @param sender	The nick of the person who caused the event.
     * @param login		The login of the person who caused the event.
     * @param host		The hostname of the person who caused the event.
     * @param extra		The additional details for this event
     * @param type		The type of event to add.
	 */
	public void addEvent( String sender, String login,
						  String host, String extra,
						  EventType type) {
		EventLog.debug("New event added", "Room", "addEvent");
		Events.addLast( new Event(sender, login, host, extra, type) );		
	}
	
	/**
	 * Method to get this rooms IRC channel
	 * @return
	 */
	public String getChannel() { return ircChannel; }
	
    /**
     * This method is called whenever a message is sent to this channel.
     *  <p>
     * The implementation of this method in the Room abstract class
     * performs no actions and may be overridden as required.
     *
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    protected void onMessage(String sender, String login,
    						 String hostname, String message) {}
    
    /**
     * This method is called whenever an ACTION is sent from a user.  E.g.
     * such events generated by typing "/me goes shopping" in most IRC clients.
     *  <p>
     * The implementation of this method in the Room abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param sender The nick of the user that sent the action.
     * @param login The login of the user that sent the action.
     * @param hostname The hostname of the user that sent the action.
     * @param target The target of the action, be it a channel or our nick.
     * @param action The action carried out by the user.
     */
    protected void onAction(String sender, String login, 
    						String hostname, String action) {}

    /**
     * This method is called whenever we receive a notice to this channel.
     *  <p>
     * The implementation of this method in the Room abstract class
     * performs no actions and may be overridden as required.
     * 
     * @param sourceNick The nick of the user that sent the notice.
     * @param sourceLogin The login of the user that sent the notice.
     * @param sourceHostname The hostname of the user that sent the notice.
     * @param notice The notice message.
     */
    protected void onNotice(String sourceNick, String sourceLogin,
    						String sourceHostname, String notice) {}
    
    /**
     * This method is called whenever someone joins a channel which we are on.
     *  <p>
     * The implementation of this method in the Room abstract class
     * performs no actions and may be overridden as required.
     *
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
    protected void onJoin(String sender, String login, String hostname) {}    
    
    /**
     * This method is called whenever someone parts this channel which we are on.
     * This is also the handler for whenever someone quits from the channel
     *  <p>
     * The implementation of this method in the Room abstract class
     * performs no actions and may be overridden as required.
     *
     * @param login The login of the user who parted from the channel.
     * @param hostname The hostname of the user who parted from the channel.
     */
    protected void onPart(String sender, String login, String hostname) {}
    
    /**
     * This method is called whenever someone changes nick on this channel.
     *  <p>
     * The implementation of this method in the Room abstract class
     * performs no actions and may be overridden as required.
     *
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The hostname of the user.
     * @param newNick The new nick.
     */
    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {}
    
    /**
     * Called when a user (possibly us) gets granted operator status for a channel.
     *
     * @param channel 		 The channel in which the mode change took place.
     * @param sourceNick 	 The nick of the user that performed the mode change.
     * @param sourceLogin 	 The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient 	 The nick of the user that got 'opped'.
     */
    protected void onOp(String sourceNick, String sourceLogin,
    					String sourceHostname, String recipient) {}
    
    /**
     * Called when a user needs to send a message back to the room
     *
     * @param timerName The type of timer that requires attention.
     */
    protected void onTimer(String timerName) {}
    
    /**
     * Sends the invalid argument message 
     * 
     * @param who		The user to send to
     * @param format	The command format
     */
    protected void invalidArguments(String who, String format) {
		ircClient.sendIRCMessage(who, Strings.InvalidArgs);
		ircClient.sendIRCMessage(who, format);		
	}
    
    /**
     * Sends a command's format message
     * 
     * @param who		The user to send to
     * @param cmd		The command
     * @param format	The command format
     */
    protected void sendFormat(String who, String cmd, String format) {
		ircClient.sendIRCMessage(who,"%b%c04 " + cmd + "%c12 - Format:" + format);		
	}
    
    /**
     * Sends a command's format followed by it's description
     * 
     * @param who		The user to send to
     * @param cmd		The command
     */
    protected void sendFullCommand(String who, CommandType cmd) {
		sendFormat( who, cmd.getCommandText(), cmd.getFormat() );
		ircClient.sendIRCMessage(who, cmd.getDescription());
	}
    
    /**
     * This method is called whenever something changes in the channel and the topic needs updating
     * @return 
     */
	protected void setTopic(String in) {		
		in = in.replaceAll("%c", "\u0003");
		in = in.replaceAll("%b", Colors.BOLD);
		in = in.replaceAll("%i", Colors.REVERSE);
		in = in.replaceAll("%u", Colors.UNDERLINE);
		in = in.replaceAll("%n", Colors.NORMAL);
		
		roomTopic = in;
		ircClient.setTopic( ircChannel, roomTopic );
	}
}
