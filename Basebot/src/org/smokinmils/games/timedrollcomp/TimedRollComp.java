package org.smokinmils.games.timedrollcomp;

/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.database.types.UserCheck;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for a timed roll.
 * 
 * @author Jamie
 */
public class TimedRollComp extends Event {
    /** The roll command. */
    public static final String               CMD         = "!roll";
    
    /** The message when someone rolls. */
    public static final String               ROLLED          = "%b%c04%who%c12 "
                           + "has used his free roll and rolled a... %c04%roll%c12.";

    /** The message when someone rolls. */
    public static final String               NEWLEADER       = "%b%c04%winner"
                                     + "%c12 is now in the lead with %c04%roll";
    
    /** The message when there is one winner. */
    public static final String               SINGLEWINNER    = "%b%c04%winner"
          + "%c01 has won this round with a roll of %c04%roll%c01 and has been "
          + "awarded %c04%coins %profile%c01 coins.";
    
    /** The message when there is one winner. */
    public static final String               CANTWIN    = "%b%c04%winner"
          + "%c01 has won this round with a roll of %c04%roll%c01 and can't be"
          + "awarded %c04%coins %profile%c01 coins. Please message an admin!";
    
    /** The message when there is more than one winner. */
    public static final String               MULTIPLEWINS = "%b%c04%winner%c01"
                   + " have won this round with rolls of %c04%roll%c01 and have"
                   + "been awarded %c04%coins %profile%c01 coins each.";

    /** The message when a new round is started. */
    public static final String               NEWGAME         = "%b%c12A new "
                                 + "round has begun, use %c04%cmd%c12 to roll.";

    /** The highest possible roll number. */
    public static final int                  MAXROLL         = 1000;

    /** The channel this game is for. */
    private String                           validChan;
    
    /** The irc bot this game is running on. */
    private IrcBot                           irc;
    
    /** The timer used to run this game. */
    private Timer                            gameTimer;
    
    /** The map of user's rolls. */
    private SortedMap<Integer, List<String>> rolls;
    
    /** List of users who have rolled. */
    private Map<String, String>            userlist;
    
    /** The prize for each round. */
    private int                              prize;
    
    /** The profile this game awards prizes for. */
    private ProfileType                      profile;
    
    /** The number of rounds to play. */
    private Integer                          rounds;
    
    /** The number of rounds already played. */
    private int                              roundsRun;
    
    /** The creater of this game, can be null. */
    private CreateTimedRoll                  parent;

    /**
     * Constructor.
     * 
     * @param bot       The irc bot.
     * @param channel   The irc channel to run on.
     * @param prof      The profile prizes are for.
     * @param prze      The prize in chips.
     * @param mins      The number of mins for each round.
     * @param rnds      The number of rounds to play.
     * @param ctr       The parent object.
     */
    public TimedRollComp(final IrcBot bot, final String channel,
            final ProfileType prof, final int prze, final int mins,
            final int rnds, final CreateTimedRoll ctr) {
        // check the channel is a valid IRC Channel name
        if (!channel.matches("([#&][^\\x07\\x2C\\s]{1,200})")) {
            throw new IllegalArgumentException(channel
                    + " is not a valid IRC channel name");
        } else if (mins <= 0) {
            throw new IllegalArgumentException(Integer.toString(mins)
                    + " minutes is less than or equal to 0");
        } else if (prze <= 0) {
            throw new IllegalArgumentException("The prize, "
                    + Integer.toString(prize) + ", is less than or equal to 0");
        } else if (prof == null) {
            throw new IllegalArgumentException(IrcBot.VALID_PROFILES);
        } else {
            validChan = channel;
            prize = prze;
            rolls = new TreeMap<Integer, List<String>>();
            userlist = new HashMap<String, String>();
            irc = bot;
            profile = prof;
            rounds = rnds;
            roundsRun = 0;
            parent = ctr;

            irc.sendIRC().joinChannel(validChan);

            // TODO: check this new way works
            //bot.getConfiguration().getListenerManager().addListener(this);
            bot.getListenerManager().addListener(this);
            
            // Start the timer
            gameTimer = new Timer(true);
            gameTimer.scheduleAtFixedRate(new TimedRollTask(),
                                          mins * Utils.MS_IN_MIN,
                                          mins * Utils.MS_IN_MIN);
        }
    }

    /**
     * Ends the game prematurely.
     */
    public final void close() {
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        
        boolean isvalid = irc.getValidChannels().contains(validChan.toLowerCase());
        if (!isvalid) {
            Channel chan = irc.getUserChannelDao().getChannel(validChan);
            if (chan == null) {
                irc.sendRaw().rawLine("PART " + validChan
                                + "Game was ended! Join #SMGamer for more!!!");
            } else {
                chan.send().part("Game was ended! Join #SMGamer for more!!!");
            }
        }
        
        // TODO: check this new way works
        //irc.getConfiguration().getListenerManager().removeListener(this);
        irc.getListenerManager().removeListener(this);
    }

    /**
     * Handles the command.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        Channel chan = event.getChannel();
        User senderu = event.getUser();
        String sender =  senderu.getNick();
        String host = event.getUser().getHostmask();

        synchronized (this) {
            if (Utils.startsWith(message, CMD)
                    && validChan.equalsIgnoreCase(chan.getName())) {
                if ((!userlist.containsKey(sender) && !userlist.containsValue(host))) {
                    int userroll = Random.nextInt(MAXROLL);

                    String out = ROLLED.replaceAll("%who", sender);
                    out = out.replaceAll("%roll", Integer.toString(userroll));
                    bot.sendIRCMessage(chan, out);

                    if (rolls.isEmpty() || rolls.lastKey() < userroll) {
                        out = NEWLEADER.replaceAll("%winner", sender);
                        out = out.replaceAll(
                                "%roll", Integer.toString(userroll));
                        irc.sendIRCMessage(chan, out);
                    }

                    // add to the map
                    List<String> users = rolls.get(userroll);
                    if (users == null) {
                        users = new ArrayList<String>();
                    }
                    users.add(sender);
                    rolls.put(userroll, users);
                    userlist.put(sender, host);
                }
            }
        }
    }

    /** 
     * Ends the current round.
     */
    private synchronized void processRound() {
        // Decide who has won and give them their prize.
        Channel vchan = irc.getUserChannelDao().getChannel(validChan);
        if (!rolls.isEmpty() && rolls.size() > 1) {
            Integer winroll = rolls.lastKey();
            List<String> winners = rolls.get(winroll);

            double win = prize;
            String out;
            String winstr;
            if (winners.size() == 1) {
                // Single winner
                out = SINGLEWINNER;
                winstr = winners.get(0);
            } else {
                // Split winnings
                win = prize / winners.size();
                out = MULTIPLEWINS;
                winstr = Utils.listToString(winners);
            }

            out = out.replaceAll("%winner", winstr);
            out = out.replaceAll("%roll", Integer.toString(winroll));
            out = out.replaceAll("%coins", Utils.chipsToString(win));
            out = out.replaceAll("%profile", profile.toString());

            irc.sendIRCMessage(vchan, out);

            for (String winner : winners) {
                DB db = DB.getInstance();
                try {
                    UserCheck ckusr = db.checkUserExists(winner, userlist.get(winner));
                    if (ckusr == UserCheck.FAILED) {
                        out = CANTWIN.replaceAll("%winner", winstr);
                        out = out.replaceAll("%roll", Integer.toString(winroll));
                        out = out.replaceAll("%coins", Utils.chipsToString(win));
                        out = out.replaceAll("%profile", profile.toString());

                        irc.sendIRCMessage(vchan, out);
                    }
                    db.adjustChips(winner, win, profile, GamesType.TIMEDROLL, TransactionType.WIN);
                } catch (Exception e) {
                    EventLog.log(e, "TimedRollComp", "processRound");
                }
            }
        }
        // Clear the rolls for the next round
        rolls.clear();
        userlist.clear();

        roundsRun++;
        if (parent != null && rounds != -1 && roundsRun == rounds) {
            parent.endRollGame(validChan, irc);
            close();
        } else {
            irc.sendIRCMessage(vchan, NEWGAME.replaceAll("%cmd", CMD));
        }
    }

    /** 
     * Timer task to process a round of the game.
     * 
     * @author Jamie
     */
    public class TimedRollTask extends TimerTask {
        @Override
        public final void run() {
            processRound();
        }
    }
}
