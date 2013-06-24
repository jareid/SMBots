/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

import java.sql.SQLException;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the lottery functionality.
 * 
 * @author Jamie
 */
public class Lottery extends Event {
    /** The command. */
    public static final String   COMMAND        = "!ticket";

    /** The command description. */
    public static final String   DESC           = "%b%c12Buys a quantity of "
                                     + "lottery tickets for the active profile";
    
    /** The command format. */
    public static final String   FORMAT         = "%b%c12" + COMMAND
                                                           + " <quantity>";
    /** The command length. */
    public static final int     CMD_LEN     = 4;

    /** Message when someone buys tickets. */
    private static final String  BOUGHTTICKETS  = "%b%c01The %c04%profile%c01 "
            + "Weekly Lottery is now at a total of %c04%amount%c01 coins! It's "
            + "1 coin per ticket, %c04%percent%%c01 of the pot is paid out. "
            + "Time to draw: %c04%timeleft%c01. To buy 1 ticket with your " 
            + "active profile type %c04%cmd 1";
    
    /** Message for a lottery ending. */
    private static final String  LOTTERYENDED   = "%b%c01The %c04%profile%c01 "
            + "Weekly Lottery has now ended! This week's winner was " 
            + "%c04%winner%c01 and they won %c04%amount%c01 coins!";
    
    /** Message for a new lottery beginning. */
    private static final String  RESET          = "%b%c01A new weekly lottery "
            + "has begun! It's 1 coin per ticket, %c04%percent%%c01 of the pot "
            + "is paid out. Time to draw: %c04%timeleft%c01. To buy 1 ticket "
            + "with your active profile type %c04%cmd 1";

    /** Percentage of tickets given to winner. */
    private static final int     LOTTERYPERCENT = 90;

    /** Figure to convert to a percent. */
    private static final double  PERCENT_CONV   = 0.01;

    /** Denotes if the lottery is enabled or not. */
    private static final boolean ENABLED        = false;

    /**
     * This method handles the command.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        if (isValidChannel(chan.getName())
                && bot.userIsIdentified(senderu)
                && Utils.startsWith(message, COMMAND)) {
            String[] msg = message.split(" ");
            if (msg.length == 2) {
                Integer amount = Utils.tryParse(msg[1]);

                if (amount != null && amount > 0) {
                    try {
                        DB db = DB.getInstance();
                        int chips = db.checkCreditsAsInt(sender);
                        ProfileType profile = db.getActiveProfile(sender);
                        if (amount > chips) {
                            bot.noChips(senderu, amount, profile);
                        } else {
                            boolean res = db.buyLotteryTickets(
                                    sender, profile, amount);
                            if (res) {
                                announceLottery(bot, profile, chan.getName());
                            } else {
                                EventLog.log(
                                        "Failed to buy lottery tickets for "
                                                + sender, "Lottery", "message");
                            }
                        }
                    } catch (Exception e) {
                        EventLog.log(e, "TransferChips", "message");
                    }
                } else {
                    bot.invalidArguments(senderu, FORMAT);
                }
            } else {
                bot.invalidArguments(senderu, FORMAT);
            }
        }
    }

    /**
     * Announces a the current lottery details.
     * 
     * @param bot The IRC bot/server for this lottery
     * @param profile The profile this msg is for.
     * @param channel The channel to announce to.
     * 
     * @throws SQLException when there is a database issue.
     */
    public static void announceLottery(final IrcBot bot,
                                       final ProfileType profile,
                                       final String channel)
        throws SQLException {
        if (!ENABLED) {
            return;
        }

        DB db = DB.getInstance();
        int amount = db.getLotteryTickets(profile);
        if (amount > 0) {
            int secs = db.getCompetitionTimeLeft();
            String duration = Utils.secondsToString(secs);

            String out = BOUGHTTICKETS.replaceAll(
                    "%profile", profile.toString());
            out = out.replaceAll("%timeleft", duration);
            out = out.replaceAll("%amount", Integer.toString(amount));
            out = out.replaceAll("%percent", Integer.toString(LOTTERYPERCENT));
            out = out.replaceAll("%cmd", COMMAND);
            
            Channel chan = bot.getUserChannelDao().getChannel(channel);
            bot.sendIRCMessage(chan, out);
        }
    }

    /**
     * Ends the current lottery.
     * 
     * @param bot The IRC bot/server for this lottery
     * @param channel The channel to announce to.
     * 
     */
    public static void endLottery(final IrcBot bot,
                                  final String channel) {
        if (!ENABLED) {
            return;
        }

        try {
            DB db = DB.getInstance();
            Channel chan = bot.getUserChannelDao().getChannel(channel);
            for (ProfileType profile : ProfileType.values()) {
                int amount = db.getLotteryTickets(profile);
                if (amount > 0) {
                    amount = (int) Math.round(amount
                            * (LOTTERYPERCENT * PERCENT_CONV));

                    String winner = db.getLotteryWinner(profile);

                    db.adjustChips(
                            winner, amount, profile, GamesType.LOTTERY,
                            TransactionType.LOTTERY_WIN);

                    String out = LOTTERYENDED.replaceAll(
                            "%profile", profile.toString());
                    out = out.replaceAll("%winner", winner);
                    out = out.replaceAll("%amount", Integer.toString(amount));
                    bot.sendIRCMessage(chan, out);
                }
            }

            // Start next week's lottery.
            db.endLottery();
        } catch (Exception e) {
            EventLog.log(e, "Lottery", "endLottery");
        }
    }

    /**
     * Announces a new lottery.
     * 
     * @param bot The IRC bot/server for this lottery
     * @param channel The channel to announce to.
     * 
     * @throws SQLException when there is a database issue.
     */
    public static void announceReset(final IrcBot bot,
                                     final String channel)
        throws SQLException {
        if (!ENABLED) {
            return;
        }

        int secs = DB.getInstance().getCompetitionTimeLeft();
        String duration = Utils.secondsToString(secs);

        String out = RESET.replaceAll("%timeleft", duration);
        out = out.replaceAll("%percent", Integer.toString(LOTTERYPERCENT));
        out = out.replaceAll("%cmd", COMMAND);

        Channel chan = bot.getUserChannelDao().getChannel(channel);
        bot.sendIRCMessage(chan, out);
    }
}
