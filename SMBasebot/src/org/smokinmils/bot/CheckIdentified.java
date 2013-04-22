/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

import org.pircbotx.User;
import org.pircbotx.hooks.WaitForQueue;
import org.smokinmils.Utils;
import org.smokinmils.bot.events.Action;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Kick;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.Notice;
import org.smokinmils.bot.events.Part;
import org.smokinmils.bot.events.PrivateMessage;
import org.smokinmils.bot.events.Quit;
import org.smokinmils.bot.events.UserList;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to automatically verify users.
 * 
 * @author Jamie
 */
public class CheckIdentified extends Event {
	/** A string to store the response from a nickserv status command */
	private static final String NickServStatus = "STATUS";
	
	/** A string to store username of nickserv */
	private static final String NickServ = "NickServ";
	
	/** The required nickserv status value */
	private static final int RequiredStatus = 3;
	
	@Override
	public void onAction(Action event) {
		IrcBot bot = event.getBot();
		String user = event.getUser().getNick();
		if ( !bot.userIsIdentified( user ) ) {
			sendStatusRequest( bot, user );
		}
	}
	
	@Override
	public void onMessage(Message event) {
		IrcBot bot = event.getBot();
		String user = event.getUser().getNick();
		if ( !bot.userIsIdentified( user ) ) {
			sendStatusRequest( bot, user );
		}
	}
	
	@Override
	public void onPrivateMessage(PrivateMessage event) {
		IrcBot bot = event.getBot();
		String user = event.getUser().getNick();
		if ( !bot.userIsIdentified( user ) ) {
			sendStatusRequest( bot, user );
		}
	}
	
	@Override
	public void onJoin(Join event) {
		String user = event.getUser().getNick();
		// (Re-)Check the user's status with NickServ
		if (user.compareToIgnoreCase( event.getBot().getNick() ) != 0) {
			event.getBot().removeIdentifiedUser( event.getUser().getNick() );
			sendStatusRequest( event.getBot(), user );
		}
	}
	
	@Override
	public void onPart(Part event) {
		event.getBot().removeIdentifiedUser( event.getUser().getNick() );
	}
	
	@Override
	public void onKick(Kick event) {
		event.getBot().removeIdentifiedUser( event.getRecipient().getNick() );
	}	
	
	@Override
	public void onQuit(Quit event) {
		event.getBot().removeIdentifiedUser( event.getUser().getNick() );
	}
	
	@Override
	public void onUserList(UserList event) {
		IrcBot bot = event.getBot();
		for (User usr: event.getUsers()) {
			bot.removeIdentifiedUser( usr.getNick() );
			sendStatusRequest( bot, usr.getNick() );
		}
	}
    
    /**
     * Sends a request to NickServ to check a user's status with the server
     * and waits for the response.
     * 
     * @param bot the IRC bot
     * @param user the username
     */
    private void sendStatusRequest(IrcBot bot, String user) {
		bot.sendRawLine( "PRIVMSG NickServ STATUS " + user );
	    EventLog.debug("Checking the status of " + user, "CheckIdentified", "sendStatusRequest");
		
	    WaitForQueue queue = new WaitForQueue( bot );
	    boolean received = false;
	    //Infinate loop since we might receive messages that aren't WaitTest's.
	    while (!received) {
	        //Use the waitFor() method to wait for a MessageEvent.
	        //This will block (wait) until a message event comes in, ignoring
	        //everything else	  
	    	Notice currentEvent = null;
			try {
				currentEvent = queue.waitFor(Notice.class);
			} catch (InterruptedException e) {
				EventLog.fatal(e, "CheckIdentified", "sendStatusRequest");
				// TODO: system.exit(0) here?
			}
			
	        //Check if this message is the response
       		String[] msg = currentEvent.getMessage().split(" ");
	        if ( currentEvent.getMessage().startsWith(NickServStatus)
	        	 && currentEvent.getUser().getNick().compareToIgnoreCase(NickServ) == 0
	        	 && msg.length == 3
	        	 && msg[2].compareToIgnoreCase( user ) == 0 ) {
    			Integer code = Utils.tryParse( msg[2] );
    			
    			// Only add users with the correct levels
    			if (code >= RequiredStatus) {
    				EventLog.info(user + " identified", "CheckIdentified", "sendStatusRequest");
    				bot.addIdentifiedUser( user );
    			}
	        	queue.close();
	        	received = true;
	        }
	    }
    }
}
