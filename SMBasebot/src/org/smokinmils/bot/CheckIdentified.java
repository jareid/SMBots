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
import org.pircbotx.hooks.events.NoticeEvent;
import org.smokinmils.Database;
import org.smokinmils.Utils;
import org.smokinmils.bot.events.Action;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Kick;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.Part;
import org.smokinmils.bot.events.PrivateMessage;
import org.smokinmils.bot.events.Quit;
import org.smokinmils.bot.events.UserList;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to automatically verify users.
 * Also, gives a player play chips if they don't exist
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
	public void action(Action event) {
		IrcBot bot = event.getBot();
		User user = event.getUser();
		if ( !bot.userIsIdentified( user.getNick() ) ) {
			sendStatusRequest( bot, user );
		}
	}
	
	@Override
	public void message(Message event) {
		IrcBot bot = event.getBot();
		User user = event.getUser();
		if ( !bot.userIsIdentified( user.getNick() ) ) {
			sendStatusRequest( bot, user );
		}
	}
	
	@Override
	public void privateMessage(PrivateMessage event) {
		IrcBot bot = event.getBot();
		User user = event.getUser();
		if ( !bot.userIsIdentified( user.getNick() ) ) {
			sendStatusRequest( bot, user );
		}
	}
	
	@Override
	public void join(Join event) {
		User user = event.getUser();
		// (Re-)Check the user's status with NickServ
		if (user.getNick().compareToIgnoreCase( event.getBot().getNick() ) != 0) {
			event.getBot().removeIdentifiedUser( user.getNick() );
			sendStatusRequest( event.getBot(), user );
		}
	}
	
	@Override
	public void part(Part event) {
		event.getBot().removeIdentifiedUser( event.getUser().getNick() );
	}
	
	@Override
	public void kick(Kick event) {
		event.getBot().removeIdentifiedUser( event.getRecipient().getNick() );
	}	
	
	@Override
	public void quit(Quit event) {
		event.getBot().removeIdentifiedUser( event.getUser().getNick() );
	}
	
	@Override
	public void userList(UserList event) {
		IrcBot bot = event.getBot();
		for (User usr: event.getUsers()) {
			bot.removeIdentifiedUser( usr.getNick() );
			sendStatusRequest( bot, usr );
		}
	}
    
    /**
     * Sends a request to NickServ to check a user's status with the server
     * and waits for the response.
     * 
     * @param bot the IRC bot
     * @param user the username
     */
    @SuppressWarnings("unchecked")
	private void sendStatusRequest(IrcBot bot, User user) {
	    EventLog.debug("Checking the status of " + user.getNick(), "CheckIdentified", "sendStatusRequest");
	    
	    WaitForQueue queue = new WaitForQueue( bot );
		bot.sendRawLine( "PRIVMSG NickServ STATUS " + user.getNick() );
		
	    boolean received = false;
	    //Infinite loop since we might receive notices from non NickServ
	    while (!received) {
	        //Use the waitFor() method to wait for a MessageEvent.
	        //This will block (wait) until a message event comes in, ignoring
	        //everything else	  
	    	NoticeEvent<IrcBot> currentEvent = null;
			try {
				currentEvent = queue.waitFor(NoticeEvent.class);
			} catch (InterruptedException ex) {
				EventLog.log(ex, "CheckIdentified", "sendStatusRequest");
			}
			
	        //Check if this message is the response
       		String[] msg = currentEvent.getMessage().split(" ");
	        if ( currentEvent.getMessage().startsWith(NickServStatus)
	        	 && currentEvent.getUser().getNick().compareToIgnoreCase(NickServ) == 0
	        	 && msg.length == 3
	        	 && msg[1].compareToIgnoreCase( user.getNick() ) == 0 ) {
    			Integer code = Utils.tryParse( msg[2] );
    			
    			// Only add users with the correct levels
    			if (code >= RequiredStatus) {
    				EventLog.info(user + " identified", "CheckIdentified", "sendStatusRequest");
    				bot.addIdentifiedUser( user.getNick() );
    				try {
    					Database.getInstance().checkUserExists( user.getNick(), user.getHostmask() );
    				} catch (Exception e) {
    					EventLog.log(e, "CheckIdentified", "sendStatusRequest");
    				}
    			}
	        	queue.close();
	        	received = true;
	        }
	    }
    }
}
