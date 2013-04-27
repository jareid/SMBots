package org.smokinmils.casino;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.smokinmils.bot.Bet;
import org.smokinmils.logging.EventLog;
import org.smokinmils.pokerbot.Database;
import org.smokinmils.pokerbot.settings.Strings;

public class Roulette implements IRCGame {

	private String MSG = Colors.BLUE;
	private String VAR = Colors.RED;
	private String BLD = Colors.BOLD;

	ArrayList<String> validCommands;

	private List<String> s_invalidBetString = (List<String>) Arrays.asList(BLD
			+ MSG + "\"!bet " + VAR + "<amount> <choice>" + MSG
			+ "\" You have entered an invalid choice. Please enter " + VAR
			+ "black" + MSG + ", " + VAR + "red" + MSG + ", " + VAR + "1-36"
			+ MSG + ", " + VAR + "1st, " + VAR + "2nd, " + VAR + "3rd, " + VAR
			+ "even" + MSG + " or " + VAR + "odd" + MSG + " as your choice.");

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

	public Roulette(int delay, String channel, PircBotX bot) {
		// TODO add in stuff for ranks, and loading stuff from file :)
		this.delay = delay;
		this.bot = bot;
		// random = new Random();
		validCommands = new ArrayList<String>();
		validCommands.add("bet");
		validCommands.add("cancel");

		// temporary debugging commands -- or, not left in
		validCommands.add("board");
		validCommands.add("end");

		// instantiate the bet lists
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
	public synchronized List<String> processCommand(String[] commands,
			User user, int userlevel, PircBotX bot) {
		// Process the command

		Accounts db = Accounts.getInstance();
		String command = commands[0];

		String username = user.getNick();
		String profile = db.getActiveProfile(username);

		if (command.equalsIgnoreCase("bet")) {
			// if we are not accepting bets
			if (this.state == CLOSE) {
				;

				return (List<String>) Arrays
						.asList(BLD
								+ MSG
								+ "Bets are now closed, please wait for the next round!");
			}
			if (commands.length < 3) // if they have done "!bet" with nothing
										// else
			{
				;
				return s_invalidBetString;

			}

			// attempt to parse the amount
			int amount;

			try {
				amount = Integer.parseInt(commands[1]); // we need some
														// exception handling
														// here
			} catch (Exception e) {
				;

				return s_invalidBetString;
			}

			// check they have enough to bet before even attempting to parse the
			// actual bet itself
			if (db.checkChips(username) < amount) {
				;

				return (List<String>) Arrays.asList(BLD + MSG
						+ "You do not have enough chips for that!");
			}
			// check for negative bets / 0 bets
			if (amount <= 0) {
				;

				return (List<String>) Arrays.asList(BLD + MSG
						+ "You have to bet more than 0!");
			}
			// check if they are betting on red or black
			String choice = commands[2].toLowerCase();

			if (choice.equalsIgnoreCase("red")
					|| choice.equalsIgnoreCase("black")) {

				db.removeChips(username, profile, amount);
				Bet bet = new Bet(username, profile, amount, choice);
				colourBets.add(bet);
				db.addBet(bet, 3); //
				db.addTransaction(username, profile, 1, -amount, 3);
				;

				return (List<String>) Arrays.asList(BLD + VAR + username + MSG
						+ ": You have bet " + VAR + amount + MSG + " on " + VAR
						+ choice);
			} else if (choice.equalsIgnoreCase("1st")
					|| choice.equalsIgnoreCase("2nd")
					|| choice.equalsIgnoreCase("3rd")) {
				db.removeChips(username, profile, amount);
				Bet bet = new Bet(username, profile, amount, choice);
				rowBets.add(bet);
				db.addBet(bet, 5);
				db.addTransaction(username, profile, 1, -amount, 5);
				;

				return (List<String>) Arrays.asList(BLD + VAR + username + MSG
						+ ": You have bet " + VAR + amount + MSG + " on the "
						+ VAR + choice + MSG + " row");
			} else if (choice.equalsIgnoreCase("even")
					|| choice.equalsIgnoreCase("odd")) {
				db.removeChips(username, profile, amount);
				Bet bet = new Bet(username, profile, amount, choice);
				evenOddBets.add(bet);
				db.addBet(bet, 6);
				db.addTransaction(username, profile, 1, -amount, 6);
				;

				return (List<String>) Arrays.asList(BLD + VAR + username + MSG
						+ ": You have bet " + VAR + amount + MSG + " on " + VAR
						+ choice);
			}
			// if we get this far the bet is either invalid, (which should never
			// reach due to checking in the bot,
			// or we are betting on a number, so let's try to

			int bet;
			try {
				bet = Integer.parseInt(choice);
			} catch (Exception e) {
				// not a number, lets just return an invalid bet
				return s_invalidBetString;
			}
			// check range for bets
			if (bet < 1 || bet > 36) {
				return s_invalidBetString;
			} else {
				db.removeChips(username, profile, amount);
				Bet bett = new Bet(username, profile, amount, choice);
				// bett? need to sort variable names
				numberBets.add(bett);
				db.addBet(bett, 4);
				db.addTransaction(username, profile, 1, -amount, 4);

				return (List<String>) Arrays.asList(BLD + VAR + username + MSG
						+ ": You have bet " + VAR + amount + MSG
						+ " on the number " + VAR + bet);
			}
		}// </bet>
		else if (command.equalsIgnoreCase("cancel")) {
			for (Bet bet : numberBets) {
				if (bet.isValid() && bet.getUser().equalsIgnoreCase(username)) {
					bet.invalidate();
					db.delBet(bet, 4);
					db.addChips(username, bet.getProfile(), bet.getAmount(),
							user);
					db.addTransaction(username, bet.getProfile(), 3,
							bet.getAmount(), 4);

				}
			}
			for (Bet bet : colourBets) {
				if (bet.isValid() && bet.getUser().equalsIgnoreCase(username)) {
					bet.invalidate();
					db.delBet(bet, 3);
					db.addChips(username, bet.getProfile(), bet.getAmount(),
							user);
					db.addTransaction(username, bet.getProfile(), 3,
							bet.getAmount(), 3);
				}
			}
			for (Bet bet : rowBets) {
				if (bet.isValid() && bet.getUser().equalsIgnoreCase(username)) {
					bet.invalidate();
					db.delBet(bet, 5);
					db.addChips(username, bet.getProfile(), bet.getAmount(),
							user);
					db.addTransaction(username, bet.getProfile(), 3,
							bet.getAmount(), 5);
				}
			}
			for (Bet bet : evenOddBets) {
				if (bet.isValid() && bet.getUser().equalsIgnoreCase(username)) {
					bet.invalidate();
					db.delBet(bet, 6);
					db.addChips(username, bet.getProfile(), bet.getAmount(),
							user);
					db.addTransaction(username, bet.getProfile(), 3,
							bet.getAmount(), 6);

				}
			}

			return (List<String>) Arrays.asList(BLD + MSG
					+ "All bets cancelled for " + VAR + username);
		} else if (command.equalsIgnoreCase("end")) {
			// call end function
			if (userlevel > 0) {

				return this.endGame(bot);
			} else {
				;

				return (List<String>) Arrays.asList(BLD + MSG
						+ "You don't have the required permissions for that");
			}
		}
		// these are debug commands to be removed later on... maybe
		else if (command.equalsIgnoreCase("board")) {

			return printBoard();
		}
		;
		return null;

	}

	@Override
	public String getInfo() {
		return "Test Instructions";
	}

	//  for setting colours
	private List<String> printBoard() {
		return (List<String>) Arrays
				.asList("0,3 00 0,1 28 0,4 09 0,1 26 0,4 30 0,1 11 0,4 07 0,1 20 0,4 32 0,1 17 0,4 05 0,1 22 0,4 34 0,1",
						"0,3 00 0,4 15 0,1 03 0,4 24 0,1 36 0,4 13 0,1 01 0,4 27 0,1 10 0,4 25 0,1 29 0,4 12 0,1 08 0,1",
						"0,3 00 0,1 19 0,4 31 0,1 18 0,4 06 0,1 21 0,4 33 0,1 16 0,4 04 0,1 23 0,4 35 0,1 14 0,4 02 0,1");
		// return board;
	}

	private String getColour(int number) {
		if (number == 28 | number == 26 | number == 11 | number == 20
				| number == 17 | number == 22 | number == 3 | number == 36
				| number == 1 | number == 10 | number == 29 | number == 8
				| number == 19 | number == 18 | number == 21 | number == 16
				| number == 23 | number == 14) {
			return "black";
		} else if (number == 0) {
			return "green";
		} else
			return "red";
	}

	private String getRow(int number) {
		if (number == 28 | number == 9 | number == 26 | number == 30
				| number == 11 | number == 7 | number == 20 | number == 32
				| number == 17 | number == 5 | number == 22 | number == 34)
			return "1st";
		else if (number == 15 | number == 3 | number == 24 | number == 36
				| number == 13 | number == 1 | number == 27 | number == 10
				| number == 25 | number == 29 | number == 12 | number == 8)
			return "2nd";
		else
			return "3rd";
	}

	/**
	 * Ends the game and prints out winners (move to arraylist as per usual)
	 * 
	 * @return winners / win info
	 */
	private List<String> endGame(PircBotX bot) {

		Accounts db = Accounts.getInstance();
		// Let's "roll"
		int winner = TrueRandom.nextInt(39);
		if (winner > 36)
			winner = 0;

		List<String> nameList = new ArrayList<String>();

		// number bets
		for (Bet bet : numberBets) {
			if (Integer.parseInt(bet.getChoice()) == winner && bet.isValid()) {
				if (!nameList.contains(bet.getUser()))
					nameList.add(bet.getUser());
				int winamount = 36;
				if (bet.getChoice() == "0")
					winamount = 12;
				db.addChips(bet.getUser(), bet.getProfile(), bet.getAmount()
						* winamount, null);
				// db.recordWin(bet.getUser());
				db.recordBet(bet.getUser(), bet.getAmount());
				db.addTransaction(bet.getUser(), bet.getProfile(), 4,
						bet.getAmount() * winamount, 4);
				// bot.sendMessage(bet.getUser(), "You bet "+
				// bet.getAmount()+" on Roulette and won! Your chips total is "+db.checkChips(bet.getUser())
				// + ".");

			} else if (bet.isValid()) {
				// the bet didn't win, so tell them they lost (hah loser!)
				// bot.sendMessage(bet.getUser(), "You bet "+
				// bet.getAmount()+" on Roulette and lost! Your chips total is "+db.checkChips(bet.getUser())
				// + ".");

			}
			addToJackpot(bet);
			db.delBet(bet, 4);
		}
		// colour bets
		for (Bet bet : colourBets) {
			if (bet.getChoice().equalsIgnoreCase(this.getColour(winner))
					&& bet.isValid() && winner != 0) {
				if (!nameList.contains(bet.getUser()))
					nameList.add(bet.getUser());
				db.addChips(bet.getUser(), bet.getProfile(),
						bet.getAmount() * 2, null);
				db.recordWin(bet.getUser());
				db.recordBet(bet.getUser(), bet.getAmount());
				db.addTransaction(bet.getUser(), bet.getProfile(), 4,
						bet.getAmount() * 2, 3);
				// bot.sendMessage(bet.getUser(), "You bet "+
				// bet.getAmount()+" on Roulette and won! Your chips total is "+db.checkChips(bet.getUser())
				// + ".");

			} else if (bet.isValid()) {
				// the bet didn't win, so tell them they lost (hah loser!)
				// bot.sendMessage(bet.getUser(), "You bet "+
				// bet.getAmount()+" on Roulette and lost! Your chips total is "+db.checkChips(bet.getUser())
				// + ".");

			}
			addToJackpot(bet);

			db.delBet(bet, 3);
		}
		// row bets

		for (Bet bet : rowBets) {
			if (bet.getChoice().equalsIgnoreCase(this.getRow(winner))
					&& bet.isValid() && winner != 0) {
				if (!nameList.contains(bet.getUser()))
					nameList.add(bet.getUser());
				db.addChips(bet.getUser(), bet.getProfile(),
						bet.getAmount() * 3, null);
				db.recordWin(bet.getUser());
				db.recordBet(bet.getUser(), bet.getAmount());
				db.addTransaction(bet.getUser(), bet.getProfile(), 4,
						bet.getAmount() * 3, 5);
				// bot.sendMessage(bet.getUser(), "You bet "+
				// bet.getAmount()+" on Roulette and won! Your chips total is "+db.checkChips(bet.getUser())
				// + ".");

			} else if (bet.isValid()) {
				// the bet didn't win, so tell them they lost (hah loser!)
				// bot.sendMessage(bet.getUser(), "You bet "+
				// bet.getAmount()+" on Roulette and lost! Your chips total is "+db.checkChips(bet.getUser())
				// + ".");

			}
			addToJackpot(bet);

			db.delBet(bet, 5);

		}
		for (Bet bet : evenOddBets) {

			if (bet.isValid()) {
				int mod = 1;
				if (bet.getChoice().equalsIgnoreCase("even"))
					mod = 0;
				if (winner % 2 == mod && winner != 0) {
					if (!nameList.contains(bet.getUser()))
						nameList.add(bet.getUser());
					db.addChips(bet.getUser(), bet.getProfile(),
							bet.getAmount() * 2, null);
					db.recordWin(bet.getUser());
					db.recordBet(bet.getUser(), bet.getAmount());
					db.addTransaction(bet.getUser(), bet.getProfile(), 4,
							bet.getAmount() * 2, 6);
					// bot.sendMessage(bet.getUser(), "You bet "+
					// bet.getAmount()+" on Roulette and won! Your chips total is "+db.checkChips(bet.getUser())
					// + ".");

				} else if (bet.isValid()) {
					// the bet didn't win, so tell them they lost (hah loser!)
					// bot.sendMessage(bet.getUser(), "You bet "+
					// bet.getAmount()+" on Roulette and lost! Your chips total is "+db.checkChips(bet.getUser())
					// + ".");

				}
			}
			addToJackpot(bet);
			db.delBet(bet, 6);

		}

		List<String> retList = new ArrayList<String>();
		// Construct the winning string
		String winningString = BLD + MSG + "The winning number is: " + VAR
				+ winner + " " + MSG + this.getColour(winner);
		if (this.getColour(winner).equalsIgnoreCase("red"))
			winningString += " 0,4 ";
		else if (this.getColour(winner).equalsIgnoreCase("black"))
			winningString += " 0,1 ";
		else
			winningString += " 0,3 ";
		winningString += winner + " 0,4";
		retList.add(winningString);
		String names = "";
		if (nameList.size() > 0) {
			// tag them all on
			for (String user : nameList) {
				names += user + " ";
			}
			retList.add(BLD + MSG + "Congratulations to " + VAR + names);
		}

		// JACK POT STUFF "between" games
		ArrayList<Bet> allBets = new ArrayList<Bet>();
		allBets.addAll(colourBets);
		allBets.addAll(evenOddBets);
		allBets.addAll(numberBets);
		allBets.addAll(rowBets);

		// find all profiles
		ArrayList<String> profiles = new ArrayList<String>();
		for (Bet bet : allBets) {
			if (!profiles.contains(bet.getProfile())) {
				profiles.add(bet.getProfile());
			}
		}
		// then each user per profile
		// check if they win
		for (String profile : profiles) {
			ArrayList<String> users = new ArrayList<String>();
			for (Bet bet : allBets) {
				if (bet.getProfile().equalsIgnoreCase(profile)
						&& !users.contains(bet.getUser())) {
					// user is on the correct profile AND not already added
					users.add(bet.getUser());
				}
			}
			// now check if this profile wins
			if (Roulette.checkJackpot()) {
				// winner
				this.jackpotWon(profile, users, bot);
			}
		}
		retList.add(BLD + MSG + "A new roulette game is starting! Type " + VAR
				+ "!info " + MSG + "for instructions on how to play.");
		retList.addAll(this.printBoard());
		retList.add(BLD + MSG + "Place your bets now!");

		// clear the bets
		colourBets = new ArrayList<Bet>();
		rowBets = new ArrayList<Bet>();
		numberBets = new ArrayList<Bet>();
		evenOddBets = new ArrayList<Bet>();

		this.state = OPEN;

		return retList;

	}

	private void addToJackpot(Bet bet) {
		if (bet.getAmount() >= 50) {
			Roulette.updateJackpot((int) bet.getAmount() / 100
					* Settings.ROULETTERAKE, bet.getProfile());
		}
	}

	@Override
	public List<String> timerTask(int taskId) {
		if (taskId == 1) {
			return this.endGame(this.bot);
		}
		if (taskId == 2) {
			this.state = CLOSE;
			return (List<String>) Arrays.asList(BLD + MSG
					+ "Bets closed! No more bets. Spinning...");
		}
		// dirty
		return new ArrayList<String>();
	}

	@Override
	public HashMap<Integer, Integer> getTimedTasks() {
		// task id / minutes between running it
		HashMap<Integer, Integer> retList = new HashMap<Integer, Integer>();
		retList.put(1, this.delay); // 1 is for ending game / starting a new
									// game
		retList.put(2, -this.delay);
		return retList;
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
				+ Integer.toString(incrint) + " (" + Integer.toString(rake)
				+ ")", "roulette", "updateJackpot");

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
			PircBotX bot) {
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

				bot.sendMessage(this.channel, out);
				bot.sendMessage(this.channel, out);
				bot.sendMessage(this.channel, out);
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
