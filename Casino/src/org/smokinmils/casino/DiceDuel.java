package org.smokinmils.casino;

import org.pircbotx.Colors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.pircbotx.User;
import org.smokinmils.bot.Bet;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.cashier.Rake;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.Database;
import org.smokinmils.pokerbot.Utils;

public class DiceDuel implements IRCGame {

	private ArrayList<String> validCommands;
	private ArrayList<Bet> openBets;
	private String MSG = Colors.BLUE;
	private String VAR = Colors.RED;
	private String BLD = Colors.BOLD;

	// id for the game as stored in the db, perhaps make this grab it from the
	// db?
	private final int ID = 2;

	private String channel;
	private List<String> s_invalidBetString = (List<String>) Arrays.asList(BLD
			+ MSG + "Error, Invalid Bet");

	/**
	 * Constructor
	 * 
	 * @param channel
	 *            The channel the game will run on
	 */
	public DiceDuel(String channel) {
		validCommands = new ArrayList<String>();
		validCommands.add("dd");
		validCommands.add("call");
		validCommands.add("ddcancel");
		openBets = new ArrayList<Bet>();

		this.channel = channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mi.cjc.ircbot.IRCGame#isValidCommand(java.lang.String)
	 */
	@Override
	public boolean isValidCommand(String command) {
		return validCommands.contains(command);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mi.cjc.ircbot.IRCGame#processCommand(java.lang.String[],
	 * org.pircbotx.User, int, org.pircbotx.PircBotX)
	 */
	@Override
	public List<String> processCommand(String[] commands, User user, int userlevel, IrcBot bot) {
		String command = commands[0];

		String username = user.getNick();
		Accounts db = Accounts.getInstance();
		Database db_new = Database.getInstance();

		if (command.equalsIgnoreCase("dd")) {
			dd(commands, user, userlevel, bot);
		} else if (command.equalsIgnoreCase("call")) {
			call(commands, user, userlevel, bot);
		} else if ( command.equalsIgnoreCase("ddcancel") ){
			cancel(commands, user, userlevel, bot);
		} else {
			// TODO: something went wrong... LOG THIS AND CRY!
			return null;
		}
	}
	
	private List<String> dd(String[] commands, User user, int userlevel, IrcBot bot) {
		List<String> ret;
		String username = user.getNick();

		Database db_new = Database.getInstance();
		
		if (commands.length < 2) {
			ret = s_invalidBetString;
		} else {		
			// check if they already have an openbet
			for (Bet bet : openBets) {
				if (bet.getUser().equalsIgnoreCase(username))
					return (List<String>) Arrays.asList(BLD + VAR + username
								+ MSG + ": You already have a wager open, Type "
								+ VAR + "!ddcancel " + MSG + "to cancel it");
			
			}
			
			// attempt to parse the amount
			Integer amount = Utils.tryParse(commands[1]);
			if (amount == null) {
				ret = s_invalidBetString;
			} else if (amount <= 0) {
				ret = (List<String>) Arrays.asList(BLD + MSG + "You have to bet more than 0!");
			} else {
			// choice is null as DiceDuels done have one.
			if (db_new.checkCredits(username) >= amount) { // add bet, remove chips,
											// notify channel
				ProfileType profile = db_new.getActiveProfile(username);
				Bet bet = new Bet(username, profile.toString(), amount, "");
				openBets.add(bet);
				db_new.adjustChips(username, profile, (0-amount));
				db.addBet(bet, 2);
				db.addTransaction(username, profile, 1, -amount, this.ID);
				// System.out.println("post adding bet");
				ret = (List<String>) Arrays.asList(BLD + VAR+ username	+ MSG
													+ " has opened a new dice duel wager of "
													+ VAR + amount + " " + MSG
													+ ((bet.getProfile().equalsIgnoreCase("play")) ? "play"
															: "real") + " chips! To call this wager type "
													+ VAR + "!call " + username);
			} else {
				ret = (List<String>) Arrays.asList(BLD + VAR + username + MSG + ": Not enough chips!");
			}
		}
		
		return ret;
	}

	private List<String> call(String[] commands, User user, int userlevel, IrcBot bot) {
		List<String> ret;
		String username = user.getNick();

		Database db_new = Database.getInstance();
		String p1 = user.getNick(); // use rwho is calling
		String p2 = commands[1];

		// check to see if someone is playing themselves...
		if (p1.equalsIgnoreCase(p2))
			ret = (List<String>) Arrays.asList(BLD + VAR + username + MSG
					+ ": You can't play against yourself!");
		for (Bet bet : openBets) {
			if (bet.getUser().equalsIgnoreCase(p2) && bet.isValid()) // if
																		// the
																		// bet
																		// is
																		// valid
																		// and
																		// it's
																		// the
																		// bet
																		// we
																		// are
																		// looking
																		// for
			{
				// first lock it to stop two people form calling it as this
				// is processing -- Shouldn't be possible with thread
				// locking now
				bet.invalidate();

				// quick hax to check if play chips vs non-play chips!
				if (!db.getActiveProfile(p1).equalsIgnoreCase("play")
						&& bet.getProfile().equalsIgnoreCase("play")) {
					bet.reset();
					return (List<String>) Arrays
							.asList(BLD
									+ VAR
									+ username
									+ MSG
									+ ": you need to use play chips to call a play chips dd!");

				} else if (db.getActiveProfile(p1).equalsIgnoreCase("play")
						&& !bet.getProfile().equalsIgnoreCase("play")) {
					bet.reset();
					return (List<String>) Arrays
							.asList(BLD
									+ VAR
									+ username
									+ MSG
									+ ": you need to use real chips to call a real chips dd!");

				}

				// make sure they have enough chips to call said bet
				if (db_new.checkCredits(p1) >= bet.getAmount())
					db.removeChips(p1, db.getActiveProfile(p1),
							bet.getAmount());
				else {
					// unlock
					bet.reset();
					return (List<String>) Arrays.asList(BLD + VAR
							+ username + MSG
							+ ": You don't have enough for that wager");
				}
				// play this wager
				// add a transaction for the 2nd player to call
				String p1Profile = db.getActiveProfile(p1);
				String p2Profile = bet.getProfile();

				db.addTransaction(p1, p1Profile, 1, -bet.getAmount(),
						this.ID);

				int d1 = (TrueRandom.nextInt(6) + 1)
						+ (TrueRandom.nextInt(6) + 1); // p1
				int d2 = (TrueRandom.nextInt(6) + 1)
						+ (TrueRandom.nextInt(6) + 1); // p2
				while (d1 == d2) // see what he wants to do for now just
									// reroll
				{
					d1 = (TrueRandom.nextInt(6) + 1)
							+ (TrueRandom.nextInt(6) + 1); // p1
					d2 = (TrueRandom.nextInt(6) + 1)
							+ (TrueRandom.nextInt(6) + 1); // p2
				}
				String winner = "";
				String loser = "";
				String winnerProfile = "";
				String loserProfile = "";
				if (d1 > d2) { // p1 wins
					winner = p1;
					loser = p2;
					winnerProfile = p1Profile;
					loserProfile = p2Profile;
				} else { // p2 wins, use his profile
					loser = p1;
					winner = p2;
					loserProfile = p1Profile;
					winnerProfile = p2Profile;
				}
				
				double rake = Rake.getRake(winner, bet.getAmount(), ProfileType.fromString(winnerProfile)) + 
							  Rake.getRake(loser, bet.getAmount(), ProfileType.fromString(loserProfile));
				double win = ((bet.getAmount() * 2) - rake);
				db.addChips(winner, winnerProfile, win, null);

				// log everything to db
				db.recordBet(p1, bet.getAmount());
				db.recordBet(p2, bet.getAmount());
				db.delBet(bet, 2);
				db.addTransaction(winner, winnerProfile, 4, win, this.ID);
				// //bot.sendMessage(bet.getUser(), message)
				openBets.remove(bet);

				// jackpot stuff
				if (p1Profile.equalsIgnoreCase(p2Profile) && Rake.checkJackpot()) {
						ArrayList<String> players = new ArrayList<String>();
						players.add(p1);
						players.add(p2);
						Rake.jackpotWon(ProfileType.fromString(p1Profile),
								 		GamesType.DICE_DUEL, players, bot, null);
				} else {
					if (Rake.checkJackpot()) { // loser first? Let's be nice
						ArrayList<String> players = new ArrayList<String>();
						players.add(loser);
						Rake.jackpotWon(ProfileType.fromString(loserProfile),
						 		GamesType.DICE_DUEL, players, bot, null);
					} else if (Rake.checkJackpot()) {
						ArrayList<String> players = new ArrayList<String>();
						players.add(winner);
						Rake.jackpotWon(ProfileType.fromString(winnerProfile),
						 		GamesType.DICE_DUEL, players, bot, null);
					}
				}

				return (List<String>) Arrays.asList(BLD + VAR + winner
						+ MSG + " rolled " + VAR + (d1 > d2 ? d1 : d2)
						+ MSG + ", " + VAR + loser + MSG + " rolled " + VAR
						+ (d1 < d2 ? d1 : d2) + ". " + VAR + winner + MSG
						+ " wins the " + VAR + (bet.getAmount() * 2 - rake)
						+ MSG + " chip pot!");

			}

		}
		// if we reach here the game doesn't exist
		return (List<String>) Arrays.asList(BLD + VAR + username + MSG
				+ ": I can't find a record of that wager");
	}
	
	private List<String> cancel(String[] commands, User user, int userlevel, IrcBot bot) {
		List<String> ret;
		String username = user.getNick();

		Database db_new = Database.getInstance();
		// try to locate and cancel the bet else ignore
		for (Bet bet : openBets) {
			if (bet.getUser().equalsIgnoreCase(username) && bet.isValid()) {
				bet.invalidate();
				db.addChips(username, bet.getProfile(), bet.getAmount(),
						null);
				openBets.remove(bet);

				db.delBet(bet, 2);
				db.addTransaction(username, bet.getProfile(), 3,
						bet.getAmount(), this.ID);
				return (List<String>) Arrays.asList(BLD + VAR + username
						+ MSG + ": Cancelled your open wager");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mi.cjc.ircbot.IRCGame#getInfo()
	 */
	@Override
	public String getInfo() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mi.cjc.ircbot.IRCGame#timerTask(int)
	 */
	@Override
	public List<String> timerTask(int taskId) {
		if (openBets.size() == 0) // no bets, no point processing them
			return new ArrayList<String>();
		else // if we have some wagers ready to be called
		{
			String open = BLD + MSG + "Current open wagers: ";
			for (Bet bet : openBets) {
				open += VAR
						+ bet.getUser()
						+ MSG
						+ "("
						+ VAR
						+ bet.getAmount()
						+ MSG
						+ " "
						+ VAR
						+ ((bet.getProfile().equalsIgnoreCase("play")) ? "play"
								: "real") + MSG + ") ";
			}
			open += MSG + "To call a wager type " + VAR + "!call <name>";
			return (List<String>) Arrays.asList(open);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mi.cjc.ircbot.IRCGame#getTimedTasks()
	 */
	@Override
	public HashMap<Integer, Integer> getTimedTasks() {
		// task id / minutes between running it
		HashMap<Integer, Integer> retList = new HashMap<Integer, Integer>();
		retList.put(1, 3); // 1 is for annoucing games
		return retList;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mi.cjc.ircbot.IRCGame#getChannel()
	 */
	@Override
	public String getChannel() {
		// TODO Auto-generated method stub
		return this.channel;
	}
}
