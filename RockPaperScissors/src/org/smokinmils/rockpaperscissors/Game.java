package org.smokinmils.rockpaperscissors;
/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.smokinmils.Database;
import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.Bet;
import org.smokinmils.database.DBException;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to give a user some chips
 * 
 * @author Jamie
 */
public class Game extends Event {
	public static final String Command = "!rps";
	public static final String Description = "%b%c12Creates a new Rock Paper Scissors game";
	public static final String Format = "%b%c12" + Command + " <amount>";
	
	public static final String CallCommand = "!rpscall";
	public static final String CallDescription = "%b%c12Calls an existing Rock Paper Scissors game";
	public static final String CallFormat = "%b%c12" + CallCommand + " <who>";	

	public static final String CxlCommand = "!rpscancel";
	public static final String CxlDescription = "%b%c12Cancels your existing Rock Paper Scissors games";
	public static final String CxlFormat = "%b%c12" + CxlCommand + " <who>";
	
	private static final String OpenWager = "%b%c04%who%c12: You already have a wager open, Type %c04" + Command + "%c12 to cancel it";
	
	private static final String ROCK = "rock";
	private static final String PAPER = "paper";
	private static final String SCISSORS = "scissors";
	private static final String[] CHOICES = {ROCK, PAPER, SCISSORS};
	
	private static Object thread_lock = new Object();
	
	private ArrayList<Bet> openBets;
	
	/**
	 * This method handles the chips command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */
	@Override
	public void message(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		Channel chan = event.getChannel();
		
		if ( isValidChannel( chan.getName() ) &&
				bot.userIsIdentified( sender ) ) {			
			if ( message.startsWith( Command ) ) {
				newGame(event);
			} else if ( message.startsWith( CallCommand ) ) {
				call(event);
			} else if ( message.startsWith( CxlCommand ) ) {
				cancel(event);
			}
		}
	}

	private void cancel(Message event) {
		
	}

	private void call(Message event) {
		
	}

	private void newGame(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		Channel chan = event.getChannel();			
		String[] msg = message.split(" ");
		
		if(msg.length == 2) {
			boolean has_open = false;
			for(Bet bet: openBets) {
				if (bet.getUser().equalsIgnoreCase(sender)) {
					has_open = true;
					break;
				}
			}
			
			if (has_open) {
				String out = OpenWager.replaceAll("%who", sender);
				bot.sendIRCNotice(sender, out);
			} else {
				Integer amount = Utils.tryParse(msg[1]);
				if (amount == null || amount == 0) {
					Database db = Database.getInstance();
					// choice is null as DiceDuels done have one.
					try {
						if(db.checkCredits(sender) >= amount) {   // add bet, remove chips, notify channel
							ProfileType profile = db.getActiveProfile(sender);
							String choice = getChoice( event.getUser(), event.getBot() );
							Bet bet = new Bet(sender, profile.toString(), amount, choice);
							openBets.add(bet);
							db.adjustChips(sender, (0-amount), profile, TransactionType.BET);
							db.addBet(sender, choice, amount, profile, GamesType.ROCKPAPERSCISSORS)
							;
							// Announce
							//return (List<String>) Arrays.asList(BLD+VAR + username + MSG + " has opened a new dice duel wager of " + VAR + amount + " "+MSG + ((bet.getProfile().equalsIgnoreCase("play")) ? "play":"real") + " chips! To call this wager type " + VAR + "!call " + username);*/
						} else {
							// No chips
						}
					} catch (Exception e) {
						EventLog.log(e, "Game", "newGame");
					}				
				} else {
					bot.invalidArguments(sender, Format);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private String getChoice(User user, IrcBot bot) {
	    WaitForQueue queue = new WaitForQueue( bot );
	    String choice = null;
		
	    boolean received = false;
	    //Infinite loop since we might receive notices from non NickServ
	    while (!received) {
	        //Use the waitFor() method to wait for a MessageEvent.
	        //This will block (wait) until a message event comes in, ignoring
	        //everything else	  
	    	PrivateMessageEvent<IrcBot> currentEvent = null;
			try {
				currentEvent = queue.waitFor(PrivateMessageEvent.class);
			} catch (InterruptedException ex) {
				EventLog.log(ex, "Game", "getChoice");
			}
			
	        //Check if this message is the response
       		String msg = currentEvent.getMessage().toLowerCase();
	        if ( currentEvent.getUser().equals(user) ) {
	        	if ( msg.equalsIgnoreCase( user.getNick() ) ) {
	        		// get and store choice		
		        	queue.close();
		        	received = true;
	        	} else {
	        		// tell user it is invalid choice.
	        	}
	        }
	    }
	    return choice;
	}
}
