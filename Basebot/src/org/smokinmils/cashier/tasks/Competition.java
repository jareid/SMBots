/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.tasks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.cashier.commands.Lottery;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.BetterInfo;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Provides announcements about the competition for the top 5 total betters.
 * 
 * @author Jamie
 */
public class Competition extends TimerTask {
    /** The output message for the statistics. */
    private static final String ANNOUNCE_LINE        = "%b%c01[%c04Weekly Top "
               + "Better Competition%c01] | [%c04%profile%c01] "
               + "| Prizes: %c04%prizes %c01| Current leaders: %players "
               + "| Time left: %c04%timeleft";

    /** The winner announce message. */
    private static final String WINNER_ANNOUNCE_LINE = "%b%c01[%c04Weekly Top "
            + "Better Competition%c01] | [%c04%profile%c01] The weekly "
            + "competition has ended! Congratulations to %players on their "
            + "prizes";
    
    /** The announce line for each user. */
    private static final String WINNER_USERLINE      = "%c04%who%c01";

    /** The number of winners in the competition. */
    private static final int    NUMBERWINNERS        = 5;

    /** The amount of minutes between announcements. */
    private static final int    ANNOUNCEMINS         = 45;

    /** The irc bot used to announce. */
    private final IrcBot        bot;

    /** The channel to announce to. */
    private final String        channel;

    /** The number of times this has run. */
    private int                 runs;

    /**
     * Constructor.
     * 
     * @param irc The irc bot
     * @param chan The announce channel
     */
    public Competition(final IrcBot irc, final String chan) {
        bot = irc;
        channel = chan;
        runs = 0;
    }

    /**
     * (non-Javadoc).
     * @see java.util.TimerTask#run()
     */
    @Override
    public final void run() {
        runs++;
        // check whether to announce or end
        boolean over = false;
        try {
            over = (DB.getInstance().getCompetitionTimeLeft() <= 0);
        } catch (Exception e) {
            EventLog.log(e, "Competition", "run");
        }

        if (over) {
            end();
        } else if (runs == ANNOUNCEMINS) {
            announce();
            runs = 0;
        }
    }

    /**
     * Announce the current leaders.
     */
    private void announce() {
        DB db = DB.getInstance();

        String duration = "";
        try {
            int secs = db.getCompetitionTimeLeft();
            duration = Utils.secondsToString(secs);

        } catch (Exception e) {
            EventLog.log(e, "Competition", "run");
        }

        for (ProfileType profile : ProfileType.values()) {
            try {
                // Announce the current lottery
                Lottery.announceLottery(bot, profile, channel);
            } catch (Exception e) {
                EventLog.log(e, "Competition", "run");
            }
        }

        Map<ProfileType, List<Integer>> allprizes = readPrizes();
        for (ProfileType profile : ProfileType.values()) {
            if (profile.hasComps()) {
                try {
                    List<BetterInfo> betters = db.getCompetition(
                            profile, NUMBERWINNERS);

                    List<Integer> prizes = allprizes.get(profile);
                    // check there are enough prizes
                    if (prizes == null || prizes.size() < betters.size()) {
                        EventLog.log(
                                "Not enough prizes for " + profile.toString(),
                                "Competition", "end");
                        bot.sendIRCMessage(
                                channel,
                                "%b%c04No competition prizes set for + "
                                        + profile.toString()
                                        + ", please talk to an admin");
                        continue;
                    }

                    String prizestr = Utils.listToString(prizes);
                    String allwins = Utils.listToString(betters);

                    String out = ANNOUNCE_LINE.replaceAll(
                            "%profile", profile.toString());
                    out = out.replaceAll("%timeleft", duration);
                    out = out.replaceAll("%prizes", prizestr);
                    out = out.replaceAll("%players", allwins);

                    bot.sendIRCMessage(channel, out);
                } catch (Exception e) {
                    EventLog.log(e, "Competition", "run");
                }
            }
        }
    }

    /**
     * End the competition.
     */
    private void end() {
        // End weekly lottery
        Lottery.endLottery(bot, channel);

        // End the competition
        DB db = DB.getInstance();
        Map<ProfileType, List<Integer>> allprizes = readPrizes();
        for (ProfileType profile : ProfileType.values()) {
            if (profile.hasComps()) {
                try {
                    List<BetterInfo> betters = db.getCompetition(
                            profile, NUMBERWINNERS);

                    String out = WINNER_ANNOUNCE_LINE.replaceAll(
                            "%profile", profile.toString());
                    String allwins = "";

                    List<Integer> prizes = allprizes.get(profile);
                    // check there are enough prizes
                    if (prizes == null || prizes.size() < betters.size()) {
                        EventLog.log(
                                "Not enough prizes for " + profile.toString(),
                                "Competition", "end");
                        bot.sendIRCMessage(
                                channel,
                                "%b%c04No competition prizes set for + "
                                        + profile.toString()
                                        + ", please talk to an admin");
                        continue;
                    }

                    int i = 0;
                    for (BetterInfo player : betters) {
                        int prize = prizes.get(i);

                        String winner = WINNER_USERLINE.replaceAll(
                                "%who", player.getUser());
                        allwins += winner;
                        if (i == (betters.size() - 2)) {
                            allwins += " and ";
                        } else if (i < (betters.size() - 2)) {
                            allwins += ", ";
                        }

                        db.adjustChips(
                                player.getUser(), prize, profile,
                                GamesType.COMPETITIONS, TransactionType.WIN);

                        i++;
                    }

                    out = out.replaceAll("%players", allwins);

                    bot.sendIRCMessage(channel, out);
                } catch (Exception e) {
                    EventLog.log(e, "Competition", "end");
                }
            }
        }

        // Update the database for next competition
        try {
            db.competitionEnd();
            Lottery.announceReset(bot, channel);
        } catch (Exception e) {
            EventLog.log(e, "Competition", "end");
        }
    }

    /**
     * Read the prizes from a file.
     * 
     * @return The map of prizes.
     */
    private Map<ProfileType, List<Integer>> readPrizes() {
        Map<ProfileType, List<Integer>> results =
                new HashMap<ProfileType, List<Integer>>();

        for (ProfileType profile : ProfileType.values()) {
            if (profile.hasComps()) {
                List<Integer> prizes = new ArrayList<Integer>();
                // read the prizes from a file
                try {
                    BufferedReader readFile = new BufferedReader(
                            new FileReader("comp_" + "prizes."
                                    + profile.toString()));
                    String line = "";
                    while ((line = readFile.readLine()) != null) {
                        prizes.add(Utils.tryParse(line));
                    }
                    readFile.close();
                } catch (IOException e) {
                    EventLog.log(e, "Competition", "end");
                    EventLog.log(
                            "No data for " + profile.toString(), "Competition",
                            "end");
                    bot.sendIRCMessage(
                            channel, "%b%c04No competition prizes set for + "
                                    + profile.toString()
                                    + " please talk to an admin");
                    continue;
                }
                results.put(profile, prizes);
            }
        }
        return results;
    }
}
