package org.smokinmils.games.rockpaperscissors;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.pircbotx.Channel;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.smokinmils.SMBaseBot;
import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.Bet;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to give a user some chips
 * 
 * @author Jamie
 */
public class RPSGame extends Event {
	public static final String Command = "!rps";
	public static final String Description = "%b%c12Creates a new Rock Paper Scissors game";
	public static final String Format = "%b%c12" + Command + " <amount>";
	
	public static final String CallCommand = "!rpscall";
	public static final String CallDescription = "%b%c12Calls an existing Rock Paper Scissors game";
	public static final String CallFormat = "%b%c12" + CallCommand + " <who>";	

	public static final String CxlCommand = "!rpscancel";
	public static final String CxlDescription = "%b%c12Cancels your existing Rock Paper Scissors games";
	public static final String CxlFormat = "%b%c12" + CxlCommand + " <who>";
	
	private static final String OpenWager = "%b%c04%who%c12: You already have a wager open, Type %c04" + CxlCommand + "%c12 to cancel it";
	private static final String OpenedWager = "%b%c04%who%c12: has opened a new RPS wager of %c04%amount%c12 %profile chips! To call this wager type %c04" + CallCommand + " %who";
	private static final String CancelledWager = "%b%c04%who%c12: Cancelled your open wager";
	private static final String NoChips = "%b%c12Sorry, you do not have %c04%chips%c12 chips available for the %c04%profile%c12 profile.";
	private static final String RealChipsOnly = "%b%c04%who%c12: : you need to use %c04%profile%c12 chips to call a %c04%profile%c12 chips rps!";
	private static final String NoBet = "%b%c04%who%c12: I can't find a record of that wager";
	private static final String SelfBet =  "%b%c04%who%c12: You can't play against yourself!";
	private static final String Win = "%b%c12%winstring. %c04%loser%c12 loses and %c04%winner%c12 wins %c04%chips%c12!";
	private static final String Draw = "%b%c04%better%c12 and %c04%caller%c12 draw with %c04%choice%c12! Attempting to replay...";
	private static final String ReplayFail = "%b%c12Replay between %c04%better%c12 and %c04%caller%c12 failed as %c04%who%c12 didn't respond. Both users have been refunded %c04%chips%c12 chips!";
	private static final String ValidChoices = "%b%c04%who%c12: Please choose an option and enter it here. Valid choices are: %c04%choices%c12!";
	private static final String PleaseChoose = "%b%c12You have received a query asking for your choice. Please send your choice in the query and not in this channel.";
	private static final String ValidChoice = "%b%c12You have chosen %c04%choice%c12!";
	private static final String InvalidChoice = "%b%c04%what%c12 is invalid. Valid choices are: %c04%choices%c12!";
	private static final String NoChoice = "%b%c04%who%c12: You didn't make a choice for the game, the bet has been cancelled.";
	private static final String OpenBets = "%c12%bCurrent open RPS wagers: %bets To call a wager type %c04" + CallCommand + " <name>";
	private static final String EachOpenBet = "%c04%user%c12(%c04%amount %profile%c12)";
	
	private static final int AnnounceMins = 3;

	private List<Bet> openBets;
	
	/**
	 * Constructor
	 * 
	 * @param chan The channel where jackpots are announce
	 */
	public RPSGame() {
		openBets = new ArrayList<Bet>();
	}
	
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
		synchronized (SMBaseBot.lockObject) {
			if ( isValidChannel( chan.getName() ) &&
					bot.userIsIdentified( sender ) ) {			
				if ( message.startsWith( CxlCommand ) ) {
					cancel(event);
				} else if ( message.startsWith( CallCommand ) ) {
					call(event);
				} else if ( message.startsWith( Command ) ) {
					newGame(event);
				}
			}
		}
	}

	private void cancel(Message event) {
		// try to locate and cancel the bet else ignore
		String username = event.getUser().getNick();
		for (Bet bet: openBets) {
			if (bet.getUser().equalsIgnoreCase(username) && bet.isValid()) {
				DB db = DB.getInstance();
				bet.invalidate();
				try {
					db.adjustChips(username, bet.getAmount(),bet.getProfile(),
									GamesType.ROCKPAPERSCISSORS,
									TransactionType.CANCEL);

					db.deleteBet(username, GamesType.ROCKPAPERSCISSORS);
					openBets.remove(bet);
				} catch (Exception e) {
					EventLog.log(e, "RPSGame", "cancel");
				}
				// Announce
				event.getBot().sendIRCMessage(event.getChannel(),
						CancelledWager.replaceAll("%who", username));
				break;
			}
		}
	}

	private void call(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		String caller = event.getUser().getNick();
		Channel chan = event.getChannel();			
		String[] msg = message.split(" ");
		
		if(msg.length == 2) {
			String better = msg[1];

			// check to see if someone is playing themselves...
			if (better.equalsIgnoreCase(caller)) {
				bot.sendIRCMessage(chan, SelfBet.replaceAll("%who", caller));
			} else {
				DB db = DB.getInstance();
				boolean found = false;
				for (Bet bet : openBets) {
					if (bet.getUser().equalsIgnoreCase(better) && bet.isValid()) {
						try {
							found = true;
							ProfileType caller_prof = db.getActiveProfile(caller);
							ProfileType better_prof = bet.getProfile();
							int amount = bet.getAmount();
							
							// first lock it to stop two people form calling it as this
							// is processing -- Shouldn't be possible with thread
							// locking now
							bet.invalidate();

							// quick hax to check if play chips vs non-play chips!
							if (caller_prof != better_prof) {
								bet.reset();
								String out = RealChipsOnly.replaceAll("%who", caller);
								out = out.replaceAll("%profile", better_prof.toString());
								bot.sendIRCMessage(chan, out);
							} else if (db.checkCredits(caller) < amount) {
								bet.reset();
								String out = NoChips.replaceAll( "%chips", Integer.toString(amount));
								out = out.replaceAll( "%profile", caller_prof.toString() );
								bot.sendIRCMessage(chan, out);
							} else {
								GameLogic choice = getChoice( event.getUser().getNick(), event.getBot() );
								if (choice != null) {									
									db.deleteBet(better, GamesType.ROCKPAPERSCISSORS);
									openBets.remove(bet);
									db.adjustChips(caller, (0-amount), caller_prof, 
											   GamesType.ROCKPAPERSCISSORS, TransactionType.BET);
									
									GameLogic better_choice = GameLogic.fromString(bet.getChoice());
									GameLogic caller_choice = choice;
									
									endGame(better, better_prof, better_choice, 
											caller, caller_prof, caller_choice,
											amount, bot, chan.getName());
								} else {
									bet.reset();
									bot.sendIRCMessage(chan, NoChoice.replaceAll("%who", event.getUser().getNick()));
								}
							}
							// Found the bet so we can exit.
							break;
						} catch (Exception e) {
							EventLog.log(e, "RPSGame", "call");
						}				
					}		
				}

				if (!found) {
					// if we reach here the game doesn't exist
					String out = NoBet.replaceAll("%who", caller);
					bot.sendIRCMessage(chan, out);
				}
			}
		}
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
				if (amount != null && amount != 0) {
					DB db = DB.getInstance();
					// choice is null as DiceDuels done have one.
					try {
						ProfileType profile = db.getActiveProfile(sender);
						if(db.checkCredits(sender) >= amount) {
							// add bet, remove chips, notify channel
							GameLogic choice = getChoice( event.getUser().getNick(), event.getBot() );
							if (choice != null) {
								Bet bet = new Bet(sender, profile, amount, choice.toString());
								openBets.add(bet);
								db.adjustChips(sender, (0-amount), profile, 
											   GamesType.ROCKPAPERSCISSORS, TransactionType.BET);
								db.addBet(sender, choice.toString(), amount, profile, GamesType.ROCKPAPERSCISSORS);
								
								String out = OpenedWager.replaceAll("%who", sender);
								out = out.replaceAll("%profile", profile.toString());
								out = out.replaceAll("%amount", Integer.toString(amount));
								bot.sendIRCMessage(chan, out);
							} else {
								bot.sendIRCMessage(chan, NoChoice.replaceAll("%who", event.getUser().getNick()));
							}
						} else {
							String out = NoChips.replaceAll( "%chips", Integer.toString(amount));
							out = out.replaceAll( "%profile", profile.toString() );
							bot.sendIRCMessage(chan, out);
						}
					} catch (Exception e) {
						EventLog.log(e, "RPSGame", "newGame");
					}				
				} else {
					bot.invalidArguments(sender, Format);
				}
			}
		} else {
			bot.invalidArguments(sender, Format);
		}
	}
	
	private GameLogic getChoice(String user, IrcBot bot) {
	    GameLogic choice = null;		
	    ExecutorService executor = Executors.newFixedThreadPool(1);
	    FutureTask<GameLogic> choicetask = new FutureTask<GameLogic>( new GetChoice(bot, user) );
	    executor.execute(choicetask);
		try {
			choice = choicetask.get(25, TimeUnit.SECONDS);
			bot.sendIRCMessage(user, ValidChoice.replaceAll("%choice", choice.toString()));
		} catch (TimeoutException e) {
			// Do nothing, we expect this.
			choice = null;
		} catch (InterruptedException | ExecutionException e) {
			EventLog.log(e, "RPSGame", "getChoice");
		}	
	    executor.shutdown();
	    
		return choice;
	}
	
	private void endGame(String better, ProfileType better_prof, GameLogic better_choice,
			   			 String caller, ProfileType caller_prof, GameLogic caller_choice,
			   			 int amount, IrcBot bot, String chan) {
		GameLogicComparator c = new GameLogicComparator();
		int order = c.compare(better_choice, caller_choice);
		String winstr = c.getWinString();
		
		if (order == -1) {
			// better won
			doWin(better, better_prof, better_choice, 
					caller, caller_prof, caller_choice,
					amount, winstr, bot, chan);
		} else if (order == 1) {
			// caller won
			doWin(caller, caller_prof, caller_choice, 
					better, better_prof, better_choice,
					amount, winstr, bot, chan);
		} else {
			doDraw(better, better_prof,
				   caller, caller_prof,
				   amount, bot, chan, caller_choice);
		}
	}
	
	private void doWin(String winner, ProfileType win_prof, GameLogic win_choice,
					   String loser, ProfileType lose_prof, GameLogic lose_choice,
					   int amount, String winstring, IrcBot bot, String chan) {
		DB db = DB.getInstance();
		// Take the rake and give chips to winner
		double rake = Rake.getRake(winner, amount, win_prof) + Rake.getRake(loser, amount, lose_prof);
		double win = (amount*2) - rake;
		
		try {
			db.adjustChips(winner, win, win_prof, 
					   GamesType.ROCKPAPERSCISSORS, TransactionType.WIN);
			
			//Announce winner and give chips			
			String out = Win.replaceAll("%winstring", winstring);
			out = out.replaceAll("%winner", winner);
			out = out.replaceAll("%loser", loser);
			out = out.replaceAll("%chips", Integer.toString( (int)Math.floor(win) ) );
			bot.sendIRCMessage(chan, out);
		} catch (Exception e) {
			EventLog.log(e, "RPSGame", "updateJackpot");
		}
		
		// jackpot stuff	
		if (Rake.checkJackpot()) {
			ArrayList<String> players = new ArrayList<String>();
			players.add(winner);
			players.add(loser);
			Rake.jackpotWon(win_prof, GamesType.ROCKPAPERSCISSORS,
							players, bot, chan);
		}
	}
	
	private void doDraw(String better, ProfileType better_prof,
						String caller, ProfileType caller_prof,						
  			 			int amount, IrcBot bot, String chan, GameLogic choice) {
		//Announce winner and give chips			
		String out = Draw.replaceAll("%choice", choice.toString());
		out = out.replaceAll("%better", better);
		out = out.replaceAll("%caller", caller);
		bot.sendIRCMessage(chan, out);
		
		boolean cxld = false;
		String who = "";
		GameLogic b_choice = getChoice(better, bot);
		if (b_choice != null) {
			GameLogic c_choice = getChoice(caller, bot);
			if (c_choice != null) {
				endGame(caller, caller_prof, c_choice, 
						better, better_prof, b_choice,
						amount, bot, chan);
			} else {
				who = caller;
				cxld = true;
			}
		} else {
			who = better;
			cxld = true;
		}
		
		if (cxld) {
			DB db = DB.getInstance();
			// cancel bets.
			try {
				db.adjustChips(better, amount, better_prof,
							   GamesType.ROCKPAPERSCISSORS,
							   TransactionType.CANCEL);
				
				for (Bet bet: openBets) {
					if (bet.getUser().equalsIgnoreCase(better) && bet.isValid()) {
						bet.invalidate();
						break;
					}
				}
				
				db.adjustChips(caller, amount, caller_prof,
						   GamesType.ROCKPAPERSCISSORS,
						   TransactionType.CANCEL);

				String fail = ReplayFail.replaceAll("%better", better);
				fail = fail.replaceAll("%caller", caller);
				fail = fail.replaceAll("%who", who);
				fail = fail.replaceAll("%chips", Integer.toString(amount) );
				bot.sendIRCMessage(chan, fail);
			} catch (Exception e) {
				EventLog.log(e, "RPSGame", "doDraw");
			}
		}
	}
	
	/**
	 * Adds a channel that announces open bets
	 * @param channel the channel name
	 * @parma bot the IRC bot object
	 */
	public void addAnnounce(String channel, IrcBot bot) {
		Timer chan_timer = new Timer(true);
		chan_timer.scheduleAtFixedRate( new OpenBetsAnnounce(bot, channel), 
										500, AnnounceMins*1000*60 );
	}
	
	class GetChoice implements Callable<GameLogic> {
		private IrcBot Bot;
		private String User;
		
		public GetChoice(IrcBot bot, String user) {
			Bot = bot;
			User = user;
		}
		
        @SuppressWarnings("unchecked")
		public GameLogic call() {
			GameLogic choice = null;
			WaitForQueue queue = new WaitForQueue( Bot );
			boolean received = false;
			String choices = Arrays.asList(GameLogic.values()).toString();
			choices = choices.substring(1, choices.length()-1);
			
			Bot.sendIRCMessage(User,
					ValidChoices.replaceAll("%choices", choices).replaceAll("%who", User));
			Bot.sendIRCNotice(User, PleaseChoose);
			
    	    //Loop until we receive the correct message
    	    while (!received) {
    	        //Use the waitFor() method to wait for a MessageEvent.
    	        //This will block (wait) until a message event comes in, ignoring
    	        //everything else
    	    	PrivateMessageEvent<IrcBot> currentEvent = null;
    	    	try {
    				currentEvent = queue.waitFor(PrivateMessageEvent.class);
    			} catch (InterruptedException ex) {
    				EventLog.log(ex, "RPSGame", "getChoice");
    			}
    			
    	        //Check if this message is the response
           		String msg = currentEvent.getMessage().toLowerCase();
    	        if ( currentEvent.getUser().getNick().equalsIgnoreCase(User) ) {
            		// get and store choice
            		choice = GameLogic.fromString(msg);
            		if (choice == null) {
            			String out = InvalidChoice.replaceAll("%what", msg);
            			out = out.replaceAll("%choices", Arrays.asList(GameLogic.values()).toString());
            			Bot.sendIRCMessage(User, out);
            		} else {
    		        	queue.close();
    		        	received = true;
            		}
    	        }
    	    }
			return choice;
        }
	}

	public class OpenBetsAnnounce extends TimerTask {
		private IrcBot Bot;
		private String Channel;

		public OpenBetsAnnounce(IrcBot bot, String channel) {
			this.Bot = bot;
			this.Channel = channel;
		}

		public void run() {
			if (openBets.size() > 0) {
				String bets = "";
				for (Bet bet : openBets) {
					String betstr = EachOpenBet.replaceAll("%user", bet.getUser());
					betstr = betstr.replaceAll("%amount", Integer.toString(bet.getAmount()));
					betstr = betstr.replaceAll("%profile", bet.getProfile().toString());
					bets += betstr;
				}
				String out = OpenBets.replaceAll("%bets", bets);
				Bot.sendIRCMessage(Channel, out);
			}
		}
	}
}
