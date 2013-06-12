package org.smokinmils.games.casino;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.smokinmils.BaseBot;
import org.smokinmils.bot.Bet;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.DBException;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

public class DiceDuel extends Event {
    private static final String  BET_CMD         = "!dd";
    private static final String  CXL_CMD         = "!ddcancel";
    private static final String  CALL_CMD        = "!call";

    private static final String  INVALID_BET     = "%b%c12\"%c04!dd <amount> %c12\". Error, Invalid Bet";
    private static final String  INVALID_BETSIZE = "%b%c12You have to bet more than %c040%c12!";
    private static final String  BET_CANCELLED   = "%b%c04%username%c12: Cancelled your open wager";
    private static final String  OPEN_WAGER      = "%b%c04%username%c12: You already have a wager open, Type %c04!ddcancel %c12to cancel it";
    private static final String  NO_WAGER        = "%b%c04%username%c12: I can't find a record of that wager";
    private static final String  NO_CHIPS        = "%b%c04%username%c12: You do not have enough chips for that!";
    private static final String  NO_SELFPLAY     = "%b%c04%username%c12: You can't play against yourself!";
    private static final String  NEW_WAGER       = "%b%c04%username%c12 has opened a new dice duel wager of %c04%amount %proftype%c12  chips! To call this wager type %c04!call %username";
    private static final String  PLAY_VS         = "%b%c04%username%c12: you need to use play chips to call a play chips dd!";
    private static final String  REAL_VS         = "%b%c04%username%c12: you need to use real chips to call a real chips dd!";
    private static final String  ROLL            = "%b%c04%winner%c12 rolled %c04%windice%c12, %c04%loser%c12 rolled %c04%losedice%c12. %c04%winner%c12 wins the %c04%amount%c12 chip pot!";
    private static final String  OPEN_WAGERS     = "%b%c12Current open wagers: %wagers. To call a wager type %c04!call <name>";
    private static final String  WAGER           = "%c04%username%c12(%c04%amount %proftype%c12) ";

    private static final int     AnnounceDelay   = 3;
    private final ArrayList<Bet> openBets;

    /**
     * Constructor.
     * 
     * @param channel The channel the game will run on
     */
    public DiceDuel(IrcBot bot, String channel) {
        openBets = new ArrayList<Bet>();

        Timer announce = new Timer(true);
        announce.scheduleAtFixedRate(new Announce(bot, channel),
                AnnounceDelay * 60 * 1000, AnnounceDelay * 60 * 1000);
    }

    /**
     * This method handles the roulette commands
     */
    @Override
    public void message(Message event) {
        String message = event.getMessage();
        String sender = event.getUser().getNick();

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

    private void dd(Message event)
        throws DBException, SQLException {
        String username = event.getUser().getNick();
        DB db = DB.getInstance();
        String[] msg = event.getMessage().split(" ");
        IrcBot bot = event.getBot();
        String channel = event.getChannel().getName();

        if (msg.length < 2) {
            bot.sendIRCMessage(channel, INVALID_BET);
        } else {
            // check if they already have an openbet
            boolean found = false;
            for (Bet bet : openBets) {
                if (bet.getUser().equalsIgnoreCase(username)) {
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
                } else if (betsize > 0.0) { // add bet, remove chips,notify
                                            // channel
                    ProfileType profile = db.getActiveProfile(username);
                    Bet bet = new Bet(username, profile, betsize, "");
                    openBets.add(bet);
                    db.adjustChips(username, -betsize, profile,
                            GamesType.DICE_DUEL, TransactionType.BET);
                    db.addBet(username, "", amount, profile,
                            GamesType.DICE_DUEL);

                    String out = NEW_WAGER.replaceAll("%username", username);
                    out = out.replaceAll("%amount",
                            Utils.chipsToString(betsize));
                    out = out.replaceAll("%proftype",
                            (bet.getProfile() == ProfileType.PLAY ? "play"
                                    : "real"));
                    bot.sendIRCMessage(channel, out);
                } else {
                    bot.sendIRCMessage(channel,
                            NO_CHIPS.replaceAll("%username", username));
                }
            }
        }
    }

    private void call(Message event)
        throws DBException, SQLException {
        String username = event.getUser().getNick();
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String channel = event.getChannel().getName();

        DB db = DB.getInstance();
        String p1 = username; // user who is calling
        String p2 = msg.length < 2 ? null : msg[1];

        // check to see if someone is playing themselves...
        if (p1.equalsIgnoreCase(p2)) {
            bot.sendIRCMessage(channel,
                    NO_SELFPLAY.replaceAll("%username", username));
        } else if (p2 != null) {
            Bet found = null;
            boolean foundb = false;
            for (Bet bet : openBets) {
                if (bet.getUser().equalsIgnoreCase(p2) && bet.isValid()) {
                    found = bet;
                    foundb = true;
                    ProfileType p1prof = db.getActiveProfile(p1);

                    // quick hax to check if play chips vs non-play chips!
                    if (p1prof != ProfileType.PLAY
                            && bet.getProfile() == ProfileType.PLAY) {
                        bot.sendIRCMessage(channel,
                                PLAY_VS.replaceAll("%username", username));
                        found = null;
                    } else if (p1prof == ProfileType.PLAY
                            && bet.getProfile() != ProfileType.PLAY) {
                        bot.sendIRCMessage(channel,
                                REAL_VS.replaceAll("%username", username));
                        found = null;
                    } else if (db.checkCredits(p1) < bet.getAmount()) {
                        bot.sendIRCMessage(channel,
                                NO_CHIPS.replaceAll("%username", username));
                        found = null;
                    } else {
                        // play this wager
                        // add a transaction for the 2nd player to call
                        db.adjustChips(p1, -bet.getAmount(), p1prof,
                                GamesType.DICE_DUEL, TransactionType.BET);

                        int d1 = 0;
                        int d2 = 0;
                        do {
                            d1 = (Random.nextInt(6) + 1)
                                    + (Random.nextInt(6) + 1); // p1
                            d2 = (Random.nextInt(6) + 1)
                                    + (Random.nextInt(6) + 1); // p2
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

                        double rake = Rake.getRake(winner, bet.getAmount(),
                                winnerProfile)
                                + Rake.getRake(loser, bet.getAmount(),
                                        loserProfile);
                        double win = (bet.getAmount() * 2) - rake;
                        db.adjustChips(winner, win, winnerProfile,
                                GamesType.DICE_DUEL, TransactionType.WIN);

                        db.deleteBet(bet.getUser(), GamesType.DICE_DUEL);

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

                        String out = ROLL.replaceAll("%winner", winner);
                        out = out.replaceAll("%loser", loser);
                        out = out.replaceAll("%amount",
                                Utils.chipsToString(win));
                        out = out.replaceAll("%windice",
                                Integer.toString((d1 > d2 ? d1 : d2)));
                        out = out.replaceAll("%losedice",
                                Integer.toString((d1 < d2 ? d1 : d2)));
                        bot.sendIRCMessage(channel, out);
                    }
                    break;
                }
            }

            if (!foundb) {
                // if we reach here the game doesn't exist
                bot.sendIRCMessage(channel,
                        NO_WAGER.replaceAll("%username", username));
            }

            if (found != null) {
                openBets.remove(found);
            }
        }
    }

    private void cancel(Message event)
        throws DBException, SQLException {
        String username = event.getUser().getNick();
        String channel = event.getChannel().getName();

        DB db = DB.getInstance();
        // try to locate and cancel the bet else ignore
        Bet found = null;
        for (Bet bet : openBets) {
            if (bet.getUser().equalsIgnoreCase(username) && bet.isValid()) {
                bet.invalidate();
                db.adjustChips(username, bet.getAmount(), bet.getProfile(),
                        GamesType.DICE_DUEL, TransactionType.CANCEL);
                found = bet;

                db.deleteBet(bet.getUser(), GamesType.DICE_DUEL);

                event.getBot().sendIRCMessage(channel,
                        BET_CANCELLED.replaceAll("%username", username));
                break;
            }
        }
        if (found != null) openBets.remove(found);
    }

    /**
     * Simple extension to time task to deal with game triggers
     * 
     * @author cjc
     */
    class Announce extends TimerTask {
        private final IrcBot bot;
        private final String channel;

        public Announce(IrcBot bot, String channel) {
            this.bot = bot;
            this.channel = channel;
        }

        @Override
        public void run() {
            if (openBets.size() > 0) {
                String wagers = "";
                for (Bet bet : openBets) {
                    wagers += WAGER.replaceAll("%proftype",
                            (bet.getProfile() == ProfileType.PLAY) ? "play"
                                    : "real");
                    wagers = wagers.replaceAll("%username", bet.getUser());
                    wagers = wagers.replaceAll("%amount",
                            Utils.chipsToString(bet.getAmount()));
                }
                String line = OPEN_WAGERS.replaceAll("%wagers", wagers);

                bot.sendIRCMessage(channel, line);
            }
        }
    }
}
