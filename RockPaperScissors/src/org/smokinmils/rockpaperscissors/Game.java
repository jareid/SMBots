package org.smokinmils.rockpaperscissors;
/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pircbotx.Channel;
import org.smokinmils.Database;
import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.Bet;
import org.smokinmils.database.types.ProfileType;
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
					if(db.checkCredits(sender) >= amount) {   // add bet, remove chips, notify channel
						String profile = db.getActiveProfile(sender).toString();
						Bet bet = new Bet(sender, profile, amount, "");
						openBets.add(bet);
						/*db.removeChips(sender, profile, amount);
						db.addBet(bet, 2);
						db.addTransaction(sender, profile,1 , -amount, this.ID);
						//System.out.println("post adding bet");
						return (List<String>) Arrays.asList(BLD+VAR + username + MSG + " has opened a new dice duel wager of " + VAR + amount + " "+MSG + ((bet.getProfile().equalsIgnoreCase("play")) ? "play":"real") + " chips! To call this wager type " + VAR + "!call " + username);*/
					} else {
						// No chips
					}				
				} else {
					bot.invalidArguments(sender, Format);
				}
			}
		}
	}
}
