package org.smokinmils.casino;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

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
		
		synchronized(locked)
		{
			locked = true;
			// TODO Auto-generated method stub
			// Process the command
			String command = commands[0];
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
						
						Accounts.getInstance().recordBet(bet.getUser(), bet.getAmount());
						if(this.doesBetWin(bet, total))
						{
	
							int winnings = 2 * bet.getAmount();
							if (bet.getChoice().equalsIgnoreCase("7"))
								winnings = 5 * bet.getAmount();
							// they win pay out and add string
							Accounts.getInstance().addChips(user.getNick(), bet.getProfile(), winnings, null);
							Accounts.getInstance().recordWin(user.getNick());
							Accounts.getInstance().addTransaction(user.getNick(), bet.getProfile(), 4, winnings, this.ID);
							
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
									Accounts.getInstance().addChips(user.getNick(), bet.getProfile(), winnings, null);
									Accounts.getInstance().addTransaction(user.getNick(), bet.getProfile(), 4, winnings, this.ID);
									/* -- Logging needs to be redone <-- TODO
									if(Accounts.getInstance().checkNonActiveChips(user.getNick()).containsKey(bet.getProfile()))
										bot.sendMessage(user.getNick(), "You bet "+ bet.getAmount()+" on OverUnder and won! Your chips total is "+Accounts.getInstance().checkNonActiveChips(user.getNick()).get(bet.getProfile()) + " on account " + bet.getProfile()+".");
									else
									{
										// otherwise we are in the default profile
										bot.sendMessage(user.getNick(), "You bet "+ bet.getAmount()+" on OverUnder and won! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + " on account " + bet.getProfile()+".");
	
									}*/
									retList.add(BLD+MSG + "BONUS ROLL! Rolling... " +VAR+ total + MSG+ ". Congratulations on your win " +VAR+ user.getNick() + MSG+ "!");
								}
								else
								{
									/* //Accounts.getInstance().recordLoss(user.getNick());
									if(Accounts.getInstance().checkNonActiveChips(user.getNick()).containsKey(bet.getProfile()))
										bot.sendMessage(user.getNick(), "You bet "+ bet.getAmount()+" on OverUnder and lost! Your chips total is "+Accounts.getInstance().checkNonActiveChips(user.getNick()).get(bet.getProfile()) + " on account " + bet.getProfile()+".");
									else
									{
										// otherwise we are in the default profile
										bot.sendMessage(user.getNick(), "You bet "+ bet.getAmount()+" on OverUnder and lost! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + " on account " + bet.getProfile()+".");
	
									}*/
									retList.add(BLD+MSG + "BONUS ROLL! Rolling... " +VAR+ total +MSG+ ". Better luck next time " + user.getNick() + "!");
								}
							}
							else
							{   /*
								// we didn't win, we didn't get a bonus roll
								if(Accounts.getInstance().checkNonActiveChips(user.getNick()).containsKey(bet.getProfile()))
									bot.sendMessage(user.getNick(), "You bet "+ bet.getAmount()+" on OverUnder and lost! Your chips total is "+Accounts.getInstance().checkNonActiveChips(user.getNick()).get(bet.getProfile()) + " on account " + bet.getProfile()+".");
								else
								{
									// otherwise we are in the default profile
									bot.sendMessage(user.getNick(), "You bet "+ bet.getAmount()+" on OverUnder and lost! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + " on account " + bet.getProfile()+".");
	
								}*/
							}
							
						}
						
						// remove the bet
						bet.invalidate();
						openBets.remove(bet);
						Accounts.getInstance().delBet(bet, 1);
						
						locked = false;
						return (List<String>) retList;
					}
				}
				locked = false;
				return (List<String>) Arrays.asList(BLD+MSG +user.getNick() + ": No bet found for you, ");
			}
			else if(command.equalsIgnoreCase("ou"))
			{
				// make sure they don't have an open bet otherwise let them know to roll or cancel.
				
				if(commands.length < 3) // if they have done "!ou" with nothing else
				{
					locked = false;
					return s_invalidBetString;
				}
				for(Bet bet : openBets)
				{
					if (bet.getUser().equalsIgnoreCase(user.getNick()))
					{
						// They already have a bet open, and as such, tell them to roll instead
						locked = false;
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
					locked = false;
					return s_invalidBetString;
				}
				
				// check the bet isn't 0 or less :)
				if(amount <= 0)
				{
					locked = false;
					return (List<String>) Arrays.asList(BLD+MSG + "You have to bet more than 0!");
				}
				String choice = commands[2];
				if(choice.equalsIgnoreCase("over") || choice.equalsIgnoreCase("under") || choice.equalsIgnoreCase("7"))
				{
	
					// valid choice check chips
					if(Accounts.getInstance().checkChips(user.getNick()) < amount)
					{
						locked = false;
						return (List<String>) Arrays.asList(BLD+MSG + "You do not have enough chips for that!" );
					}
					else
					{
						Bet bet = new Bet(user.getNick(), Accounts.getInstance().getActiveProfile(user.getNick()), amount, choice);
						openBets.add(bet);
						Accounts.getInstance().removeChips(user.getNick(), Accounts.getInstance().getActiveProfile(user.getNick()),amount);
						Accounts.getInstance().addBet(bet, 1); //add to table for refunds
						Accounts.getInstance().addTransaction(user.getNick(), Accounts.getInstance().getActiveProfile(user.getNick()), 1, -bet.getAmount(), this.ID);
						locked = false;
						return (List<String>) Arrays.asList(BLD+VAR +user.getNick() +MSG+ " has bet " +VAR+ amount +MSG+ " on " +VAR+ choice +MSG+ "! " +VAR+ user.getNick() +MSG+ " type "+VAR+"!ouroll"+MSG+" to roll");
					}
				}
				else
				{
					locked = false;
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
						Accounts.getInstance().delBet(bet, 1);
						Accounts.getInstance().addChips(bet.getUser(), bet.getProfile(), bet.getAmount(), null);
						Accounts.getInstance().addTransaction(user.getNick(), bet.getProfile(), 3, bet.getAmount(), this.ID);
	
						openBets.remove(bet);
						locked = false;
						return (List<String>) Arrays.asList(BLD+VAR + user.getNick() + MSG+ ": Cancelled your open OverUnder wager");
					}
				}
			}
			locked = false;
			return null;
		}
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
}
