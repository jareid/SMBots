/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

import org.pircbotx.Channel;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.BetterInfo;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to check a user's position in the competition.
 * 
 * @author Jamie
 */
public class CompPosition extends Event {
    /** The command. */
    public static final String COMMAND           = "!position";
    
    /** The command description. */
    public static final String        DESC              = "%b%c12Returns your "
               + "position for this week's competitiong for all profiles or a "
               + "single profile";
    
    /** The command format. */
    public static final String        FORMAT            = "%b%c12" + COMMAND
                                                        + " <profile> <user>";
    
    /** The command length. */
    public static final int     CMD_LEN     = 3;

    /** Message for a user's position in the competition. */
    private static final String       POSITION          = "%b%c04%sender:%c12 "
           + "%c04%who%c12 is currently in position %c04%position%c12 for the "
           + "%c04%profile%c12 competition with %c04%chips%c12 chips bet";
    
    /** Message when a user is not ranked for the competition. */
    private static final String       NOTRANKED         = "%b%c04%sender:%c12 "
                     + "%c04%who%c12 is currently in %c04unranked%c12 for the "
                     + "%c04%profile%c12 competition";

    /** Last 30 days message. */
    private static final String       LAST30DAYS        =
           "%b%c04(%c12Last 30 days on the %c04%profile%c12 profile%c04)%c12 "
       + "%c04%who%c12 highest bet was %c04%hb_chips%c12 on %c04%hb_game%c12 | "
       + "%c04%who%c12 bet total is %c04%hbt_chips%c12";
    
    
    /** No data for the last 30 days message. */
    private static final String       LAST30DAYS_NODATA = "%b%c04(%c12Last 30 "
            + "days on the %c04%profile%c12 profile%c04)%c12 There is no data "
            + "for %c04%who%c12 on this profile.";

    /** No competition running message. */
    private static final String       NOCOMPETITION     = "%b%c04%sender:%c12 "
           + "There is no competition running for the %c04%profile%c12 profile";

    /**
     * This method handles the position command.
     * 
     * @param event The message event
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        String sender = event.getUser().getNick();
        Channel chan = event.getChannel();

        if (isValidChannel(chan.getName()) && bot.userIsIdentified(sender)
                && Utils.startsWith(message, COMMAND)) {
            String[] msg = message.split(" ");
            if (msg.length == 2 || msg.length == CMD_LEN) {
                String who;
                if (msg.length == 2) {
                    who = sender;
                } else {
                    who = msg[2];
                }

                ProfileType profile = ProfileType.fromString(msg[1]);
                if (profile == null) {
                    bot.sendIRCMessage(chan.getName(), IrcBot.VALID_PROFILES);
                } else if (!profile.hasComps()) {
                    String out = NOCOMPETITION.replaceAll("%sender", sender);
                    out = out.replaceAll("%profile", profile.toString());
                    bot.sendIRCMessage(chan.getName(), out);
                } else {
                    BetterInfo better = null;
                    try {
                        better = DB.getInstance().competitionPosition(profile,
                                who);
                    } catch (Exception e) {
                        EventLog.log(e, "CompPosition", "message");
                    }

                    String out = "";
                    if (better.getPosition() == -1) {
                        out = NOTRANKED.replaceAll("%profile",
                                profile.toString());
                    } else {
                        out = POSITION.replaceAll("%profile",
                                profile.toString());
                        out = out.replaceAll("%position",
                                Integer.toString(better.getPosition()));
                        out = out.replaceAll("%chips",
                                Long.toString(better.getAmount()));
                    }

                    out = out.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", who);

                    bot.sendIRCMessage(chan.getName(), out);

                    DB db = DB.getInstance();
                    for (ProfileType prof : ProfileType.values()) {
                        if (profile.hasComps()) {
                            try {
                                BetterInfo highbet = db.getHighestBet(prof,
                                        who);
                                BetterInfo topbet = db.getTopBetter(prof,
                                        who);

                                if (highbet.getUser() == null
                                        || topbet.getUser() == null) {
                                    out = LAST30DAYS_NODATA;
                                    out = out.replaceAll("%who",
                                            who);
                                } else {
                                    out = LAST30DAYS.replaceAll("%hb_game",
                                            highbet.getGame().toString());
                                    out = out.replaceAll("%hb_chips",
                                            Long.toString(highbet.getAmount()));
                                    out = out.replaceAll("%hbt_chips",
                                            Long.toString(topbet.getAmount()));
                                    out = out.replaceAll("%who",
                                            highbet.getUser());
                                }
                                out = out.replaceAll("%profile",
                                        prof.toString());

                                bot.sendIRCNotice(sender, out);
                            } catch (Exception e) {
                                EventLog.log(e, "BetDetails", "run");
                            }
                        }
                    }
                }
            } else {
                bot.invalidArguments(sender, FORMAT);
            }
        }
    }

}
