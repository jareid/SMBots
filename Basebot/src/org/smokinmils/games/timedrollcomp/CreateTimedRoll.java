package org.smokinmils.games.timedrollcomp;

/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.types.ProfileType;

/**
 * Provides the functionality create a timed roll.
 * 
 * @author Jamie
 */
public class CreateTimedRoll extends Event {
    /** The Create command. */
    public static final String               CRE_CMD        = "!startroll";

    /** The Create format. */
    public static final String               CRE_FORMAT     = "%b%c12"
                  + CRE_CMD + " <channel> <minutes> <prize> <profile> <rounds>";

    /** The create command length. */
    public static final int                  CRE_CMD_LEN    = 6;

    /** The End command. */
    public static final String               ENDCMD     = "!end";
    
    /** The End command format. */
    public static final String               END_FORMAT      = "%b%c12"
                                                        + ENDCMD + " <channel>";

    /** The message when a game is created. */
    public static final String               CREATED        = "%b%c12A new "
                 + "timed roll game has been created in %c04%chan%c12 with "
                 + "%c04%mins%c12 minute rounds (%c04%rounds%c12 rounds) and"
                 + " a prize of %c04%prize%c12 chips for the %c04%profile%c12!";
    
    /** The message when a game is ended. */
    public static final String               ENDED          = "%b%c12The timed "
                                + "roll game for %c04%chan%c12 has been ended!";
   
    /** The message when a game exists. */
    public static final String               EXIST          = "%b%c12A timed "
                                 + "roll game already exists in %c04%chan%c12!";
    
    /** The message when a game doesn't exist. */
    public static final String               NOEXIST        = "%b%c12A timed "
                                  + "roll game doesn't exist in %c04%chan%c12!";

    /** The map of games. */
    private final Map<String, TimedRollComp> games;

    /**
     * Constructor.
     */
    public CreateTimedRoll() {
        games = new HashMap<String, TimedRollComp>();
    }

    /**
     * This method handles the chips command.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        String message = event.getMessage().toLowerCase();
        User sender = event.getUser();

        if (isValidChannel(event.getChannel().getName())
                && event.getBot().userIsIdentified(sender)) {
            if (Utils.startsWith(message, CRE_CMD)) {
                createGame(event);
            } else if (Utils.startsWith(message, ENDCMD)) {
                endGame(event);
            }
        }
    }

    /**
     * Handles the end command.
     * 
     * @param event the message event.
     */
    private void createGame(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        Channel chan = event.getChannel();
        User senderu = event.getUser();
        String[] msg = message.split(" ");

        if (msg.length == CRE_CMD_LEN) {
            int i = 1;
            String channel = msg[i];
            i++;
            Integer mins = Utils.tryParse(msg[i]);
            i++;
            Integer prize = Utils.tryParse(msg[i]);
            i++;
            ProfileType profile = ProfileType.fromString(msg[i]);
            i++;
            Integer rounds = Utils.tryParse(msg[i]);

            if (!channel.isEmpty() && mins != null && prize != null
                    && rounds != null && rounds > 0) {
                // Check valid profile
                if (profile != null) {
                    TimedRollComp trc = games.get(channel.toLowerCase());
                    if (trc != null) {
                        bot.sendIRCMessage(
                                chan.getName(),
                                EXIST.replaceAll("%chan", channel));
                    } else {
                        try {
                            trc = new TimedRollComp(bot, channel, profile,
                                    prize, mins, rounds, this);

                            String out = CREATED.replaceAll("%chan", channel);
                            out = out.replaceAll(
                                    "%prize", Integer.toString(prize));
                            out = out.replaceAll(
                                    "%mins", Integer.toString(mins));
                            out = out
                                    .replaceAll("%profile", profile.toString());
                            out = out.replaceAll(
                                    "%rounds", Integer.toString(rounds));
                            bot.sendIRCMessage(chan, out);

                            games.put(channel.toLowerCase(), trc);
                        } catch (IllegalArgumentException e) {
                            bot.sendIRCNotice(
                                    senderu,
                                    "%b%c12Received the following error: %c04"
                                            + e.getMessage());
                        }
                    }
                } else {
                    bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                }
            } else {
                bot.invalidArguments(senderu, CRE_FORMAT);
            }
        } else {
            bot.invalidArguments(senderu, CRE_FORMAT);
        }
    }

    /**
     * Handles the end command.
     * 
     * @param event the message event.
     */
    private void endGame(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        Channel chan = event.getChannel();
        User senderu = event.getUser();
        String[] msg = message.split(" ");

        if (msg.length == 2) {
            String channel = msg[1];

            if (!channel.isEmpty()) {
                TimedRollComp trc = games.remove(channel.toLowerCase());
                if (trc == null) {
                    bot.sendIRCMessage(
                            chan.getName(),
                            NOEXIST.replaceAll("%chan", channel));
                } else {
                    trc.close();
                    bot.sendIRCMessage(
                            chan.getName(), ENDED.replaceAll("%chan", channel));
                }
            } else {
                bot.invalidArguments(senderu, END_FORMAT);
            }
        } else {
            bot.invalidArguments(senderu, END_FORMAT);
        }
    }

    /**
     * Ends the game in a channel.
     * 
     * @param channel The channel
     * @param bot The irc bot.
     */
    public final void endRollGame(final String channel,
                                  final IrcBot bot) {
        TimedRollComp trc = games.remove(channel.toLowerCase());
        if (trc != null) {
            bot.sendIRCMessage(channel, ENDED.replaceAll("%chan", channel));
        }
    }
}
