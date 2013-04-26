package org.smokinmils.casino;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import org.smokinmils.pokerbot.Database;
import org.smokinmils.bot.Bet;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;
import org.smokinmils.pokerbot.settings.Strings;

public class OverUnder implements IRCGame {
	


	private String MSG = Colors.BLUE;
	private String VAR = Colors.RED;
	private String BLD = Colors.BOLD;
	
	private final int ID = 1;
	
	private ArrayList<String> validCommands;
	private ArrayList<Bet> openBets;
	private List<String> s_invalidBetString = (List<String>) Arrays.asList(BLD+VAR + "\"!ou <amount> <choice>\" " + MSG + "You have entered an invalid choice. Please use " + VAR +"over" + MSG +", " + VAR +"under " + MSG +"or " + VAR +"7");
	private String channel;
	
	//private Random random;

	private static Object locked = new Object();
	
	public OverUnder(String channel)
	{
		
		// sort out the valid commands
		validCommands = new ArrayList<String>();
		validCommands.add("ouroll");
		validCommands.add("ou");
		validCommands.add("oucancel");
		
		// initialize openBets
		openBets = new ArrayList<Bet>();
		
		this.channel = channel;
	}

	@Override
	public boolean isValidCommand(String command) {
		// TODO Auto-generated method stub
		return (validCommands.contains(command));
	}

	@Override
	public synchronized List<String> processCommand(String[] commands, User user,
			int userlevel, PircBotX bot) {
		
		
			// TODO Auto-generated method stub
			// Process the command
			String command = commands[0];
			Accounts db = Accounts.getInstance();
			
			if(command.equalsIgnoreCase("ouroll"))
			{
				// iterate through bets to find it, if we do run it
				
				for(Bet bet : openBets)
				{
					
					if (bet.isValid() && bet.getUser().equalsIgnoreCase(user.getNick()))
					{
						
						List<String> retList = new ArrayList<String>();
						
						// generate some die rolls
						int d1 = TrueRandom.nextInt(6) + 1;//random.nextInt(6) + 1; // oh lord
						int d2 = TrueRandom.nextInt(6) + 1;//random.nextInt(6) + 1;
						int total = d1 + d2;
						
						// record the bet now since they have rolled and can't back out ;)
						
						db.recordBet(bet.getUser(), bet.getAmount());
						if(this.doesBetWin(bet, total))
						{
	
							int winnings = 2 * bet.getAmount();
							if (bet.getChoice().equalsIgnoreCase("7"))
								winnings = 5 * bet.getAmount();
							// they win pay out and add string
							db.addChips(user.getNick(), bet.getProfile(), winnings, null);
							db.recordWin(user.getNick());
							db.addTransaction(user.getNick(), bet.getProfile(), 4, winnings, this.ID);
							
							retList.add(BLD+MSG + "Rolling... " +VAR+ total +MSG+ ". Congratulations on your win " +VAR+ user.getNick() + MSG+"!");
							
						}
						else
						{
							// didn't win, 
							retList.add(BLD+MSG + "Rolling... " +VAR+ total + MSG+ ". Better luck next time " +VAR+ user.getNick() + MSG+ "!");
							// bonus roll ONLY ON LOSSES
							double r = 0;
							if (bet.getChoice().equalsIgnoreCase("7"))
								r = 0.84;
							else
								r = 0.75;
							if(Math.random() > r )
							{
								
								d1 = TrueRandom.nextInt(6) + 1;// // oh lord
								d2 = TrueRandom.nextInt(6) + 1;//
								total = d1 + d2;
								if(this.doesBetWin(bet, total))
								{
									int winnings =2 * bet.getAmount();
									if (bet.getChoice().equalsIgnoreCase("7"))
										winnings = 5 * bet.getAmount();
									// they win pay out and add string
									db.addChips(user.getNick(), bet.getProfile(), winnings, null);
									db.addTransaction(user.getNick(), bet.getProfile(), 4, winnings, this.ID);
									
									retList.add(BLD+MSG + "BONUS ROLL! Rolling... " +VAR+ total + MSG+ ". Congratulations on your win " +VAR+ user.getNick() + MSG+ "!");
								}
								else
								{
									
									retList.add(BLD+MSG + "BONUS ROLL! Rolling... " +VAR+ total +MSG+ ". Better luck next time " + user.getNick() + "!");
								}
							}
							
						}
						
						// remove the bet
						bet.invalidate();
						openBets.remove(bet);
						db.delBet(bet, 1);
						
						// do jackpot
						if(bet.getAmount() >= 50)
						{
							if(OverUnder.checkJackpot())
							{
								// we win
								ArrayList<String> winners = new ArrayList<String>();
								winners.add(bet.getUser());
								this.jackpotWon(bet.getProfile(), winners, bot);
							}
							else
							{
								// we lose
								int rake = (int)Math.floor(bet.getAmount()/100*Settings.OURAKE);
								OverUnder.updateJackpot(rake, bet.getProfile());
							}
						}	
						return (List<String>) retList;
					}
				}
				
				return (List<String>) Arrays.asList(BLD+MSG +user.getNick() + ": No bet found for you, ");
			}
			else if(command.equalsIgnoreCase("ou"))
			{
				// make sure they don't have an open bet otherwise let them know to roll or cancel.
				
				if(commands.length < 3) // if they have done "!ou" with nothing else
				{
					
					return s_invalidBetString;
				}
				for(Bet bet : openBets)
				{
					if (bet.getUser().equalsIgnoreCase(user.getNick()))
					{
						// They already have a bet open, and as such, tell them to roll instead
						
						return (List<String>) Arrays.asList(BLD+VAR+user.getNick() + MSG+": You already have an open bet, type "+VAR+"!ouroll"+MSG+" to roll!");
					}
				}
				// attempt to parse the amount
				int amount;
				
				try {
					amount = Integer.parseInt(commands[1]); // we need some exception handling here
				}
				catch(Exception e)
				{
					
					return s_invalidBetString;
				}
				
				// check the bet isn't 0 or less :)
				if(amount <= 0)
				{
					
					return (List<String>) Arrays.asList(BLD+MSG + "You have to bet more than 0!");
				}
				String choice = commands[2];
				if(choice.equalsIgnoreCase("over") || choice.equalsIgnoreCase("under") || choice.equalsIgnoreCase("7"))
				{
	
					// valid choice check chips
					if(db.checkChips(user.getNick()) < amount)
					{
						
						return (List<String>) Arrays.asList(BLD+MSG + "You do not have enough chips for that!" );
					}
					else
					{
						Bet bet = new Bet(user.getNick(), db.getActiveProfile(user.getNick()), amount, choice);
						openBets.add(bet);
						db.removeChips(user.getNick(), db.getActiveProfile(user.getNick()),amount);
						db.addBet(bet, 1); //add to table for refunds
						db.addTransaction(user.getNick(), db.getActiveProfile(user.getNick()), 1, -bet.getAmount(), this.ID);
						return (List<String>) Arrays.asList(BLD+VAR +user.getNick() +MSG+ " has bet " +VAR+ amount +MSG+ " on " +VAR+ choice +MSG+ "! " +VAR+ user.getNick() +MSG+ " type "+VAR+"!ouroll"+MSG+" to roll");
					}
				}
				else
				{
					
					return s_invalidBetString;
				}
			}
			else if(command.equalsIgnoreCase("oucancel"))
			{
				for(Bet bet : openBets)
				{
					
					if (bet.isValid() && bet.getUser().equalsIgnoreCase(user.getNick()))
					{
						bet.invalidate();
						db.delBet(bet, 1);
						db.addChips(bet.getUser(), bet.getProfile(), bet.getAmount(), null);
						db.addTransaction(user.getNick(), bet.getProfile(), 3, bet.getAmount(), this.ID);
	
						openBets.remove(bet);
						
						return (List<String>) Arrays.asList(BLD+VAR + user.getNick() + MSG+ ": Cancelled your open OverUnder wager");
					}
				}
			}
			return null;
		
	}

	@Override
	public String getInfo() {
		// TODO Auto-generated method stub DEPRECATED!
		return "Test Instructions";
	}

	@Override
	public List<String> timerTask(int taskId) {
		// No Timed tasks for this game
		return null;
	}

	@Override
	public HashMap<Integer, Integer> getTimedTasks() {
		// no Timed Tasks for this game
		return null;
	}

	private boolean doesBetWin(Bet bet, int total)
	{
		if (bet.getChoice().equalsIgnoreCase("7"))
		{
			if(total == 7)
			{
				return true;
				
			}
			
		}
		else if (bet.getChoice().equalsIgnoreCase("under"))
		{
			if(total < 7)
			{
				return true;
			}
		}
		else if (bet.getChoice().equalsIgnoreCase("over"))
		{
			if(total > 7)
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public String getChannel() {
		return this.channel;
	}
	
	
	/**
	 * Save the new jackpot value
	 */
	private static synchronized boolean updateJackpot(int rake, String profile) {
		
		boolean added = false;
		int jackpot = Database.getInstance().getJackpot(profile);

		int incrint = rake;
		
		EventLog.log(profile + " jackpot: " + Integer.toString(jackpot) + " + "
					 + Integer.toString(incrint) + " (" + Integer.toString(rake) + ")",
					 "OverUnder", "updateJackpot");
		
		if (incrint > 0) {
			added = true;
			jackpot += incrint;
			// Announce to lobbyChan
			//String out = Strings.JackpotIncreased.replaceAll("%chips", Integer.toString(jackpot));
			//out = out.replaceAll("%profile", profile);
			//irc.sendIRCMessage(out);
			 
			Database.getInstance().updateJackpot(profile, incrint);
		}
		return added;
	}
	
	/**
	 * Check if the jackpot has been won
	 */
	private static synchronized boolean checkJackpot() {
		return (TrueRandom.nextInt(Settings.JACKPOTCHANCE + 1) == Settings.JACKPOTCHANCE);		
	}
	
	/**
	 * Jackpot has been won, split between all players on the table
	 */
	private void jackpotWon(String profileName, ArrayList<String> players, PircBotX bot) {
		int jackpot = Database.getInstance().getJackpot(profileName);
		 
		if (jackpot > 0) {
			int remainder = jackpot % players.size();
			jackpot -= remainder;
			
			if (jackpot != 0) {
				int win = jackpot;// / players.size();
				for (String player: players) {
					Database.getInstance().jackpot(player, win, profileName);
				}
				
				// Announce to channel
					
				String out = Strings.JackpotWon.replaceAll("%chips", Integer.toString(jackpot));
				out = out.replaceAll("%profile", profileName);
				out = out.replaceAll("%winners", players.toString());
				
				bot.sendMessage(this.channel, out);
				bot.sendMessage(this.channel, out);
				bot.sendMessage(this.channel, out);
				/*
				 * ircClient.sendIRCMessage(out);
				ircClient.sendIRCMessage(out);
				ircClient.sendIRCMessage(out);
				
				// Announce to table
				out = Strings.JackpotWonTable.replaceAll("%chips", Integer.toString(win));
				out = out.replaceAll("%profile", profileName);
				out = out.replaceAll("%winners", jackpotPlayers.toString());
				ircClient.sendIRCMessage(ircChannel, out);
				ircClient.sendIRCMessage(ircChannel, out);
				ircClient.sendIRCMessage(ircChannel, out);
				
				// Update jackpot with remainder
				if (remainder > 0) {
					out = Strings.JackpotIncreased.replaceAll("%chips", Integer.toString(remainder));
					out = out.replaceAll("%profile", profileName);
					//ircClient.sendIRCMessage(out);
				}*/
				Database.getInstance().updateJackpot(profileName, remainder);
			
			}
		}

	}
}
