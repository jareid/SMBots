/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.tasks;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.pircbotx.Channel;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.cashier.ManagerSystem;
import org.smokinmils.logging.EventLog;

/**
 * Provides announcements about the betting on an irc server.
 * 
 * @author Jamie
 */
public class ManagerAnnounce extends TimerTask {
    /** The bot that is announcing. */
    private final IrcBot        bot;

    /** The channel for the announcements. */
    private final String        channel;

    /** The timer used to announce. */
    private Timer               announceTimer;

    /** The list of messages to announce. */
    private final List<String>  messages;

    /** The list of intervals for each message. */
    private final List<Integer> intervals;

    /**The directory the messages are stored in. */
    private static final String DIRNAME         = "settings";
    
    /** The file name the messages are stored in. */
    private static final String FILENAME         = "messages.ini";

    /** The default interval. */
    private static final int    DEFAULT_INTERVAL = 1;

    /** MS in a minute. */
    private static final int    MIN              = 60 * 1000;

    /**
     * Constructor.
     * 
     * @param irc The irc bot for this manager system.
     * @param chan The channel for announcements
     */
    public ManagerAnnounce(final IrcBot irc, final String chan) {
        bot = irc;
        channel = chan;
        intervals = new ArrayList<Integer>();
        messages = new ArrayList<String>();
        readData();
    }

    /**
     * Constructor.
     * 
     * @param irc The irc bot for this manager system.
     * @param chan The channel for announcements
     * @param intervls The intervals between announcements
     * @param msgs The messages to announce.
     * @param next The interval to the next announcement.
     */
    public ManagerAnnounce(final IrcBot irc, final String chan,
            final List<Integer> intervls, final List<String> msgs,
            final int next) {
        bot = irc;
        channel = chan;
        intervals = intervls;
        messages = msgs;
        begin(next);
    }

    /**
     * Initialise the system.
     * @param next the interval between announcements.
     */
    public final void begin(final int next) {
        int interval = next;
        if (interval <= 0) {
            interval = DEFAULT_INTERVAL;
        }
        announceTimer = new Timer(true);
        announceTimer.schedule(this, next * MIN);
    }

    /**
     * (non-Javadoc).
     * @see java.util.TimerTask#run()
     */
    @Override
    public final void run() {
        String out = null;
        Integer interval = DEFAULT_INTERVAL;
        if (messages.size() >= 1 && intervals.size() >= 1) {
            out = messages.remove(0);
            interval = intervals.remove(0);
            
            Channel chan = bot.getUserChannelDao().getChannel(channel);
            out = out.replaceAll("%manager", ManagerSystem.getLoggedInUser());
            bot.sendIRCMessage(chan, out);
        }

        if (messages.size() == 0) {
            intervals.clear();
            readData();
        }

        if (announceTimer != null) {
            announceTimer.cancel();
        }
        new ManagerAnnounce(bot, channel, intervals, messages, interval);
    }

    /**
     * Read the logged in managers from file.
     */
    private void readData() {
        try {
            File dir = new File(DIRNAME);
            File file = new File(dir, FILENAME);
            Ini ini = new Ini(new FileReader(file));

            for (String name : ini.keySet()) {
                Section section = ini.get(name);
                String msg = section.get("message");
                Integer interval = section.get("interval", Integer.class);
                if (msg == null) {
                    EventLog.log(
                            name + " has no message", "ManagerAnnounce",
                            "readData");
                } else if (interval == null) {
                    EventLog.log(
                            name + " has no interval", "ManagerAnnounce",
                            "readData");
                } else {
                    messages.add(msg);
                    intervals.add(interval);
                }
            }
        } catch (IOException e) {
            EventLog.log(e, "ManagerAnnounce", "readData");
        }
    }
}
