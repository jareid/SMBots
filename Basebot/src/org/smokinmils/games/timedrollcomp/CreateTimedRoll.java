package org.smokinmils.games.timedrollcomp;

/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.Channel;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.types.ProfileType;

public class CreateTimedRoll extends Event {
	public static final String Command = "!startroll";
	public static final String Description = "%b%c12Start's a timed free roll.";
	public static final String Format = "%b%c12" + Command + " <channel> <minutes> <prize> <profile> <rounds>";

	public static final String EndCommand = "!end";
	public static final String EndDescription = "%b%Ends a timed free roll game.";
	public static final String EndFormat = "%b%c12" + EndCommand + " <channel>";
	
	public static final String Created = "%b%c12A new timed roll game has been created in %c04%chan%c12 with %c04%mins%c12 minute rounds (%c04%rounds%c12 rounds) and a prize of %c04%prize%c12 chips for the %c04%profile%c12!";
	public static final String Ended = "%b%c12The timed roll game for %c04%chan%c12 has been ended!";
	public static final String Exist = "%b%c12A timed roll game already exists in %c04%chan%c12!";
	public static final String NoExist = "%b%c12A timed roll game doesn't exist in %c04%chan%c12!";
	
	private Map<String, TimedRollComp> Games;
	
	public CreateTimedRoll() {
		Games = new HashMap<String, TimedRollComp>();
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
		String message = event.getMessage().toLowerCase();
		String sender = event.getUser().getNick();
		
		if ( isValidChannel( event.getChannel().getName() ) &&
				event.getBot().userIsIdentified( sender )) {
			if ( Utils.startsWith(message, Command ) ) {
				createGame(event);
			} else if ( Utils.startsWith(message, EndCommand ) ) {
				endGame(event);
			}
		}
	}
	
	private void createGame(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		Channel chan = event.getChannel();
		String sender = event.getUser().getNick();
		String[] msg = message.split(" ");

		if (msg.length == 6) {
			String channel = msg[1];
			Integer mins = Utils.tryParse(msg[2]);
			Integer prize = Utils.tryParse(msg[3]);
			ProfileType profile = ProfileType.fromString(msg[4]);
			Integer rounds = Utils.tryParse(msg[5]);
			
			if (!channel.isEmpty() && mins != null && prize != null && rounds != null && rounds > 0) {						
				// Check valid profile
				if ( profile != null ) {
					TimedRollComp trc = Games.get(channel.toLowerCase());
					if (trc != null) {
						
					} else {
						try { 
							trc = new TimedRollComp(bot, channel, profile,
													(int)prize, (int)mins, (int)rounds, this);
							
							String out = Created.replaceAll("%chan", channel);
							out = out.replaceAll("%prize", Integer.toString(prize));
							out = out.replaceAll("%mins", Integer.toString(mins));
							out = out.replaceAll("%profile", profile.toString());
							out = out.replaceAll("%rounds", Integer.toString(rounds));
							bot.sendIRCMessage(chan, out);

							Games.put(channel.toLowerCase(), trc);
						} catch (IllegalArgumentException e) {
							bot.sendIRCNotice(sender, "%b%c12Received the following error: %c04" + e.getMessage());
						}
					}
				} else {
					bot.sendIRCMessage(chan.getName(), IrcBot.VALID_PROFILES);
				}
			} else {
				bot.invalidArguments( sender, Format );
			}
		} else {
			bot.invalidArguments( sender, Format );
		}
	}
	
	private void endGame(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		Channel chan = event.getChannel();
		String sender = event.getUser().getNick();
		String[] msg = message.split(" ");

		if (msg.length == 2) {
			String channel = msg[1];
			
			if (!channel.isEmpty()) {
				TimedRollComp trc = Games.remove(channel.toLowerCase());
				if (trc == null) {
					bot.sendIRCMessage(chan.getName(), NoExist.replaceAll("%chan", channel) );
				} else {
					trc.close();
					bot.sendIRCMessage(chan.getName(), Ended.replaceAll("%chan", channel) );
				}
			} else {
				bot.invalidArguments( sender, EndFormat );
			}
		} else {
			bot.invalidArguments( sender, EndFormat );
		}
	}
	
	public void endRollGame(String channel, IrcBot bot) {
		TimedRollComp trc = Games.remove(channel.toLowerCase());
		if (trc != null) {
			bot.sendIRCMessage(channel, Ended.replaceAll("%chan", channel) );
		}
	}
}
