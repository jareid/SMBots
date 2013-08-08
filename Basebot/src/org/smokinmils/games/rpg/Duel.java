package org.smokinmils.games.rpg;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Bet;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.Variables;

/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */


/**
 * Provides the functionality for basic duelling.
 * 
 * @author Jamie
 */
public class Duel extends Event {
    /** The dm command. */
    public static final String  DM_CMD        = "!dm";
    
    /** The cancel command. */
    private static final String  CXL_CMD         = "!ddcancel";
    
    /** The call command. */
    private static final String  CALL_CMD        = "!call";
    
    /** The train command. */
    public static final String  TRAIN_CMD       = "!train";


    
    /** The invalid bet message. */
    private static final String  INVALID_BET     = "%b%c12\"%c04!dd <amount> "
                                                 + "%c12\". Error, Invalid Bet";
    
    /** The invalid bet size message. */
    private static final String  INVALID_BETSIZE = "%b%c12You have to bet more"
                                                 + "than %c040%c12!";
    
    /** The bet cancelled message. */
    private static final String  BET_CANCELLED   = "%b%c04%username%c12: "
                                                 + "Cancelled your open wager";
    
    /** The message when you already have a bet open. */
    private static final String  OPEN_WAGER      = "%b%c04%username%c12: "
         + "You already have a wager open, Type %c04!ddcancel %c12to cancel it";
    
    /** No bet exsits message. */
    private static final String  NO_WAGER        = "%b%c04%username%c12: "
                                        + "I can't find a record of that wager";
    
    /** Not enough coins message. */
    private static final String  NO_CHIPS        = "%b%c04%username%c12: "
                                     + "You do not have enough coins for that!";
    
    /** Can't play against yourself message. */
    private static final String  NO_SELFPLAY     = "%b%c04%username%c12: "
                                           + "You can't play against yourself!";
    
    /** New bet made message.*/
    private static final String  NEW_WAGER       = "%b%c04%username%c12 has "
           + "opened a new dice duel wager of %c04%amount %proftype%c12 coins! "
           + "To call this wager type %c04!call %username";
    
    /** Play vs real message. */
    private static final String  PLAY_VS         = "%b%c04%username%c12: you "
                            + "need to use play coins to call a play coins dd!";
    
    /** Real vs play message. */
    private static final String  REAL_VS         = "%b%c04%username%c12: you "
                            + "need to use real coins to call a real coins dd!";
    
    /** Attack message. */
    private static final String  HIT            = "%b%c04%attacker%c12 attacked %c04%defender%c12"
                 + "and dealt a total of %c04%atkpoints%c12 damage but %c04%dfndpoints%c12 of that "
                 + "was deflected. %c04%defender%c12 now has %c04%health%c12";
    
    /** Attack message. */
    private static final String  WIN            = "%b%c04%winner%c12 annihilated %c04%defender%c12"
                                                + " and still has %c04%health%c12 health left."
                                                + " wins the %c04%amount%c12 chip pot!";
    
    /** Open wages list message. */
    private static final String  OPEN_WAGERS     = "%b%c12Current open wagers: "
                             + "%wagers. To call a wager type %c04!call <name>";
    
    /** Open wage line. */
    private static final String  WAGER           = "%c04%username%c12 (%c04%amount %proftype%c12) ";
    
    /** Minutes between announcements. */
    private static final int     ANNOUNCEDELAY   = 3;

    /** Random number. */
    private static final int    ATTACK = 50;
    
    /** Random number. */
    private static final int    DEFENSE = 50;
    
    /** Health number. */
    private static final int    HEALTH = 100;
    
    
    /** All the open bets. */
    private final ArrayList<Bet> openBets;
    
    /**
     * Constructor.
     * @param bot    The irc bot.
     * @param channel The channel the game will run on
     */
    public Duel(final IrcBot bot, final String channel) {
        openBets = new ArrayList<Bet>();

        Timer announce = new Timer(true);
        announce.scheduleAtFixedRate(new Announce(bot, channel),
                                     ANNOUNCEDELAY * Utils.MS_IN_MIN,
                                     ANNOUNCEDELAY * Utils.MS_IN_MIN);
    }

    /**
     * This method handles the commands.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        
        if (isValidChannel(chan.getName())
                && bot.userIsIdentified(sender)) {
            try {
                if (Utils.startsWith(message, DM_CMD)) {
                        newDM(event);
                } else if (Utils.startsWith(message, CXL_CMD)) {
                    cancel(event);
                } else if (Utils.startsWith(message, CALL_CMD)) {
                    call(event);
                } else if (Utils.startsWith(message, TRAIN_CMD)) {
                    train(event);
                }
            } catch (Exception e) {
                EventLog.log(e, "Duel", "message");
            }
        }
    }

    /**
     * Let's the user train a certain skill.
     * 
     * Limited to twice per day.
     * 
     * @param event The message event.
     */
    private void train(final Message event) {
        
    }
    
    /**
     * Creates a new duel match.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when creating the Bet object failed.
     */
    private void newDM(final Message event) throws SQLException {
        User user = event.getUser();
        String username = user.getNick();
        DB db = DB.getInstance();
        String[] msg = event.getMessage().split(" ");
        IrcBot bot = event.getBot();
        Channel channel = event.getChannel();

        if (msg.length < 2) {
            bot.sendIRCMessage(channel, INVALID_BET);
        } else {
            // check if they already have an open bet
            boolean found = false;
            for (Bet bet : openBets) {
                if (bet.getUser().compareTo(user) == 0) {
                    bot.sendIRCMessage(channel, OPEN_WAGER.replaceAll("%username", username));
                    found = true;
                }
            }

            if (!found) {
                // attempt to parse the amount
                Double amount = Utils.tryParseDbl(msg[1]);
                double betsize = Utils.checkCredits(user, amount, bot, channel);
                if (amount == null) {
                    bot.sendIRCMessage(channel, INVALID_BET);
                } else if (amount <= 0) {
                    bot.sendIRCMessage(channel, INVALID_BETSIZE);
                } else if (amount > Variables.MAXBET) {
                    bot.maxBet(user, channel, Variables.MAXBET);
                } else if (betsize > 0.0) { // add bet, remove chips,notify channel
                    ProfileType profile = db.getActiveProfile(username);
                    Bet bet = new Bet(user, profile, GamesType.DICE_DUEL, betsize, "");
                    openBets.add(bet);

                    String out = NEW_WAGER.replaceAll("%username", username);
                    out = out.replaceAll("%amount", Utils.chipsToString(betsize));
                    String ptype = "real";
                    if (bet.getProfile() == ProfileType.PLAY) {
                        ptype = "play";
                    }
                    out = out.replaceAll("%proftype", ptype);
                    bot.sendIRCMessage(channel, out);
                } else {
                    bot.sendIRCMessage(channel, NO_CHIPS.replaceAll("%username", username));
                }
            }
        }
    }
    
    /**
     * This method handles the call command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when the system failed to perform db tasks
     */
    private void call(final Message event)
        throws SQLException {
        User user = event.getUser();
        String username = user.getNick();
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        Channel channel = event.getChannel();

        DB db = DB.getInstance();
        String caller = username; // user who is calling
        String better =  null;
        if (msg.length >= 2) {
            better = msg[1];
        }

        // check to see if someone is playing themselves...
        if (caller.equalsIgnoreCase(better)) {
            bot.sendIRCMessage(channel, NO_SELFPLAY.replaceAll("%username", username));
        } else if (better != null) {
            Bet found = null;
            boolean foundb = false;
            for (Bet bet : openBets) {
                if (bet.getUser().getNick().equalsIgnoreCase(better)) {
                    found = bet;
                    foundb = true;
                    ProfileType cprof = db.getActiveProfile(caller);

                    // quick hax to check if play chips vs non-play chips!
                    if (cprof != ProfileType.PLAY && bet.getProfile() == ProfileType.PLAY) {
                        bot.sendIRCMessage(channel, PLAY_VS.replaceAll("%username", username));
                        found = null;
                    } else if (cprof == ProfileType.PLAY && bet.getProfile() != ProfileType.PLAY) {
                        bot.sendIRCMessage(channel, REAL_VS.replaceAll("%username", username));
                        found = null;
                    } else if (db.checkCredits(caller) < bet.getAmount()) {
                        bot.sendIRCMessage(channel, NO_CHIPS.replaceAll("%username", username));
                        found = null;
                    } else {
                        // play this wager
                        bet.call(caller, cprof);
                        
                        String player1 = better;
                        String player2 = caller;
                        int active = Random.nextInt(2);

                        int h1 = HEALTH;
                        int h2 = HEALTH;
                        while (h1 > 0 && h2 > 0) {
                            int attack = Random.nextInt(ATTACK) + 1;
                            int defense = Random.nextInt(DEFENSE) + 1;
                            int total = attack - defense;

                            String attacker, defender;
                            int dfndh = 0;
                            if (active == 0) {
                                h2 -= total;
                                dfndh = h2;
                                attacker = player1;
                                defender = player2;
                            } else {
                                h1 -= total;
                                dfndh = h1;
                                attacker = player2;
                                defender = player1;
                            }
                            
                            if (dfndh < 0) {
                                dfndh = 0;
                            }
                            
                            String out = HIT.replaceAll("%attacker", attacker);
                            out = out.replaceAll("%defender", defender);
                            out = out.replaceAll("%atkpoints", Integer.toString(attack));
                            out = out.replaceAll("%dfndpoints", Integer.toString(defense));
                            out = out.replaceAll("%health", Integer.toString(dfndh));
                            bot.sendIRCMessage(channel, out);
                            
                            active = (active + 1) % 2;
                        }

                        // Calculate rake.
                        double rake = Rake.getRake(caller, bet.getAmount(), cprof) + bet.getRake();
                        double win = (bet.getAmount() * 2) - rake;
                        
                        String winner, loser;
                        int winh;
                        if (h1 <= 0) { // caller wins
                            winner = caller;
                            loser = better;
                            winh = h2;
                            bet.lose(caller, cprof, win);
                        } else { // better wins
                            winner = better;
                            loser = caller;
                            winh = h1;
                            bet.win(win);
                        }

                        // jack pot stuff
                        bet.checkJackpot(bot);
                        if (Rake.checkJackpot(bet.getAmount())) {
                            ArrayList<String> players = new ArrayList<String>();
                            players.add(caller);
                            Rake.jackpotWon(cprof, GamesType.DICE_DUEL, players, bot, null);
                        }

                        String out = WIN.replaceAll("%winner", winner);
                        out = out.replaceAll("%loser", loser);
                        out = out.replaceAll("%amount", Utils.chipsToString(win));
                        out = out.replaceAll("%health", Integer.toString(winh));
                        bot.sendIRCMessage(channel, out);
                    }
                    break;
                }
            }

            if (!foundb) {
                // if we reach here the game doesn't exist
                bot.sendIRCMessage(channel, NO_WAGER.replaceAll("%username", username));
            }

            if (found != null) {
                found.close();
                openBets.remove(found);
            }
        }
    }
    
    /**
     * This method handles the cancel command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when the Bet objecty failed to cancel.
     */
    private void cancel(final Message event) throws SQLException {
        User user = event.getUser();
        String username = user.getNick();
        Channel channel = event.getChannel();

        // try to locate and cancel the bet else ignore
        Bet found = null;
        for (Bet bet : openBets) {
            if (bet.getUser().compareTo(user) == 0) {
                found = bet;
                break;
            }
        }
        if (found != null) {
            found.cancel();
            openBets.remove(found);
            event.getBot().sendIRCMessage(channel, BET_CANCELLED.replaceAll("%username", username));
        }
    }

    /**
     * Simple extension to time task to deal with game triggers.
     * 
     * @author cjc
     */
    class Announce extends TimerTask {
        /** The IRC bot. */
        private final IrcBot irc;
        
        /** The IRC channel. */
        private final String channel;

        /**
         * Constructor.
         * @param ib   The irc bot/server.
         * @param chan The channel.
         */
        public Announce(final IrcBot ib, final String chan) {
            irc = ib;
            channel = chan;
        }

        @Override
        public void run() {
            if (openBets.size() > 0) {
                String wagers = "";
                for (Bet bet : openBets) {
                    String prof = "play";
                    if (bet.getProfile() != ProfileType.PLAY) {
                        prof = "real";
                    }

                    wagers += WAGER.replaceAll("%proftype", prof);
                    wagers = wagers.replaceAll("%username", bet.getUser().getNick());
                    wagers = wagers.replaceAll("%amount", Utils.chipsToString(bet.getAmount()));
                }             
                
                Channel chan = irc.getUserChannelDao().getChannel(channel);
                
                String line = OPEN_WAGERS.replaceAll("%wagers", wagers);
                irc.sendIRCMessage(chan, line);
            }
        }
    }
}
