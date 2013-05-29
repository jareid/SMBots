package org.smokinmils.games.timedrollcomp;

/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
import org.pircbotx.Channel;
import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

/**
 * Provides the functionality for a timed roll
 * 
 * @author Jamie
 */
public class TimedRollComp extends Event {
	public static final String Command = "!roll";
	public static final String Description = "%b%c12Let's you use your free roll.";
	public static final String Format = "%b%c12" + Command + " <channel> <minutes> <prize> <profile>";

	public static final String Rolled = "%b%c04%who%c12 has used his roll and rolled a... %c04%roll%c12.";
	public static final String NewLeader = "%b%c04%winner%c12 is now in the lead with %c04%roll";
	public static final String SingleWinner = "%b%c04%winner%c12 has won this round with a roll of %c04%roll%c12 and has been awarded %c04%chips %profile%c12 chips.";
	public static final String MultipleWinners = "%b%c04%winner%c12 have won this round with rolls of %c04%roll%c12 and have been awarded %c04%chips %profile%c12 chips each.";
	public static final String NewGame = "%b%c12A new round has begun, use %c04%cmd%c12 to roll.";
	public static final String AlreadyRolled = "%b%c04%who%c12: You have already rolled for this round.";
	
	public static final int MaxRoll = 1000;
	
	private String ValidChan;
	private IrcBot Bot;
	private Timer Timer;
	private SortedMap<Integer, List<String>> Rolls;
	private List<String> Users;
	private int Prize;
	private ProfileType Profile;
	private Integer Rounds;
	private int RoundsRun;
	private CreateTimedRoll Parent;
	
	public TimedRollComp(IrcBot bot, String channel, ProfileType profile,
						 int prize, int mins, int rounds, CreateTimedRoll ctr)
			throws IllegalArgumentException {
		// check the channel is a valid IRC Channel name
		if (!channel.matches("([#&][^\\x07\\x2C\\s]{1,200})")) {
			throw new IllegalArgumentException(channel + " is not a valid IRC channel name");
		} else if (mins <= 0) {
			throw new IllegalArgumentException(Integer.toString(mins) + " minutes is less than or equal to 0");
		} else if (prize <= 0) {
			throw new IllegalArgumentException("The prize, " + Integer.toString(prize) + ", is less than or equal to 0");
		} else if (profile == null) {
			throw new IllegalArgumentException(IrcBot.ValidProfiles);
		} else {
			ValidChan = channel;
			Prize = prize;
			Rolls = new TreeMap<Integer, List<String>>();
			Users = new ArrayList<String>();
			Bot = bot;
			Profile = profile;
			Rounds = rounds;
			RoundsRun = 0;
			Parent = ctr;
			
			Bot.joinChannel(ValidChan);
			
			bot.getListenerManager().addListener( this );	
	
			// Start the timer
			Timer = new java.util.Timer();
			Timer.scheduleAtFixedRate( new TimedRollTask(), mins*60*1000, mins*60*1000);
		}
	}
	
	public void close() {
		if (Timer != null) Timer.cancel();
		Channel chan = Bot.getChannel(ValidChan);
		boolean is_valid = Bot.getValidChannels().contains(ValidChan.toLowerCase());
		if (chan != null && !is_valid)
			Bot.partChannel(chan);
		Bot.getListenerManager().removeListener( this );
	}

	@Override
	public void message(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		Channel chan = event.getChannel();
		String sender = event.getUser().getNick();
		
		synchronized (this) {			 
			if ( message.startsWith( Command ) &&
				 ValidChan.equalsIgnoreCase(chan.getName()) ) {
				if ( !Users.contains( sender.toLowerCase() ) ) {
					int user_roll = Random.nextInt(MaxRoll);
					
					String out = Rolled.replaceAll("%who", sender);
					out = out.replaceAll("%roll", Integer.toString(user_roll));
					bot.sendIRCMessage(ValidChan, out);
					
					if (Rolls.isEmpty() || Rolls.lastKey() < user_roll) {
						out = NewLeader.replaceAll("%winner", sender);
						out = out.replaceAll("%roll", Integer.toString(user_roll));
						Bot.sendIRCMessage(ValidChan, out);
					}
					
					// add to the map
					List<String> users = Rolls.get(user_roll);
					if (users == null) users = new ArrayList<String>();
					users.add(sender);
					Rolls.put(user_roll, users);
					Users.add(sender.toLowerCase());
				} else {
					Bot.sendIRCNotice(sender, AlreadyRolled.replaceAll("%who", sender));
				}
			}
		}
	}
	
	private synchronized void processRound() {
		// Decide who has won and give them their prize.
		if (!Rolls.isEmpty() && Rolls.size() > 1) {
			Integer win_roll = Rolls.lastKey();
			List<String> winners = Rolls.get( win_roll );
			
			int win = Prize;
			String out;
			String winstr;
			if (winners.size() == 1) {
				// Single winner
				out = SingleWinner;
				winstr = winners.get(0);
			} else {
				// Split winnings
				win = Prize / winners.size();
				if (win == 0) win = 1;
				out = MultipleWinners;
				winstr = Utils.ListToString(winners);
			}
			
			out = out.replaceAll("%winner", winstr);
			out = out.replaceAll("%roll", Integer.toString(win_roll));
			out = out.replaceAll("%chips", Integer.toString(win));
			out = out.replaceAll("%profile", Profile.toString());
			
			Bot.sendIRCMessage(ValidChan, out);
			
			for (String winner: winners) {
				try {
					DB.getInstance().adjustChips(winner, win, Profile,
												GamesType.TIMEDROLL, TransactionType.WIN);
				} catch (Exception e) {
					EventLog.log(e, "TimedRollComp", "processRound");
				}
			}
		}
		// Clear the rolls for the next round
		Rolls.clear();
		Users.clear();
		
		RoundsRun++;
		if (Parent != null && Rounds != -1 && RoundsRun == Rounds) {
			Parent.endRollGame(ValidChan, Bot);
			close();
		} else {
			Bot.sendIRCMessage(ValidChan, NewGame.replaceAll("%cmd", Command));
		}
	}
	
	public class TimedRollTask extends TimerTask {
		@Override
		public void run() {
			processRound();
		}
	}
}

