package org.smokinmils.casino;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.smokinmils.bot.Bet;

public class Roulette implements IRCGame {

	private String MSG = Colors.BLUE;
	private String VAR = Colors.RED;
	private String BLD = Colors.BOLD;
	
	ArrayList<String> validCommands;
	
	private List<String> s_invalidBetString = (List<String>) Arrays.asList(BLD + MSG + "\"!bet "+VAR+"<amount> <choice>"+MSG+"\" You have entered an invalid choice. Please enter "+VAR+"black"+MSG+", "+VAR+"red"+MSG+", "+VAR+"1-36"+MSG+", "+VAR+"1st, "+VAR+"2nd, "+VAR+"3rd, "+VAR+"even"+MSG+" or "+VAR+"odd"+MSG+" as your choice.");
	
	
	// to contain the list of bets
	private ArrayList<Bet> colourBets;
	private ArrayList<Bet> rowBets;
	private ArrayList<Bet> numberBets;
	private ArrayList<Bet> evenOddBets;
	
	private PircBotX bot;
	private int delay;
	private String channel;
	
	public final int OPEN = 0;
	public final int CLOSE = 1;
	public int state;
	
	private static Object locked = new Object();
	
	public Roulette(int delay, String channel, PircBotX bot)
	{
		//TODO add in stuff for ranks, and loading stuff from file :)
		this.delay = delay;
		this.bot = bot;
		//random = new Random();
		validCommands = new ArrayList<String>();
		validCommands.add("bet");
		validCommands.add("cancel");

		// temporary debugging commands -- or, not left in
		validCommands.add("board");
		validCommands.add("end");
		
		// instantiate the bets and try to load them from the db
		colourBets = new ArrayList<Bet>();
		rowBets = new ArrayList<Bet>();
		numberBets = new ArrayList<Bet>();
		evenOddBets = new ArrayList<Bet>();
		
		// assign channel
		this.channel = channel;
		
		this.state = OPEN;
	}
	
	@Override
	public boolean isValidCommand(String command) {
		return (validCommands.contains(command));
	}

	@Override
	public synchronized List<String> processCommand(String[] commands, User user, int userlevel, PircBotX bot) {
		// Process the command
		synchronized(locked)
		{
			locked = true;
			String command = commands[0];
			
			String username = user.getNick();
			
			if(command.equalsIgnoreCase("bet"))
			{
				// if we are not accepting bets
				if(this.state == CLOSE)
				{
					locked = false;

					return (List<String>) Arrays.asList(BLD +MSG + "Bets are now closed, please wait for the next round!" );
				}
				if(commands.length < 3) // if they have done "!bet" with nothing else
				{
					locked = false;
					return s_invalidBetString;
				
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
				
				// check they have enough to bet before even attempting to parse the actual bet itself
				if(Accounts.getInstance().checkChips(username) < amount)
				{
					locked = false;

					return (List<String>) Arrays.asList(BLD +MSG + "You do not have enough chips for that!" );
				}
				//check for negative bets / 0 bets
				if(amount <= 0)
				{			
					locked = false;

					return (List<String>) Arrays.asList(BLD+MSG + "You have to bet more than 0!");
				}
					// check if they are betting on red or black
				String choice = commands[2].toLowerCase();
				
				if(choice.equalsIgnoreCase("red") || choice.equalsIgnoreCase("black"))
				{
					
					Accounts.getInstance().removeChips(username, Accounts.getInstance().getActiveProfile(username), amount);
					Bet bet = new Bet(username, Accounts.getInstance().getActiveProfile(username), amount, choice);
					colourBets.add(bet);
					Accounts.getInstance().addBet(bet, 3); // 
					Accounts.getInstance().addTransaction(username,Accounts.getInstance().getActiveProfile(username),1, -amount, 3);
					locked = false;

					return (List<String>) Arrays.asList(BLD+VAR + username +MSG+": You have bet " +VAR + amount +MSG+ " on " + VAR+choice);
				}
				else if(choice.equalsIgnoreCase("1st") || choice.equalsIgnoreCase("2nd") || choice.equalsIgnoreCase("3rd"))
				{
					Accounts.getInstance().removeChips(username, Accounts.getInstance().getActiveProfile(username), amount);
					Bet bet = new Bet(username, Accounts.getInstance().getActiveProfile(username), amount, choice);
					rowBets.add(bet);
					Accounts.getInstance().addBet(bet, 5);
					Accounts.getInstance().addTransaction(username, Accounts.getInstance().getActiveProfile(username),1, -amount, 5);
					locked = false;

					return (List<String>) Arrays.asList(BLD+VAR+username +MSG+": You have bet " +VAR+ amount +MSG+ " on the " +VAR+ choice +MSG+ " row" );
				}
				else if(choice.equalsIgnoreCase("even") || choice.equalsIgnoreCase("odd") )
				{
					Accounts.getInstance().removeChips(username, Accounts.getInstance().getActiveProfile(username), amount);
					Bet bet = new Bet(username, Accounts.getInstance().getActiveProfile(username), amount, choice);
					evenOddBets.add(bet);
					Accounts.getInstance().addBet(bet, 6);
					Accounts.getInstance().addTransaction(username, Accounts.getInstance().getActiveProfile(username),1, -amount, 6);
					locked = false;

					return (List<String>) Arrays.asList(BLD+VAR+username +MSG+": You have bet " + VAR+amount + MSG+" on " +VAR+ choice);
				}
				// if we get this far the bet is either invalid, (which should never reach due to checking in the bot,
				// or we are betting on a number, so let's try to 
					
				int bet;
				try
				{
					bet = Integer.parseInt(choice);
				}
				catch (Exception e)
				{
					// not a number, lets just return an invalid bet
					locked = false;
					return s_invalidBetString;
				}
				// check range for bets
				if (bet < 1 || bet > 36)
				{
					locked = false;

					return s_invalidBetString;
				}
				else
				{
					Accounts.getInstance().removeChips(username, Accounts.getInstance().getActiveProfile(username), amount);
					Bet bett = new Bet(username, Accounts.getInstance().getActiveProfile(username), amount, choice);
					// bett?  need to sort variable names
					numberBets.add(bett);
					Accounts.getInstance().addBet(bett, 4);
					Accounts.getInstance().addTransaction(username,Accounts.getInstance().getActiveProfile(username), 1, -amount, 4);
					locked = false;

					return (List<String>) Arrays.asList(BLD+VAR+username +MSG+": You have bet " + VAR+amount + MSG+" on the number " + VAR+bet);
				}
			}// </bet>
			else if(command.equalsIgnoreCase("cancel"))
			{
				for (Bet bet : numberBets)
				{
					if(bet.isValid() && bet.getUser().equalsIgnoreCase(username))
					{
						bet.invalidate();
						Accounts.getInstance().delBet(bet, 4);
						Accounts.getInstance().addChips(username, bet.getProfile(), bet.getAmount(), user);
						Accounts.getInstance().addTransaction(username, bet.getProfile(),3, bet.getAmount(), 4);
						
					}
				}
				for (Bet bet : colourBets)
				{
					if(bet.isValid() && bet.getUser().equalsIgnoreCase(username))
					{
						bet.invalidate();
						Accounts.getInstance().delBet(bet, 3);
						Accounts.getInstance().addChips(username, bet.getProfile(), bet.getAmount(), user);
						Accounts.getInstance().addTransaction(username, bet.getProfile(),3, bet.getAmount(), 3);
					}
				}
				for (Bet bet : rowBets)
				{
					if(bet.isValid() && bet.getUser().equalsIgnoreCase(username))
					{
						bet.invalidate();
						Accounts.getInstance().delBet(bet, 5);
						Accounts.getInstance().addChips(username, bet.getProfile(),bet.getAmount(), user);
						Accounts.getInstance().addTransaction(username, bet.getProfile(),3, bet.getAmount(), 5);
					}
				}
				for (Bet bet : evenOddBets)
				{
					if(bet.isValid() && bet.getUser().equalsIgnoreCase(username))
					{
						bet.invalidate();
						Accounts.getInstance().delBet(bet, 6);
						Accounts.getInstance().addChips(username, bet.getProfile(), bet.getAmount(), user);
						Accounts.getInstance().addTransaction(username, bet.getProfile(),3, bet.getAmount(), 6);
						
					}
				}
				locked = false;

				return (List<String>) Arrays.asList(BLD+MSG+"All bets cancelled for " +VAR+ username);
			}
			else if (command.equalsIgnoreCase("end"))
			{
				// call end function
				if(userlevel > 0)
				{
					locked = false;

					return this.endGame(bot);
				}
				else
				{
					locked = false;

					return (List<String>) Arrays.asList(BLD+MSG+"You don't have the required permissions for that");
				}
			}
			// these are debug commands to be removed later on... maybe
			else if (command.equalsIgnoreCase("board"))
			{
				locked = false;

				return printBoard();
			}
			locked = false;
			return null;
		}
		
	}

	@Override
	public String getInfo() {
		return "Test Instructions";
	}

	//  for setting colours
	private List<String> printBoard()
	{
		return (List<String>) Arrays.asList("0,3 00 0,1 28 0,4 09 0,1 26 0,4 30 0,1 11 0,4 07 0,1 20 0,4 32 0,1 17 0,4 05 0,1 22 0,4 34 0,1", 
				          "0,3 00 0,4 15 0,1 03 0,4 24 0,1 36 0,4 13 0,1 01 0,4 27 0,1 10 0,4 25 0,1 29 0,4 12 0,1 08 0,1", 
				          "0,3 00 0,1 19 0,4 31 0,1 18 0,4 06 0,1 21 0,4 33 0,1 16 0,4 04 0,1 23 0,4 35 0,1 14 0,4 02 0,1");
		//return board;
	}
	
	private String getColour(int number)
	{
		if (number == 28 | number == 26 |number == 11 |number == 20 |number == 17 |
				number == 22 |number == 3 |number == 36 |number == 1 |
				number == 10 |number == 29 |number == 8 |number == 19 |
				number == 18 |number == 21 |number == 16 |number == 23 |
				number == 14 )
		{
			return "black";
		}
		else if (number == 0)
		{
			return "green";
		}
		else
			return "red";
	}
	
	private String getRow(int number)
	{
		if(number == 28 | number == 9 | number == 26 | 
				number == 30 | number == 11 | number == 7 | 
				number == 20 | number == 32 | number == 17 | 
				number == 5 | number == 22 | number == 34)
			return "1st";
		else if(number == 15 | number == 3 | number == 24 | 
				number == 36 | number == 13 | number == 1 | 
				number == 27 | number == 10 | number == 25 | 
				number == 29 | number == 12 | number == 8)
			return "2nd";
		else
			return "3rd";
	}
	
	/**
	 * Ends the game and prints out winners (move to arraylist as per usual)
	 * @return winners / win info
	 */
	private List<String> endGame(PircBotX bot)
	{
		// TODO move this to timer function
		// Let's "roll"
		int winner = TrueRandom.nextInt(39);
		if (winner > 36)
			winner = 0;
		
		List<String> nameList = new ArrayList<String>();
		
		// number bets
		for (Bet bet : numberBets)
		{
			if (Integer.parseInt(bet.getChoice()) == winner && bet.isValid()&& winner !=0)
			{
				if (!nameList.contains(bet.getUser()))
					nameList.add(bet.getUser());
				Accounts.getInstance().addChips(bet.getUser(), bet.getProfile(), bet.getAmount() * 36, null);
				//Accounts.getInstance().recordWin(bet.getUser());
				Accounts.getInstance().recordBet(bet.getUser(), bet.getAmount());
				Accounts.getInstance().addTransaction(bet.getUser(), bet.getProfile(),4, bet.getAmount()*36, 4);
				//bot.sendMessage(bet.getUser(), "You bet "+ bet.getAmount()+" on Roulette and won! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + ".");

				
			}
			else if (bet.isValid())
			{
				// the bet didn't win, so tell them they lost (hah loser!)
				//bot.sendMessage(bet.getUser(), "You bet "+ bet.getAmount()+" on Roulette and lost! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + ".");

			}
			Accounts.getInstance().delBet(bet, 4);
		}
		// colour bets
		for (Bet bet : colourBets)
		{
			if (bet.getChoice().equalsIgnoreCase(this.getColour(winner)) && bet.isValid()&& winner !=0)
			{
				if (!nameList.contains(bet.getUser()))
					nameList.add(bet.getUser());
				Accounts.getInstance().addChips(bet.getUser(), bet.getProfile(), bet.getAmount() * 2, null);
				Accounts.getInstance().recordWin(bet.getUser());
				Accounts.getInstance().recordBet(bet.getUser(), bet.getAmount());
				Accounts.getInstance().addTransaction(bet.getUser(), bet.getProfile(),4, bet.getAmount()*2, 3);
				//bot.sendMessage(bet.getUser(), "You bet "+ bet.getAmount()+" on Roulette and won! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + ".");

				
			}
			else if (bet.isValid())
			{
				// the bet didn't win, so tell them they lost (hah loser!)
				//bot.sendMessage(bet.getUser(), "You bet "+ bet.getAmount()+" on Roulette and lost! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + ".");

			}
			Accounts.getInstance().delBet(bet, 3);
		}
		// row bets
		
		for (Bet bet : rowBets)
		{
			if (bet.getChoice().equalsIgnoreCase(this.getRow(winner)) && bet.isValid() && winner !=0)
			{
				if (!nameList.contains(bet.getUser()))
					nameList.add(bet.getUser());
				Accounts.getInstance().addChips(bet.getUser(), bet.getProfile(), bet.getAmount() * 3, null);
				Accounts.getInstance().recordWin(bet.getUser());
				Accounts.getInstance().recordBet(bet.getUser(), bet.getAmount());
				Accounts.getInstance().addTransaction(bet.getUser(), bet.getProfile(), 4, bet.getAmount()*3, 5);
				//bot.sendMessage(bet.getUser(), "You bet "+ bet.getAmount()+" on Roulette and won! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + ".");

			}
			else if (bet.isValid())
			{
				// the bet didn't win, so tell them they lost (hah loser!)
				//bot.sendMessage(bet.getUser(), "You bet "+ bet.getAmount()+" on Roulette and lost! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + ".");

			}
			Accounts.getInstance().delBet(bet, 5);

		}
		for (Bet bet : evenOddBets)
		{
			
			if (bet.isValid())
			{
				int mod = 1;
				if(bet.getChoice().equalsIgnoreCase("even"))
					mod = 0;
				if(winner % 2 == mod && winner != 0)
				{
					if (!nameList.contains(bet.getUser()))
						nameList.add(bet.getUser());
					Accounts.getInstance().addChips(bet.getUser(), bet.getProfile(), bet.getAmount() * 2, null);
					Accounts.getInstance().recordWin(bet.getUser());
					Accounts.getInstance().recordBet(bet.getUser(), bet.getAmount());
					Accounts.getInstance().addTransaction(bet.getUser(), bet.getProfile(),4, bet.getAmount()*2, 6);
					//bot.sendMessage(bet.getUser(), "You bet "+ bet.getAmount()+" on Roulette and won! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + ".");

				}
				else if (bet.isValid())
				{
					// the bet didn't win, so tell them they lost (hah loser!)
					//bot.sendMessage(bet.getUser(), "You bet "+ bet.getAmount()+" on Roulette and lost! Your chips total is "+Accounts.getInstance().checkChips(bet.getUser()) + ".");

				}
			}
			Accounts.getInstance().delBet(bet, 6);

		}
		
		// clear the bets
		colourBets = new ArrayList<Bet>();
		rowBets = new ArrayList<Bet>();
		numberBets = new ArrayList<Bet>();
		evenOddBets = new ArrayList<Bet>();
		
		List<String> retList = new ArrayList<String>();
		//Construct the winning string 
		String winningString = BLD+MSG+"The winning number is: " + VAR + winner + " " + MSG + this.getColour(winner);
		if(this.getColour(winner).equalsIgnoreCase("red"))
			winningString += " 0,4 ";
		else if(this.getColour(winner).equalsIgnoreCase("black"))
			winningString += " 0,1 ";
		else
			winningString += " 0,3 ";
		winningString += winner + " 0,4";
		retList.add(winningString);
		String names = "";
		if(nameList.size() > 0)
		{
			// tag them all on
			for(String user : nameList)
			{
				names += user + " ";
			}
			retList.add(BLD+MSG + "Congratulations to " +VAR+ names);
		}
		retList.add(BLD+MSG+"A new roulette game is starting! Type "+VAR+"!info "+MSG+"for instructions on how to play.");
		retList.addAll(this.printBoard());
		retList.add(BLD+MSG+"Place your bets now!");
		
		this.state = OPEN;
		
		return retList;

	}
		

	@Override
	public List<String> timerTask(int taskId) {
		if(taskId == 1)
		{
			return this.endGame(this.bot);
		}
		if(taskId == 2)
		{
			this.state = CLOSE;
			return (List<String>) Arrays.asList(BLD +MSG + "Bets closed! No more bets. Spinning..." );
		}
		// dirty
		return new ArrayList<String>();
	}

	@Override
	public HashMap<Integer,Integer> getTimedTasks() {
		// task id / minutes between running it
		HashMap<Integer,Integer> retList = new HashMap<Integer,Integer>();
		retList.put(1, this.delay); // 1 is for ending game / starting a new game
		retList.put(2, -this.delay);
		return retList; 
	}

	@Override
	public String getChannel() {
		// TODO Auto-generated method stub
		return this.channel;
	}
}
