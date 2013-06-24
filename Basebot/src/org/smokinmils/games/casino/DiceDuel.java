package org.smokinmils.games.casino;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.BaseBot;
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
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.Variables;

/**
 * Class the provide a roulette game.
 * 
 * @author Jamie
 */
public class DiceDuel extends Event {
    /** The dd command. */
    private static final String  BET_CMD         = "!dd";
    
    /** The cancel command. */
    private static final String  CXL_CMD         = "!ddcancel";
    
    /** The call command. */
    private static final String  CALL_CMD        = "!call";

    /** THe invalid bet message. */
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
           + "opened a new dice duel wager of %c04%amount %proftype%c12  coins!"
           + "To call this wager type %c04!call %username";
    
    /** Play vs real message. */
    private static final String  PLAY_VS         = "%b%c04%username%c12: you "
                            + "need to use play coins to call a play coins dd!";
    
    /** Real vs play message. */
    private static final String  REAL_VS         = "%b%c04%username%c12: you "
                            + "need to use real coins to call a real coins dd!";
    
    /** Bet called message. */
    private static final String  ROLL            = "%b%c04%winner%c12 rolled "
                 + "%c04%windice%c12, %c04%loser%c12 rolled %c04%losedice%c12. "
                 + "%c04%winner%c12 wins the %c04%amount%c12 chip pot!";
    
    /** Open wages list message. */
    private static final String  OPEN_WAGERS     = "%b%c12Current open wagers: "
                             + "%wagers. To call a wager type %c04!call <name>";
    
    /** Open wage line. */
    private static final String  WAGER           = "%c04%username%c12(%c04"
                                                 + "%amount %proftype%c12) ";

    /** Minutes between announcements. */
    private static final int     ANNOUNCEDELAY   = 3;

    /** Random number. */
    private static final int    RANDOM = 6;
    
    /** All the open bets. */
    private final ArrayList<Bet> openBets;

    /**
     * Constructor.
     * 
     * @param bot    The irc bot.
     * @param channel The channel the game will run on
     */
    public DiceDuel(final IrcBot bot, final String channel) {
        openBets = new ArrayList<Bet>();

        Timer announce = new Timer(true);
        announce.scheduleAtFixedRate(
                new Announce(bot, channel), ANNOUNCEDELAY * Utils.MS_IN_MIN,
                ANNOUNCEDELAY * Utils.MS_IN_MIN);
    }

    /**
     * This method handles the dice duel commands.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        String message = event.getMessage();
        User sender = event.getUser();

        synchronized (BaseBot.getLockObject()) {
            if (isValidChannel(event.getChannel().getName())
                    && event.getBot().userIsIdentified(sender)) {
                try {
                    if (Utils.startsWith(message, CXL_CMD)) {
                        cancel(event);
                    } else if (Utils.startsWith(message, BET_CMD)) {
                        dd(event);
                    } else if (Utils.startsWith(message, CALL_CMD)) {
                        call(event);
                    }
                } catch (Exception e) {
                    EventLog.log(e, "DiceDuel", "message");
                }
            }
        }
    }
    
    /**
     * This method handles the dd command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when the system failed to perform db tasks
     */
    private void dd(final Message event)
        throws SQLException {
        User user = event.getUser();
        String username = user.getNick();
        DB db = DB.getInstance();
        String[] msg = event.getMessage().split(" ");
        IrcBot bot = event.getBot();
        Channel channel = event.getChannel();

        if (msg.length < 2) {
            bot.sendIRCMessage(channel, INVALID_BET);
        } else {
            // check if they already have an openbet
            boolean found = false;
            for (Bet bet : openBets) {
                if (bet.getUser().compareTo(user) == 0) {
                    bot.sendIRCMessage(channel,
                            OPEN_WAGER.replaceAll("%username", username));
                    found = true;
                }
            }

            if (!found) {
                // attempt to parse the amount
                Double amount = Utils.tryParseDbl(msg[1]);
                double betsize = db.checkCredits(username, amount);
                if (amount == null) {
                    bot.sendIRCMessage(channel, INVALID_BET);
                } else if (amount <= 0) {
                    bot.sendIRCMessage(channel, INVALID_BETSIZE);
                } else if (amount > Variables.MAXBET) {
                    bot.maxBet(user, channel, Variables.MAXBET);
                } else if (betsize > 0.0) { // add bet, remove chips,notify
                                            // channel
                    ProfileType profile = db.getActiveProfile(username);
                    Bet bet = new Bet(user, profile, betsize, "");
                    openBets.add(bet);
                    db.adjustChips(
                            username, -betsize, profile, GamesType.DICE_DUEL,
                            TransactionType.BET);
                    db.addBet(
                            username, "", amount, profile, GamesType.DICE_DUEL);

                    String out = NEW_WAGER.replaceAll("%username", username);
                    out = out.replaceAll(
                            "%amount", Utils.chipsToString(betsize));
                    String ptype = "real";
                    if (bet.getProfile() == ProfileType.PLAY) {
                        ptype = "play";
                    }
                    out = out.replaceAll("%proftype", ptype);
                    bot.sendIRCMessage(channel, out);
                } else {
                    bot.sendIRCMessage(channel,
                            NO_CHIPS.replaceAll("%username", username));
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
        String p1 = username; // user who is calling
        String p2 =  null;
        if (msg.length >= 2) {
            p2 = msg[1];
        }

        // check to see if someone is playing themselves...
        if (p1.equalsIgnoreCase(p2)) {
            bot.sendIRCMessage(channel,
                               NO_SELFPLAY.replaceAll("%username", username));
        } else if (p2 != null) {
            Bet found = null;
            boolean foundb = false;
            for (Bet bet : openBets) {
                if (bet.getUser().getNick().equalsIgnoreCase(p2)) {
                    found = bet;
                    foundb = true;
                    ProfileType p1prof = db.getActiveProfile(p1);

                    // quick hax to check if play chips vs non-play chips!
                    if (p1prof != ProfileType.PLAY
                            && bet.getProfile() == ProfileType.PLAY) {
                        bot.sendIRCMessage(
                                channel,
                                PLAY_VS.replaceAll("%username", username));
                        found = null;
                    } else if (p1prof == ProfileType.PLAY
                            && bet.getProfile() != ProfileType.PLAY) {
                        bot.sendIRCMessage(
                                channel,
                                REAL_VS.replaceAll("%username", username));
                        found = null;
                    } else if (db.checkCredits(p1) < bet.getAmount()) {
                        bot.sendIRCMessage(
                                channel,
                                NO_CHIPS.replaceAll("%username", username));
                        found = null;
                    } else {
                        // play this wager
                        // add a transaction for the 2nd player to call
                        db.adjustChips(
                                p1, -bet.getAmount(), p1prof,
                                GamesType.DICE_DUEL, TransactionType.BET);

                        int d1 = 0;
                        int d2 = 0;
                        do {
                            d1 = (Random.nextInt(RANDOM) + 1)
                                    + (Random.nextInt(RANDOM) + 1); // p1
                            d2 = (Random.nextInt(RANDOM) + 1)
                                    + (Random.nextInt(RANDOM) + 1); // p2
                        } while (d1 == d2); // re-roll until winner

                        String winner = "";
                        String loser = "";
                        ProfileType winnerProfile;
                        ProfileType loserProfile;
                        if (d1 > d2) { // p1 wins
                            winner = p1;
                            loser = p2;
                            winnerProfile = p1prof;
                            loserProfile = bet.getProfile();
                        } else { // p2 wins, use his profile
                            winner = p2;
                            loser = p1;
                            loserProfile = p1prof;
                            winnerProfile = bet.getProfile();
                        }

                        double rake = Rake.getRake(
                                winner, bet.getAmount(), winnerProfile)
                                + Rake.getRake(
                                        loser, bet.getAmount(), loserProfile);
                        double win = (bet.getAmount() * 2) - rake;
                        db.adjustChips(
                                winner, win, winnerProfile,
                                GamesType.DICE_DUEL, TransactionType.WIN);

                        db.deleteBet(winner, GamesType.DICE_DUEL);
                        db.deleteBet(loser, GamesType.DICE_DUEL);

                        // jackpot stuff
                        if (Rake.checkJackpot(bet.getAmount())) { // loser
                                                                  // first?
                                                                  // Let's be
                                                                  // nice
                            ArrayList<String> players = new ArrayList<String>();
                            players.add(loser);
                            Rake.jackpotWon(loserProfile, GamesType.DICE_DUEL, 
                                            players, bot, null);
                        } else if (Rake.checkJackpot(bet.getAmount())) {
                            ArrayList<String> players = new ArrayList<String>();
                            players.add(winner);
                            Rake.jackpotWon(winnerProfile, GamesType.DICE_DUEL,
                                            players, bot, null);
                        }
                        
                        int losed = d1;
                        int wind = d1;
                        if (d1 > d2) {
                            losed = d2;
                        } else if (d1 < d2) {
                            wind = d2;
                        }

                        String out = ROLL.replaceAll("%winner", winner);
                        out = out.replaceAll("%loser", loser);
                        out = out.replaceAll(
                                "%amount", Utils.chipsToString(win));
                        out = out.replaceAll("%windice",
                                Integer.toString(wind));
                        out = out.replaceAll("%losedice",
                                Integer.toString(losed));
                        bot.sendIRCMessage(channel, out);
                    }
                    break;
                }
            }

            if (!foundb) {
                // if we reach here the game doesn't exist
                bot.sendIRCMessage(
                        channel, NO_WAGER.replaceAll("%username", username));
            }

            if (found != null) {
                openBets.remove(found);
            }
        }
    }
    
    /**
     * This method handles the cancel command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when the system failed to perform db tasks
     */
    private void cancel(final Message event)
        throws SQLException {
        User user = event.getUser();
        String username = user.getNick();
        Channel channel = event.getChannel();

        DB db = DB.getInstance();
        // try to locate and cancel the bet else ignore
        Bet found = null;
        for (Bet bet : openBets) {
            if (bet.getUser().compareTo(user) == 0) {
                db.adjustChips(
                        username, bet.getAmount(), bet.getProfile(),
                        GamesType.DICE_DUEL, TransactionType.CANCEL);
                found = bet;

                db.deleteBet(username, GamesType.DICE_DUEL);

                event.getBot().sendIRCMessage(channel,
                        BET_CANCELLED.replaceAll("%username", username));
                break;
            }
        }
        if (found != null) {
            openBets.remove(found);
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
                    wagers = wagers.replaceAll("%username",
                                               bet.getUser().getNick());
                    wagers = wagers.replaceAll("%amount",
                                        Utils.chipsToString(bet.getAmount()));
                }
                String line = OPEN_WAGERS.replaceAll("%wagers", wagers);
                
                Channel chan = irc.getUserChannelDao().getChannel(channel);
                irc.sendIRCMessage(chan, line);
            }
        }
    }
}
