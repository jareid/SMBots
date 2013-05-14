package org.smokinmils.casino;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.User;
import org.smokinmils.SMBaseBot;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.logging.EventLog;

public class Casino extends Event {

	// <Channel, Game>
	private ArrayList<IRCGame> games;

	private String MSG = Colors.BLUE;
	private String VAR = Colors.RED;
	private String BLD = Colors.BOLD;

	private ArrayList<Timer> events;
	
	public Casino(IrcBot bot) {
		Accounts.getInstance().processRefunds();
		
		games = new ArrayList<IRCGame>();

		new ArrayList<String>();

		// Live configuration
		games.add(new Roulette(5, "#smokin_dice", bot));
		games.add(new Roulette(1, "#sm_roulette", bot));
		games.add(new OverUnder("#sm_overunder"));
		games.add(new DiceDuel("#smokin_dice"));
		
		games.add(new Roulette(2, "#sm_tournaments", bot));
		games.add(new DiceDuel("#sm_tournaments"));

		// test config
		/*
		 * games.add(new Roulette(1, "#testeroo", bot)); games.add(new
		 * DiceDuel("#testeroo")); games.add(new OverUnder("#testeroo"));
		 */

		// initialize timing events
		events = new ArrayList<Timer>();
		for (IRCGame g : games) {
			// join all the channels
			bot.joinChannel(g.getChannel());

			// add all the timed events if we have them
			if (g.getTimedTasks() != null) {
				for (Map.Entry<Integer, Integer> entry : g.getTimedTasks()
						.entrySet()) {
					if (entry.getValue() < 0) {
						Timer timer = new Timer(true);
						timer.schedule(new gameTrigger(entry.getKey(), g, bot,
								g.getChannel()),
								(-entry.getValue() * 60 * 1000) - 10000, -entry
										.getValue() * 60 * 1000);
						events.add(timer);
					} else {
						Timer timer = new Timer(true);
						timer.schedule(new gameTrigger(entry.getKey(), g, bot,
								g.getChannel()), entry.getValue() * 60 * 1000,
								entry.getValue() * 60 * 1000);
						events.add(timer);
					}
				}
			}
		}

	}

	public void message(Message e) throws Exception {
		synchronized (SMBaseBot.lockObject) {
			// if the message starts with ! (so it is a command) and it is longer
			// than just !
			if (e.getBot().userIsIdentified(e.getUser().getNick())) {
				if (e.getMessage().startsWith("!") && e.getMessage().length() > 1) {
					// parse the messages
					String[] words = e.getMessage().split("!")[1].split(" ");
					String command = words[0];
					String sender = e.getUser().getNick();
					String chan = e.getChannel().getName();
	
					// HAX to handle ou in smokin_dice
					if (command.equalsIgnoreCase("ou")
							&& chan.equalsIgnoreCase("#smokin_dice")) {
						e.respond(BLD + VAR + sender + MSG
								+ " Please join #SM_OverUnder to play Over/Under"); // todo
																					// fix
																					// this
																					// /
																					// tidy
					} else {
						// game commands
						// since replies might require multiple lines, iterate
						// through the replies and send them
						for (IRCGame g : games) {
							if (g.getChannel().equalsIgnoreCase(chan)
									&& g.isValidCommand(command)) {
								// should check if is valid here
								List<String> replies = g.processCommand(words, e
										.getUser(), this.getUserLevel(sender, chan,
										e.getUser()), e.getBot());
								if (replies != null) {
									for (String reply : replies)
										e.getBot().sendIRCMessage(chan, reply);
								} else {
									EventLog.log("Casino reply is null", "Casino", "message");
								}
							}
						}
					}
	
				}
			} else {
				// user isn't verified
			}
		}
	}

	/**
	 * Iterate through the list of users and get the level
	 * 
	 * @param username
	 *            The username in question
	 * @return The users level, where 0 = normal, 1 = voiced, 2 = op
	 */

	private int getUserLevel(String username, String channel, User user) {
		int retVal = 0;

		for (Channel chan : user.getChannelsOpIn()) {
			if (chan.getName().equalsIgnoreCase(channel))
				retVal = 2;
		}
		for (Channel chan : user.getChannelsVoiceIn()) {
			if (chan.getName().equalsIgnoreCase(channel))
				retVal = 1;
		}
		return retVal;
	}

	/**
	 * Simple extension to time task to deal with game triggers
	 * 
	 * @author cjc
	 * 
	 */
	class gameTrigger extends TimerTask {
		private int id;
		private IrcBot bot;
		private String channel;
		private IRCGame game;

		public gameTrigger(int id, IRCGame game, IrcBot bot, String channel) {
			this.id = id;
			this.bot = bot;
			this.channel = channel;
			this.game = game;
		}

		public void run() {
			// System.out.println("triggered");
			for (String reply : this.game.timerTask(id))
				bot.sendIRCMessage(this.channel, reply);
			// sendMessage(channel, reply);
		}
	}
}
