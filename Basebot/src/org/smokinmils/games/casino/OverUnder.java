package org.smokinmils.games.casino;

import java.sql.SQLException;
import java.util.ArrayList;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.BaseBot;
import org.smokinmils.bot.Bet;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;
import org.smokinmils.bot.SpamEnforcer;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.Variables;

/**
 * Class the provide a overunder game.
 * 
 * @author Jamie
 */
public class OverUnder extends Event {
    /** The bet command. */
    private static final String  BET_CMD         = "!ou";
    
    /** The cancel command. */    
    private static final String  CXL_CMD         = "!oucancel";
    
    /** The roll command. */
    private static final String  ROLL_CMD        = "!ouroll";
    
    /** The roll command length. */
    private static final int     ROLL_CMD_LEN    = 3;

    /** Invalid bet choice message. */
    private static final String  INVALID_BET     = "%b%c12\"%c04!ou <amount> "
            + " <choice>%c12\". You have entered an invalid choice. Please use "
            + "%c04over%c12, %c04under%c12 or %c047";

    /** Invalid bet size message. */
    private static final String  INVALID_BETSIZE = "%b%c12You have to bet more "
                                                 + "than %c040%c12!";

    /** Open bet exists message. */
    private static final String  OPEN_WAGER      = "%b%c04%username%c12: You "
            + "already have a wager open. Type %c04!ouroll %c12to roll! Type "
            + "%c04!oucancel %c12to cancel it";

    /** Not enough chips message. */
    private static final String  NO_CHIPS        = "%b%c04%username%c12: You do"
                                           + " not have enough coins for that!";

    /** Invalid bet choice message. */
    private static final String  NO_WAGER        = "%b%c04%username%c12: I "
                                          + "can't find a record of that wager";

    /** Bet cancelled message. */
    private static final String  BET_CANCELLED   = "%b%c04%username%c12: "
                                        + "Cancelled your open OverUnder wager";

    /** New bet made message. */
    private static final String  NEW_WAGER       = "%b%c04%username%c12 " 
               + "has bet %c04%amount%c12 on %c04%choice%c12. Type %c04!ouroll "
               + "%c12to roll!";

    /** Win message. */
    private static final String  ROLL_WIN        = "%b%c12%bonusRolling... "
             + "%c04%total%c12. Congratulations on your win %c04%username%c12!";

    /** Lose message. */
    private static final String  ROLL_LOSE       = "%b%c12%bonusRolling... "
                   + "%c04%total%c12. Better luck next time %c04%username%c12!";

    /** Bonus roll string. */
    private static final String  BONUS_ROLL      = "BONUS ROLL! ";
    
    /** Middle number. */
    private static final int    NUMBER = 7;
    
    /** Standard odds. */
    private static final int    ODDS_EVEN = 2;
    
    /** Better odds. */
    private static final int    ODDS_NUMB = 5;

    /** Random number. */
    private static final int    RANDOM = 6;
    
    /** Bonus chance. */
    private static final double BONUS_EVEN = 0.76;
    
    /** Bonus chance. */
    private static final double BONUS_NUMB = 0.86;

    /** All open bets for OU. */
    private final ArrayList<Bet> openBets;

    /**
     * Constructor.
     */
    public OverUnder() {
        openBets = new ArrayList<Bet>();
    }

    /**
     * This method handles the over under commands.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        String message = event.getMessage();
        User sender = event.getUser();
        String channel = event.getChannel().getName();
        SpamEnforcer se = SpamEnforcer.getInstance();

        synchronized (BaseBot.getLockObject()) {
            if (isValidChannel(channel)
                    && event.getBot().userIsIdentified(sender)) { // TODO Move to string
                try {
                    if (Utils.startsWith(message, ROLL_CMD)) {
                        if (se.check(event, "#SM_overunder")) { roll(event); }
                    } else if (Utils.startsWith(message, CXL_CMD)) {
                        if (se.check(event, "#SM_overunder")) { cancel(event); }
                    } else if (Utils.startsWith(message, BET_CMD)) {
                        if (se.check(event, "#SM_overunder")) { ou(event); }
                    }
                } catch (Exception e) {
                    EventLog.log(e, "OverUnder", "message");
                }
            }
        }
    }
    
    /**
     * This method handles the ou command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when the system failed to perform db tasks
     */
    private void ou(final Message event)
        throws SQLException {
        // make sure they don't have an open bet otherwise let them know to
        // roll or cancel.
        DB db = DB.getInstance();
        String[] msg = event.getMessage().split(" ");
        IrcBot bot = event.getBot();
        User user = event.getUser();
        String username = user.getNick();
        Channel channel = event.getChannel();

        if (msg.length < ROLL_CMD_LEN) {
            bot.sendIRCMessage(channel, INVALID_BET);
        } else {
            boolean found = false;
            for (Bet bet : openBets) {
                if (bet.getUser().compareTo(user) == 0) {
                    // They already have a bet open, and as such, tell them to
                    // roll instead
                    found = true;
                    break;
                }
            }

            Double amount = Utils.tryParseDbl(msg[1]);
            String choice = msg[2];
            double betsize = Utils.checkCredits(user, amount, bot, channel);
            if (found) {
                bot.sendIRCMessage(
                        channel, OPEN_WAGER.replaceAll("%username", username));
            } else if (amount == null) {
                bot.sendIRCMessage(channel, INVALID_BET);
            } else if (amount <= 0) {
                bot.sendIRCMessage(channel, INVALID_BETSIZE);
            } else if (!choice.equalsIgnoreCase("over")
                    && !choice.equalsIgnoreCase("under")
                    && !choice.equalsIgnoreCase("7")) {
                bot.sendIRCMessage(channel, INVALID_BET);
            } else if (choice.equalsIgnoreCase("7")
                    && amount > Variables.MAXBET_OU_7) {
                bot.maxBet(user, channel, Variables.MAXBET_OU_7);
            } else if (!choice.equalsIgnoreCase("7")
                    && amount > Variables.MAXBET) {
                bot.maxBet(user, channel, Variables.MAXBET);
            } else if (betsize <= 0.0) {
                bot.sendIRCMessage(
                        channel, NO_CHIPS.replaceAll("%username", username));
            } else {
                ProfileType prof = db.getActiveProfile(username);
                Bet bet = new Bet(user, prof, GamesType.OVER_UNDER, betsize, choice);
                openBets.add(bet);

                String out = NEW_WAGER.replaceAll("%username", username);
                out = out.replaceAll("%choice", choice);
                out = out.replaceAll("%amount", Utils.chipsToString(betsize));
                bot.sendIRCMessage(channel, out);
            }
        }
    }
    
    /**
     * This method handles the roll command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when the system failed to perform db tasks
     */
    private void roll(final Message event) throws SQLException {
        User user = event.getUser();
        String username = user.getNick();
        IrcBot bot = event.getBot();
        Channel channel = event.getChannel();

        Bet found = null;
        for (Bet bet : openBets) {
            if (bet.getUser().compareTo(user) == 0) {
                found = bet;
                // generate some die rolls
                int total = (Random.nextInt(RANDOM) + 1) + (Random.nextInt(RANDOM) + 1);

                if (doesBetWin(bet, total)) {
                    double winnings = ODDS_EVEN * bet.getAmount();
                    if (bet.getChoice().equalsIgnoreCase("7")) {
                        winnings = ODDS_NUMB * bet.getAmount();
                    }

                    // they win pay out and add string
                    bet.win(winnings);

                    String out = ROLL_WIN.replaceAll("%bonus", "");
                    out = out.replaceAll("%username", username);
                    out = out.replaceAll("%total", Integer.toString(total));
                    bot.sendIRCMessage(channel, out);
                } else {
                    // didn't win,
                    String out = ROLL_LOSE.replaceAll("%bonus", "");
                    out = out.replaceAll("%username", username);
                    out = out.replaceAll("%total", Integer.toString(total));
                    bot.sendIRCMessage(channel, out);

                    // bonus roll ONLY ON LOSSES
                    double r = BONUS_EVEN;
                    if (bet.getChoice().equalsIgnoreCase("7")) {
                        r = BONUS_NUMB;
                    }

                    if (Math.random() > r) {
                        total = (Random.nextInt(RANDOM) + 1) + (Random.nextInt(RANDOM) + 1);
                        if (doesBetWin(bet, total)) {
                            double winnings = ODDS_EVEN * bet.getAmount();
                            if (bet.getChoice().equalsIgnoreCase("7")) {
                                winnings = ODDS_NUMB * bet.getAmount();
                            }

                            // they win pay out and add string
                            bet.win(winnings);

                            out = ROLL_WIN.replaceAll("%bonus", BONUS_ROLL);
                            out = out.replaceAll("%username", username);
                            out = out.replaceAll(
                                    "%total", Integer.toString(total));
                            bot.sendIRCMessage(channel, out);
                        } else {
                            out = ROLL_LOSE.replaceAll("%bonus", BONUS_ROLL);
                            out = out.replaceAll("%username", username);
                            out = out.replaceAll(
                                    "%total", Integer.toString(total));
                            bot.sendIRCMessage(channel, out);
                        }
                    }
                }

                // Generate "rake"
                bet.getRake();

                // check if jackpot won
                if (Rake.checkJackpot(bet.getAmount())) {
                    ArrayList<String> winners = new ArrayList<String>();
                    winners.add(username);
                    Rake.jackpotWon(
                            bet.getProfile(), GamesType.OVER_UNDER, winners,
                            bot, null);
                }
                break;
            }
        }

        if (found == null) {
            bot.sendIRCMessage(channel, NO_WAGER.replaceAll("%username", username));
        } else {
            found.close();
            openBets.remove(found);
        }
    }
    
    /**
     * This method handles the cancel command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when the bet couldn't be cancelled.
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
     * Checks if a bet wins.
     * 
     * @param bet   the Bet object
     * @param total the total dice thrown.
     * 
     * @return true if the bet wins.
     */
    private boolean doesBetWin(final Bet bet,
                               final int total) {
        boolean ret = false;
        if (bet.getChoice().equalsIgnoreCase("7") && total == NUMBER) {
            ret = true;
        } else if (bet.getChoice().equalsIgnoreCase("under")
                && total < NUMBER) {
            ret = true;
        } else if (bet.getChoice().equalsIgnoreCase("over")
                && total > NUMBER) {
            ret = true;
        }
        return ret;
    }
}
