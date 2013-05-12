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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.pircbotx.User;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.NoticeEvent;
import org.smokinmils.Database;
import org.smokinmils.Utils;
import org.smokinmils.bot.events.Action;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Kick;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.NickChange;
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
	/** A list of users already sent a no identification message */
	private static List<String> SentNoIdent = new ArrayList<String>();
	
	/**
	 * This string is output when the user does not meet the above status with NickServ
	 */
	public static final String NotIdentifiedMsg = "%b%c12You must be identified with %c04NickServ%c12 to use the bot commands";
	public static final String NotIdentified = "%b%c12Sorry, %c04%user%c12 is not current identified.";
	
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
			if ( event.getMessage().startsWith("!") ) {
    			// If we already told this user, don't tell them again
    			if ( !SentNoIdent.contains( user.getNick() ) ) {
        			bot.sendIRCMessage( user.getNick(), NotIdentifiedMsg );
        			SentNoIdent.add( user.getNick() );
    			}
			}
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
	public void nickChange(NickChange event) {
		event.getBot().removeIdentifiedUser( event.getOldNick() );
		sendStatusRequest( event.getBot(), event.getUser() );
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
	private void sendStatusRequest(IrcBot bot, User user) {
		Boolean identd = checkIdentified(bot, user.getNick());
		// Only add users with the correct levels
    	if (identd == null) {
    	    bot.sendIRCMessage(user.getNick(), "%b%c12The IRC Network Services appear to be down, please try again shortly.");
    	} else if (identd) {
    		EventLog.info(user.getNick() + " identified", "CheckIdentified", "sendStatusRequest");
    		bot.addIdentifiedUser( user.getNick() );
        	SentNoIdent.remove( user.getNick() );
    		try {
				boolean created = Database.getInstance().checkUserExists( user.getNick(), user.getHostmask() );
				if (!created) {
					bot.sendIRCMessage(user.getNick(), "%b%c12You have too many accounts, speak to an admin if there is a problem");
				}
			} catch (Exception e) {
				EventLog.log(e, "CheckIdentified", "sendStatusRequest");
			}
	    }
    }
    
    /**
     * Sends a request to NickServ to check a user's status with the server
     * and waits for the response.
     * 
     * @param bot the IRC bot
     * @param user the username
     * 
     * @return true if the user meets the required status
     */
	public static boolean checkIdentified(IrcBot bot, String user) {
        Boolean ret = null;        
        ExecutorService executor = Executors.newFixedThreadPool(1);
        FutureTask<Boolean> choicetask = new FutureTask<Boolean>( new CheckUser(bot, user) );
        executor.execute(choicetask);
        try {
            ret = choicetask.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Do nothing, we expect this.
            ret = null;
        } catch (InterruptedException | ExecutionException e) {
            EventLog.log(e, "CheckIdentified", "checkIdentified");
        }   
        executor.shutdown();
        
        return ret;
    }
}

class CheckUser implements Callable<Boolean> {
    /** A string to store the response from a nickserv status command */
    private static final String NickServStatus = "STATUS";
    
    /** A string to store username of nickserv */
    private static final String NickServ = "NickServ";
    
    /** The required nickserv status value */
    private static final int RequiredStatus = 3;
    
    private IrcBot Bot;
    private String User;
    
    public CheckUser(IrcBot bot, String user) {
        Bot = bot;
        User = user;
    }

    @SuppressWarnings("unchecked")
    public Boolean call() {
        EventLog.debug("Checking the status of " + User, "CheckUser::CheckIdentified", "call");
        
        WaitForQueue queue = new WaitForQueue( Bot );
        Bot.sendRawLine( "PRIVMSG NickServ " + NickServStatus + " " + User );
        
        boolean received = false;
        Boolean ret = null;
        //Infinite loop since we might receive notices from non NickServ
        while (!received) {
            //Use the waitFor() method to wait for a MessageEvent.
            //This will block (wait) until a message event comes in, ignoring
            //everything else     
            NoticeEvent<IrcBot> currentEvent = null;
            try {
                currentEvent = queue.waitFor(NoticeEvent.class);
            } catch (InterruptedException ex) {
                EventLog.log(ex, "CheckUser::CheckIdentified", "call");
            }
            
            //Check if this message is the response
            String[] msg = currentEvent.getMessage().split(" ");
            if ( currentEvent.getMessage().startsWith(NickServStatus)
                 && currentEvent.getUser().getNick().compareToIgnoreCase(NickServ) == 0
                 && msg.length == 3
                 && msg[1].compareToIgnoreCase( User ) == 0 ) {
                Integer code = Utils.tryParse( msg[2] );
                
                // Only add users with the correct levels
                if (code >= RequiredStatus) {
                    EventLog.info(User + " identified", "CheckUser::CheckIdentified", "call");
                    ret = true;
                }
                queue.close();
                received = true;
            }
        }
        return ret;
    }
}
