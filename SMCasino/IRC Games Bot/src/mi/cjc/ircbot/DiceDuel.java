package mi.cjc.ircbot;

import org.pircbotx.Colors;
import org.pircbotx.PircBotX;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.pircbotx.User;

public class DiceDuel implements IRCGame {

	
	private ArrayList<String> validCommands;
	private ArrayList<Bet> openBets;
	private String MSG = Colors.BLUE;
	private String VAR = Colors.RED;
	private String BLD = Colors.BOLD;

	// id for the game as stored in the db, perhaps make this grab it from the db?
	private final int ID = 2;
	
	private String channel;
	private List<String> s_invalidBetString = (List<String>) Arrays.asList(BLD+MSG + "Error, Invalid Bet");
	
	/** Constructor
	 * @param channel	The channel the game will run on
	 */
	public DiceDuel(String channel)
	{
		validCommands = new ArrayList<String>();
		validCommands.add("dd");
		validCommands.add("call");
		validCommands.add("ddcancel");
		openBets = new ArrayList<Bet>();

		this.channel = channel;
	}
	
	/* (non-Javadoc)
	 * @see mi.cjc.ircbot.IRCGame#isValidCommand(java.lang.String)
	 */
	@Override
	public boolean isValidCommand(String command) {
		return validCommands.contains(command);
	}

	/* (non-Javadoc)
	 * @see mi.cjc.ircbot.IRCGame#processCommand(java.lang.String[], org.pircbotx.User, int, org.pircbotx.PircBotX)
	 */
	@Override
	public List<String> processCommand(String[] commands, User user,
			int userlevel, PircBotX bot) {
		String command = commands[0];
				
		String username = user.getNick();

		if(command.equalsIgnoreCase("dd"))
		{
			if(commands.length < 2) // if they have done "!dd" with nothing else
				return s_invalidBetString;
			
			// check if they already have an openbet
			for(Bet bet : openBets)
			{
				if (bet.getUser().equalsIgnoreCase(username))
					return (List<String>) Arrays.asList(BLD+VAR + username + MSG + ": You already have a wager open, Type " + VAR + "!ddcancel "+ MSG +"to cancel it");

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
			// if we make it this far, bet will be legit, so add it to open bets.
			if(amount <= 0)
				return (List<String>) Arrays.asList(BLD+MSG+"You have to bet more than 0!");
			// choice is null as DiceDuels done have one.
			if(Accounts.getInstance().checkChips(username) >= amount)
			{   // add bet, remove chips, notify channel

				Bet bet = new Bet(username, Accounts.getInstance().getActiveProfile(username),amount, "");
				openBets.add(bet);
				Accounts.getInstance().removeChips(username, Accounts.getInstance().getActiveProfile(username),amount);
				Accounts.getInstance().addBet(bet, 2);
				Accounts.getInstance().addTransaction(username, Accounts.getInstance().getActiveProfile(username),1, -amount, this.ID);
				//System.out.println("post adding bet");
				return (List<String>) Arrays.asList(BLD+VAR + username + MSG + " has opened a new dice duel wager of " + VAR + amount + MSG +" chips! To call this wager type " + VAR + "!call " + username);
			}
			else
			{
				return (List<String>) Arrays.asList(BLD+VAR + username + MSG + ": Not enough chips!");

				// not enough chips
			}
		} //</dd>
		else if (command.equalsIgnoreCase("call"))
		{
			String p1 = user.getNick(); // use rwho is calling
			String p2 = commands[1]; 
			
			// check to see if someone is playing themselves...
			if(p1.equalsIgnoreCase(p2))
				return (List<String>) Arrays.asList(BLD+VAR + username + MSG + ": You can't play against yourself!");
			for (Bet bet: openBets)
			{
				if(bet.getUser().equalsIgnoreCase(p2) && bet.isValid()) // if the bet is valid and it's the bet we are looking for
				{
					// first lock it to stop two people form calling it as this is processing
					bet.invalidate();
					
					// quick hax to check if play chips vs non-play chips!
					if(!Accounts.getInstance().getActiveProfile(p1).equalsIgnoreCase("play") && bet.getProfile().equalsIgnoreCase("play"))
					{
						bet.reset();
						return (List<String>) Arrays.asList(BLD+VAR + username + MSG + ": you need to use play chips to call a play chips dd!");
 
					}
					else if(Accounts.getInstance().getActiveProfile(p1).equalsIgnoreCase("play") && !bet.getProfile().equalsIgnoreCase("play"))
					{
						bet.reset();
						return (List<String>) Arrays.asList(BLD+VAR + username + MSG + ": you need to use play chips to call a play chips dd!");
 
					}
					
					// make sure they have enough chips to call said bet
					if(Accounts.getInstance().checkChips(p1) >= bet.getAmount())
						Accounts.getInstance().removeChips(p1, Accounts.getInstance().getActiveProfile(p1), bet.getAmount());
					else
					{
						//unlock
						bet.reset();
						return (List<String>) Arrays.asList(BLD+VAR + username + MSG + ": You don't have enough for that wager");
					}
					// play this wager
					// add a transaction for the 2nd player to call
					Accounts.getInstance().addTransaction(user.getNick(), Accounts.getInstance().getActiveProfile(p1),1, -bet.getAmount(), this.ID);

					int d1 = (TrueRandom.nextInt(6) + 1) + (TrueRandom.nextInt(6) + 1); // p1
					int d2 = (TrueRandom.nextInt(6) + 1) + (TrueRandom.nextInt(6) + 1); // p2
					while(d1 == d2) // see what he wants to do for now just reroll
					{
						d1 = (TrueRandom.nextInt(6) + 1) + (TrueRandom.nextInt(6) + 1); // p1
						d2 = (TrueRandom.nextInt(6) + 1) + (TrueRandom.nextInt(6) + 1); // p2
					}
					String winner = "";
					String loser = "";
					String profile = bet.getProfile(); // p2 profile
					if(d1 > d2) //  p1 wins
					{
						profile = Accounts.getInstance().getActiveProfile(p1);
						winner = p1;
						loser = p2;
						

					}
					else // p2 wins, use his profile
					{
						loser = p1;
						winner = p2;
					}
					int rake = 1;
					if(0.02*bet.getAmount()*2 > 1)
						rake = (int)Math.round(0.02*bet.getAmount()*2);
					Accounts.getInstance().addChips(winner, profile, (bet.getAmount()*2)-rake, null);
					
					// log everything to db
					//Accounts.getInstance().recordLoss(loser);
					//Accounts.getInstance().recordWin(winner);
					Accounts.getInstance().recordBet(p1, bet.getAmount());
					Accounts.getInstance().recordBet(p2, bet.getAmount());
					Accounts.getInstance().delBet(bet, 2);
					Accounts.getInstance().addTransaction(winner, profile, 4, (bet.getAmount()*2)-rake, this.ID);
					////bot.sendMessage(bet.getUser(), message)
					openBets.remove(bet);
					
					// logging stuff? TODO add this
					//bot.sendMessage(winner, "You wagered "+ bet.getAmount()+" on DiceDuel and won! Your chips total is "+Accounts.getInstance().checkChips(winner) + ".");
					//bot.sendMessage(loser, "You wagered "+ bet.getAmount()+" on DiceDuel and lost! Your chips total is "+Accounts.getInstance().checkChips(loser) + ".");

					return (List<String>) Arrays.asList(BLD+VAR + winner + MSG + " rolled " + VAR + (d1>d2 ? d1 : d2) + MSG + ", " + VAR + loser + MSG + " rolled " + VAR + (d1<d2 ? d1 : d2) + ". " + 
							VAR + winner + MSG + " wins the " + VAR + (bet.getAmount()*2-rake) + MSG + " chip pot!");
						
				}
				//else

			}
			// if we reach here the game doesn't exist
			return (List<String>) Arrays.asList(BLD+VAR + username + MSG + ": I can't find a record of that wager");
		}
		else if(command.equalsIgnoreCase("ddcancel"))// || command.equalsIgnoreCase("cancel"))
		{
			// try to locate and cancel the bet else ignore
			for (Bet bet : openBets)
			{
				if (bet.getUser().equalsIgnoreCase(username) && bet.isValid())
				{
					bet.invalidate();
					Accounts.getInstance().addChips(username, bet.getProfile(), bet.getAmount(), null);
					openBets.remove(bet);
					
					Accounts.getInstance().delBet(bet, 2);
					Accounts.getInstance().addTransaction(username, bet.getProfile(), 3, bet.getAmount(), this.ID);
					return (List<String>) Arrays.asList(BLD+VAR + username + MSG+ ": Cancelled your open wager");
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see mi.cjc.ircbot.IRCGame#getInfo()
	 */
	@Override
	public String getInfo() {
		return null;
	}

	/* (non-Javadoc)
	 * @see mi.cjc.ircbot.IRCGame#timerTask(int)
	 */
	@Override
	public List<String> timerTask(int taskId) {
		if (openBets.size() == 0) // no bets, no point processing them
			return new ArrayList<String>();
		else // if we have some wagers ready to be called
		{
			String open = BLD+MSG + "Current open wagers: ";
			for(Bet bet : openBets)
			{
				open += VAR + bet.getUser() + MSG + "(" + VAR + bet.getAmount() + MSG +") ";
			}
			open += MSG + "To call a wager type !call <name>";
			return (List<String>) Arrays.asList(open);
		}
	}

	/* (non-Javadoc)
	 * @see mi.cjc.ircbot.IRCGame#getTimedTasks()
	 */
	@Override
	public HashMap<Integer, Integer> getTimedTasks() {
		// task id / minutes between running it
		HashMap<Integer,Integer> retList = new HashMap<Integer,Integer>();
		retList.put(1, 3); // 1 is for annoucing games
		return retList;
	}
	
	/* (non-Javadoc)
	 * @see mi.cjc.ircbot.IRCGame#getChannel()
	 */
	@Override
	public String getChannel() {
		// TODO Auto-generated method stub
		return this.channel;
	}

}
