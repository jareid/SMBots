package org.smokinmils.games.tournaments;

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
 * @author cjc
 */
public class CreateTournament extends Event {
    /** The Create command. */
    public static final String               CRE_CMD        = "!starttournament";

    /** The Create format. */
    public static final String               CRE_FORMAT     = "%b%c12"
                  + CRE_CMD + " <channel> <profile> <time per round> <entry cost>";

    /** The create command length. */
    public static final int                  CRE_CMD_LEN    = 5;
    
    /** The message when a game is created. */
    public static final String               CREATED        = "%b%c12A new "
                 + "tournament has been created in %c04%chan%c12 with "
                 + "the %c04%profile%c12 profile!";
   
    /** The message when a game exists. */
    public static final String               EXIST          = "%b%c12A "
                                 + "tournament already exists in %c04%chan%c12!";
    
    /** The message when a game doesn't exist. */
    public static final String               NOEXIST        = "%b%c12A "
                                  + "tournament doesn't exist in %c04%chan%c12!";
    
    /** The map channel -> games. */
    private final Map<String, Tournament> games;

    /**
     * Constructor.
     */
    public CreateTournament() {
        games = new HashMap<String, Tournament>();
    }

    /**
     * This method handles the message.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        String message = event.getMessage().toLowerCase();

        if (isValidChannel(event.getChannel().getName())) {
            if (Utils.startsWith(message, CRE_CMD)) {
                createGame(event);
            } 
        }
    }

    /**
     * Handles the create command.
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
            ProfileType profile = ProfileType.fromString(msg[i]);
            i++;
            int timePerRound = Utils.tryParse(msg[i]);
            i++;
            double amount = Utils.tryParseDbl(msg[i]);
            if (!channel.isEmpty()) {
                // Check valid profile
                if (profile != null) {
                    Tournament tourn = games.get(channel.toLowerCase());
                    if (tourn != null && bot.getListenerManager().getListeners().contains(tourn)) {
                        bot.sendIRCMessage(chan, EXIST.replaceAll("%chan", channel));
                    } else {
                        try {
                            tourn = new Tournament(bot, bot.getUserChannelDao()
                                    .getChannel(channel), timePerRound, profile, amount);

                            String out = CREATED.replaceAll("%chan", channel);
                            out = out.replaceAll("%profile", profile.toString());
                            bot.sendIRCMessage(chan, out);

                            games.put(channel.toLowerCase(), tourn);
                        } catch (IllegalArgumentException e) {
                            bot.sendIRCNotice(senderu, "%b%c12Received the following error: %c04"
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
    
}
