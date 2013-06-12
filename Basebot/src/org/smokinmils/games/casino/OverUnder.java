package org.smokinmils.games.casino;

import java.sql.SQLException;
import java.util.ArrayList;

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

public class OverUnder extends Event {
    private static final String  BET_CMD         = "!ou";
    private static final String  CXL_CMD         = "!oucancel";
    private static final String  ROLL_CMD        = "!ouroll";

    private static final String  INVALID_BET     = "%b%c12\"%c04!ou <amount> <choice>%c12\". You have entered an invalid choice. Please use %c04over%c12, %c04under%c12 or %c047";
    private static final String  INVALID_BETSIZE = "%b%c12You have to bet more than %c040%c12!";
    private static final String  OPEN_WAGER      = "%b%c04%username%c12: You already have a wager open. Type %c04!ouroll %c12to roll! Type %c04!oucancel %c12to cancel it";
    private static final String  NO_CHIPS        = "%b%c04%username%c12: You do not have enough chips for that!";
    private static final String  NO_WAGER        = "%b%c04%username%c12: I can't find a record of that wager";
    private static final String  BET_CANCELLED   = "%b%c04%username%c12: Cancelled your open OverUnder wager";
    private static final String  NEW_WAGER       = "%b%c04%username%c12 has bet %c04%amount%c12 on %c04%choice%c12. Type %c04!ouroll %c12to roll!";
    private static final String  ROLL_WIN        = "%b%c12%bonusRolling... %c04%total%c12. Congratulations on your win %c04%username%c12!";
    private static final String  ROLL_LOSE       = "%b%c12%bonusRolling... %c04%total%c12. Better luck next time %c04%username%c12!";
    private static final String  BONUS_ROLL      = "BONUS ROLL! ";

    private final ArrayList<Bet> openBets;

    public OverUnder() {
        openBets = new ArrayList<Bet>();
    }

    /**
     * This method handles the roulette commands.
     */
    @Override
    public final void message(Message event) {
        String message = event.getMessage();
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();

        synchronized (BaseBot.getLockObject()) {
            if (isValidChannel(channel)
                    && event.getBot().userIsIdentified(sender)) {
                try {
                    if (Utils.startsWith(message, ROLL_CMD)) {
                        roll(event);
                    } else if (Utils.startsWith(message, CXL_CMD)) {
                        cancel(event);
                    } else if (Utils.startsWith(message, BET_CMD)) {
                        ou(event);
                    }
                } catch (Exception e) {
                    EventLog.log(e, "OverUnder", "message");
                }
            }
        }
    }

    private void ou(Message event)
        throws DBException, SQLException {
        // make sure they don't have an open bet otherwise let them know to
        // roll or cancel.
        DB db = DB.getInstance();
        String[] msg = event.getMessage().split(" ");
        IrcBot bot = event.getBot();
        String username = event.getUser().getNick();
        String channel = event.getChannel().getName();

        if (msg.length < 3) {
            bot.sendIRCMessage(channel, INVALID_BET);
        } else {
            boolean found = false;
            for (Bet bet : openBets) {
                if (bet.getUser().equalsIgnoreCase(username)) {
                    // They already have a bet open, and as such, tell them to
                    // roll instead
                    found = true;
                    break;
                }
            }

            Double amount = Utils.tryParseDbl(msg[1]);
            String choice = msg[2];
            double betsize = db.checkCredits(username, amount);
            if (found) {
                bot.sendIRCMessage(channel,
                        OPEN_WAGER.replaceAll("%username", username));
            } else if (amount == null) {
                bot.sendIRCMessage(channel, INVALID_BET);
            } else if (amount <= 0) {
                bot.sendIRCMessage(channel, INVALID_BETSIZE);
            } else if (!choice.equalsIgnoreCase("over")
                    && !choice.equalsIgnoreCase("under")
                    && !choice.equalsIgnoreCase("7")) {
                bot.sendIRCMessage(channel, INVALID_BET);
            } else if (betsize <= 0.0) {
                bot.sendIRCMessage(channel,
                        NO_CHIPS.replaceAll("%username", username));
            } else {
                ProfileType prof = db.getActiveProfile(username);
                Bet bet = new Bet(username, prof, betsize, choice);
                openBets.add(bet);
                db.adjustChips(username, -betsize, prof, GamesType.OVER_UNDER,
                        TransactionType.BET);
                db.addBet(username, choice, betsize, prof, GamesType.OVER_UNDER);

                String out = NEW_WAGER.replaceAll("%username", username);
                out = out.replaceAll("%choice", choice);
                out = out.replaceAll("%amount", Utils.chipsToString(betsize));
                bot.sendIRCMessage(channel, out);
            }
        }
    }

    private void roll(Message event)
        throws DBException, SQLException {
        DB db = DB.getInstance();

        String username = event.getUser().getNick();
        IrcBot bot = event.getBot();
        String channel = event.getChannel().getName();

        boolean found = false;
        Bet foundbet = null;
        for (Bet bet : openBets) {
            if (bet.isValid() && bet.getUser().equalsIgnoreCase(username)) {
                found = true;
                foundbet = bet;
                // generate some die rolls
                int total = (Random.nextInt(6) + 1) + (Random.nextInt(6) + 1);

                if (doesBetWin(bet, total)) {
                    double winnings = 2 * bet.getAmount();
                    if (bet.getChoice().equalsIgnoreCase("7")) {
                        winnings = 5 * bet.getAmount();
                    }

                    // they win pay out and add string
                    db.adjustChips(username, winnings, bet.getProfile(),
                            GamesType.OVER_UNDER, TransactionType.WIN);

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
                    double r = 0.75;
                    if (bet.getChoice().equalsIgnoreCase("7")) {
                        r = 0.84;
                    }

                    if (Math.random() > r) {
                        total = (Random.nextInt(6) + 1)
                                + (Random.nextInt(6) + 1);
                        if (doesBetWin(bet, total)) {
                            double winnings = 2 * bet.getAmount();
                            if (bet.getChoice().equalsIgnoreCase("7")) {
                                winnings = 5 * bet.getAmount();
                            }

                            // they win pay out and add string
                            db.adjustChips(username, winnings,
                                    bet.getProfile(), GamesType.OVER_UNDER,
                                    TransactionType.WIN);

                            out = ROLL_WIN.replaceAll("%bonus", BONUS_ROLL);
                            out = out.replaceAll("%username", username);
                            out = out.replaceAll("%total",
                                    Integer.toString(total));
                            bot.sendIRCMessage(channel, out);
                        } else {
                            out = ROLL_LOSE.replaceAll("%bonus", BONUS_ROLL);
                            out = out.replaceAll("%username", username);
                            out = out.replaceAll("%total",
                                    Integer.toString(total));
                            bot.sendIRCMessage(channel, out);
                        }
                    }
                }

                // remove the bet
                bet.invalidate();
                db.deleteBet(bet.getUser(), GamesType.OVER_UNDER);

                // Generate "rake"
                Rake.getRake(bet.getUser(), bet.getAmount(), bet.getProfile());

                // check if jackpot won
                if (Rake.checkJackpot(bet.getAmount())) {
                    ArrayList<String> winners = new ArrayList<String>();
                    winners.add(bet.getUser());
                    Rake.jackpotWon(bet.getProfile(), GamesType.OVER_UNDER,
                            winners, bot, null);
                }
                break;
            }
        }

        if (!found) {
            bot.sendIRCMessage(channel,
                    NO_WAGER.replaceAll("%username", username));
        }

        if (foundbet != null) {
            openBets.remove(foundbet);
        }
    }

    private void cancel(Message event)
        throws DBException, SQLException {
        DB db = DB.getInstance();
        String username = event.getUser().getNick();
        String channel = event.getChannel().getName();
        Bet found = null;
        for (Bet bet : openBets) {
            if (bet.isValid() && bet.getUser().equalsIgnoreCase(username)) {
                bet.invalidate();
                found = bet;
                db.deleteBet(bet.getUser(), GamesType.OVER_UNDER);
                db.adjustChips(bet.getUser(), bet.getAmount(),
                        bet.getProfile(), GamesType.OVER_UNDER,
                        TransactionType.CANCEL);

                event.getBot().sendIRCMessage(channel,
                        BET_CANCELLED.replaceAll("%username", username));
            }
        }
        if (found != null) openBets.remove(found);
    }

    private boolean doesBetWin(Bet bet,
                               int total) {
        boolean ret = false;
        if (bet.getChoice().equalsIgnoreCase("7") && total == 7) {
            ret = true;
        } else if (bet.getChoice().equalsIgnoreCase("under") && total < 7) {
            ret = true;
        } else if (bet.getChoice().equalsIgnoreCase("over") && total > 7) {
            ret = true;
        }
        return ret;
    }
}
