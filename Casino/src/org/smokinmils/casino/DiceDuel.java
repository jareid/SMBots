package org.smokinmils.casino;

import org.pircbotx.Colors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.pircbotx.User;
import org.smokinmils.bot.Bet;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.logging.EventLog;
import org.smokinmils.pokerbot.Database;
import org.smokinmils.pokerbot.settings.Strings;

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
	public List<String> processCommand(String[] commands, User user,
			int userlevel, IrcBot bot) {
		String command = commands[0];

		String username = user.getNick();
		Accounts db = Accounts.getInstance();

		if (command.equalsIgnoreCase("dd")) {
			if (commands.length < 2) // if they have done "!dd" with nothing
										// else
				return s_invalidBetString;

			// check if they already have an openbet
			for (Bet bet : openBets) {
				if (bet.getUser().equalsIgnoreCase(username))
					return (List<String>) Arrays.asList(BLD + VAR + username
							+ MSG + ": You already have a wager open, Type "
							+ VAR + "!ddcancel " + MSG + "to cancel it");

			}
			// attempt to parse the amount
			int amount;

			try {
				amount = Integer.parseInt(commands[1]); // we need some
														// exception handling
														// here
			} catch (Exception e) {
				return s_invalidBetString;
			}
			// if we make it this far, bet will be legit, so add it to open
			// bets.
			if (amount <= 0)
				return (List<String>) Arrays.asList(BLD + MSG
						+ "You have to bet more than 0!");
			// choice is null as DiceDuels done have one.
			if (db.checkChips(username) >= amount) { // add bet, remove chips,
														// notify channel
				String profile = db.getActiveProfile(username);
				Bet bet = new Bet(username, profile, amount, "");
				openBets.add(bet);
				db.removeChips(username, profile, amount);
				db.addBet(bet, 2);
				db.addTransaction(username, profile, 1, -amount, this.ID);
				// System.out.println("post adding bet");
				return (List<String>) Arrays.asList(BLD
						+ VAR
						+ username
						+ MSG
						+ " has opened a new dice duel wager of "
						+ VAR
						+ amount
						+ " "
						+ MSG
						+ ((bet.getProfile().equalsIgnoreCase("play")) ? "play"
								: "real") + " chips! To call this wager type "
						+ VAR + "!call " + username);
			} else {
				return (List<String>) Arrays.asList(BLD + VAR + username + MSG
						+ ": Not enough chips!");
				// not enough chips
			}
		} // </dd>
		else if (command.equalsIgnoreCase("call")) {
			String p1 = user.getNick(); // use rwho is calling
			String p2 = commands[1];

			// check to see if someone is playing themselves...
			if (p1.equalsIgnoreCase(p2))
				return (List<String>) Arrays.asList(BLD + VAR + username + MSG
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
					if (db.checkChips(p1) >= bet.getAmount())
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
					if (d1 > d2) // p1 wins
					{
						winner = p1;
						loser = p2;
						winnerProfile = p1Profile;
						loserProfile = p2Profile;
					} else // p2 wins, use his profile
					{
						loser = p1;
						winner = p2;
						loserProfile = p1Profile;
						winnerProfile = p2Profile;
					}
					int rake = 1;
					if (0.05 * bet.getAmount() * 2 > 1)
						rake = (int) Math.round(0.02 * bet.getAmount() * 2);
					db.addChips(winner, winnerProfile, (bet.getAmount() * 2)
							- rake, null);

					// log everything to db
					// db.recordLoss(loser);
					// db.recordWin(winner);
					db.recordBet(p1, bet.getAmount());
					db.recordBet(p2, bet.getAmount());
					db.delBet(bet, 2);
					db.addTransaction(winner, winnerProfile, 4,
							(bet.getAmount() * 2) - rake, this.ID);
					// //bot.sendMessage(bet.getUser(), message)
					openBets.remove(bet);

					// jackpot stuff
					if (bet.getAmount() >= 50) {

						if (p1Profile.equalsIgnoreCase(p2Profile)) { // same
																		// profile
							int jackpotRake = (int) Math
									.floor((bet.getAmount() * 2) / 100
											* Settings.DDRAKE);
							if (DiceDuel.checkJackpot()) {
								ArrayList<String> players = new ArrayList<String>();
								players.add(p1);
								players.add(p2);
								this.jackpotWon(p1Profile, players, bot);
							} else {
								DiceDuel.updateJackpot(jackpotRake, p1Profile);
							}
						} else { // different profiles
							int jackpotRake = (int) Math
									.floor((bet.getAmount()) / 100
											* Settings.DDRAKE);
							if (DiceDuel.checkJackpot()) { // loser first? Let's
															// be nice
								ArrayList<String> players = new ArrayList<String>();
								players.add(loser);
								this.jackpotWon(loserProfile, players, bot);
								DiceDuel.updateJackpot(jackpotRake,
										winnerProfile);
							} else if (DiceDuel.checkJackpot()) {
								ArrayList<String> players = new ArrayList<String>();
								players.add(winner);
								this.jackpotWon(winnerProfile, players, bot);
								DiceDuel.updateJackpot(jackpotRake,
										loserProfile);
							} else {
								DiceDuel.updateJackpot(jackpotRake,
										winnerProfile);
								DiceDuel.updateJackpot(jackpotRake,
										loserProfile);

							}
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
		} else if (command.equalsIgnoreCase("ddcancel"))// ||
														// command.equalsIgnoreCase("cancel"))
		{
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
		return null;
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

	/**
	 * Save the new jackpot value
	 */
	private static synchronized boolean updateJackpot(int rake, String profile) {
		boolean added = false;
		int jackpot = Database.getInstance().getJackpot(profile);

		int incrint = rake;

		EventLog.log(profile + " jackpot: " + Integer.toString(jackpot) + " + "
				+ Integer.toString(incrint) + " (" + Integer.toString(rake)
				+ ")", "DiceDuel", "updateJackpot");

		if (incrint > 0) {
			added = true;
			jackpot += incrint;
			// Announce to lobbyChan
			// String out = Strings.JackpotIncreased.replaceAll("%chips",
			// Integer.toString(jackpot));
			// out = out.replaceAll("%profile", profile);
			// irc.sendIRCMessage(out);

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
	private void jackpotWon(String profileName, ArrayList<String> players,
			IrcBot bot) {
		int jackpot = Database.getInstance().getJackpot(profileName);

		if (jackpot > 0) {
			int remainder = jackpot % players.size();
			jackpot -= remainder;

			if (jackpot != 0) {
				int win = jackpot;// / players.size();
				for (String player : players) {
					Database.getInstance().jackpot(player, win, profileName);
				}

				// Announce to channel

				String out = Strings.JackpotWon.replaceAll("%chips",
						Integer.toString(jackpot));
				out = out.replaceAll("%profile", profileName);
				out = out.replaceAll("%winners", players.toString());

				bot.sendIRCMessage(this.channel, out);
				bot.sendIRCMessage(this.channel, out);
				bot.sendIRCMessage(this.channel, out);
				/*
				 * ircClient.sendIRCMessage(out); ircClient.sendIRCMessage(out);
				 * ircClient.sendIRCMessage(out);
				 * 
				 * // Announce to table out =
				 * Strings.JackpotWonTable.replaceAll("%chips",
				 * Integer.toString(win)); out = out.replaceAll("%profile",
				 * profileName); out = out.replaceAll("%winners",
				 * jackpotPlayers.toString());
				 * ircClient.sendIRCMessage(ircChannel, out);
				 * ircClient.sendIRCMessage(ircChannel, out);
				 * ircClient.sendIRCMessage(ircChannel, out);
				 * 
				 * // Update jackpot with remainder if (remainder > 0) { out =
				 * Strings.JackpotIncreased.replaceAll("%chips",
				 * Integer.toString(remainder)); out =
				 * out.replaceAll("%profile", profileName);
				 * ircClient.sendIRCMessage(out); }
				 */
				Database.getInstance().updateJackpot(profileName, remainder);

			}
		}

	}
}
