package org.smokinmils.games.casino;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

public class Roulette extends Event {
    private static final int     NoMoreBetsDelay = 10;

    private static final String  BET_CMD         = "!bet";
    private static final String  CXL_CMD         = "!cancel";
    private static final String  END_CMD         = "!end";

    private static final String  SPINNING        = "%b%c12Bets closed! No more bets. Spinning...";
    private static final String  BETS_CLOSED     = "%b%c12Bets are now closed, please wait for the next round!";
    private static final String  NO_CHIPS        = "%b%c12You do not have enough chips for that!";
    private static final String  INVALID_BET     = "%b%c12\"%c04!bet <amount> <choice>%c12\" You have entered an invalid choice. Please enter %c04black%c12, %c04red%c12, %c041-36%c12, %c041st%c12, %c042nd%c12, %c043rd%c12, %c04even%c12 or %c04odd%c12 as your choice.";
    private static final String  INVALID_BETSIZE = "%b%c12You have to bet more than %c040%c12!";
    private static final String  BET_MADE        = "%b%c04%username%c12: You have bet %c04%amount%c12 on %c04%choice";
    private static final String  BETS_CANCELLED  = "%b%c12All bets cancelled for %c04%username";
    private static final String  CANT_END        = "%b%c12You don't have the required permissions for that";
    private static final String  NEW_GAME        = "%b%c12A new roulette game is starting! Type %c04!info %c12for instructions on how to play.";
    private static final String  PLACE_BETS      = "%b%c12Place your bets now!";
    private static final String  WIN_LINE        = "%b%c12The winning number is: %c04%number %c12%colour %board%n";
    private static final String  CONGRATULATIONS = "%b%c12Congratulations to %c04%names";

    ArrayList<String>            validCommands;
    Timer                        gameTimer;

    private final ArrayList<Bet> allBets;

    private final IrcBot         bot;
    private final int            delay;
    private final String         channel;

    public final int             OPEN            = 0;
    public final int             CLOSE           = 1;
    public int                   state;

    public Roulette(int delay, String channel, IrcBot bot) {
        // TODO add in stuff for ranks, and loading stuff from file :)
        // instantiate the bet lists
        allBets = new ArrayList<Bet>();
        this.channel = channel;
        this.state = OPEN;
        this.delay = delay;
        this.bot = bot;

        gameTimer = new Timer(true);
        gameTimer.schedule(new AnnounceEnd(bot, channel), delay * 60 * 1000);
    }

    /**
     * This method handles the roulette commands
     */
    @Override
    public void message(Message event) {
        String message = event.getMessage();
        String sender = event.getUser().getNick();

        synchronized (BaseBot.getLockObject()) {
            if (channel.equalsIgnoreCase(event.getChannel().getName())
                    && bot.userIsIdentified(sender)) {
                try {
                    if (Utils.startsWith(message, BET_CMD)) {
                        bet(event);
                    } else if (Utils.startsWith(message, CXL_CMD)) {
                        cancel(event);
                    } else if (Utils.startsWith(message, END_CMD)) {
                        end(event);
                    }
                } catch (Exception e) {
                    EventLog.log(e, "Roulette", "message");
                }
            }
        }
    }

    private void bet(Message event)
        throws DBException, SQLException {
        // if we are not accepting bets
        DB db = DB.getInstance();
        String username = event.getUser().getNick();
        String[] msg = event.getMessage().split(" ");

        if (state == CLOSE) {
            bot.sendIRCMessage(channel, BETS_CLOSED);
        } else if (msg.length < 3) {
            bot.sendIRCNotice(username, INVALID_BET);
        } else {
            Double amount = Utils.tryParseDbl(msg[1]);
            double betsize = db.checkCredits(username, amount);
            String choice = msg[2].toLowerCase();
            Integer choicenum = Utils.tryParse(msg[2]);
            ProfileType profile = db.getActiveProfile(username);
            if (amount == null) {
                bot.sendIRCNotice(username, INVALID_BET);
            } else if (amount <= 0) {
                bot.sendIRCNotice(username, INVALID_BETSIZE);
            } else if (!((choice.equalsIgnoreCase("red")
                    || choice.equalsIgnoreCase("black")
                    || choice.equalsIgnoreCase("1st")
                    || choice.equalsIgnoreCase("2nd")
                    || choice.equalsIgnoreCase("3rd")
                    || choice.equalsIgnoreCase("even") || choice
                        .equalsIgnoreCase("odd")) || (choicenum != null
                    && choicenum >= 0 && choicenum <= 36))) {
                bot.sendIRCNotice(username, INVALID_BET);
            } else if (betsize <= 0.0) {
                bot.sendIRCNotice(username, NO_CHIPS);
            } else {
                Bet bet = new Bet(username, profile, betsize, choice);
                allBets.add(bet);
                db.adjustChips(username, -betsize, profile, GamesType.ROULETTE,
                        TransactionType.BET);
                db.addBet(username, choice, betsize, profile,
                        GamesType.ROULETTE);

                String out = BET_MADE.replaceAll("%username", username);
                out = out.replaceAll("%choice", choice);
                out = out.replaceAll("%amount", Utils.chipsToString(betsize));
                bot.sendIRCMessage(channel, out);
            }
        }
    }

    private void cancel(Message event)
        throws DBException, SQLException {
        DB db = DB.getInstance();
        boolean found = false;

        String username = event.getUser().getNick();

        for (Bet bet : allBets) {
            if (bet.isValid() && bet.getUser().equalsIgnoreCase(username)) {
                bet.invalidate();
                found = true;
                db.adjustChips(username, bet.getAmount(), bet.getProfile(),
                        GamesType.ROULETTE, TransactionType.CANCEL);;
            }
        }

        if (found) {
            db.deleteBet(username, GamesType.ROULETTE);
        }

        bot.sendIRCMessage(channel,
                BETS_CANCELLED.replaceAll("%username", username));
    }

    private void end(Message event)
        throws DBException, SQLException {
        if (bot.userIsHost(event.getUser(), channel)) {
            endGame(bot);
        } else {
            bot.sendIRCNotice(event.getUser().getNick(), CANT_END);
        }
    }

    //  for setting colours
    private void printBoard() {
        bot.sendIRCMessage(
                channel,
                "0,3 00 0,1 28 0,4 09 0,1 26 0,4 30 0,1 11 0,4 07 0,1 20 0,4 32 0,1 17 0,4 05 0,1 22 0,4 34 0,1");
        bot.sendIRCMessage(
                channel,
                "0,3 00 0,4 15 0,1 03 0,4 24 0,1 36 0,4 13 0,1 01 0,4 27 0,1 10 0,4 25 0,1 29 0,4 12 0,1 08 0,1");
        bot.sendIRCMessage(
                channel,
                "0,3 00 0,1 19 0,4 31 0,1 18 0,4 06 0,1 21 0,4 33 0,1 16 0,4 04 0,1 23 0,4 35 0,1 14 0,4 02 0,1");
    }

    private String getColour(int number) {
        String ret = "red";
        if (number == 0) {
            ret = "green";
        } else if (number == 28 | number == 26 | number == 11 | number == 20
                | number == 17 | number == 22 | number == 3 | number == 36
                | number == 1 | number == 10 | number == 29 | number == 8
                | number == 19 | number == 18 | number == 21 | number == 16
                | number == 23 | number == 14) {
            ret = "black";
        }
        return ret;
    }

    private String getRow(int number) {
        String ret = "3rd";
        if (number == 28 | number == 9 | number == 26 | number == 30
                | number == 11 | number == 7 | number == 20 | number == 32
                | number == 17 | number == 5 | number == 22 | number == 34) {
            ret = "1st";
        } else if (number == 15 | number == 3 | number == 24 | number == 36
                | number == 13 | number == 1 | number == 27 | number == 10
                | number == 25 | number == 29 | number == 12 | number == 8) {
            ret = "2nd";
        }
        return ret;
    }

    /**
     * Ends the game and prints out winners (move to arraylist as per usual)
     * 
     * @return winners / win info
     * @throws SQLException
     * @throws DBException
     */
    private void endGame(IrcBot bot)
        throws DBException, SQLException {
        DB db = DB.getInstance();
        // Let's "roll"
        int winner = Random.nextInt(39);
        if (winner > 36) winner = 0;
        boolean isEven = ((winner % 2 == 0 && winner != 0) ? true : false);

        List<String> nameList = new ArrayList<String>();
        Map<ProfileType, Integer> profbets = new HashMap<ProfileType, Integer>();
        Map<ProfileType, List<String>> profusers = new HashMap<ProfileType, List<String>>();
        List<ProfileType> profiles = new ArrayList<ProfileType>();

        for (Bet bet : allBets) {
            String choice = bet.getChoice();
            Integer choicenum = Utils.tryParse(choice);
            String user = bet.getUser();
            ProfileType profile = bet.getProfile();

            if (bet.isValid()) {
                int winamount = 0;
                boolean win = false;
                if ((choice.equalsIgnoreCase("red") || choice
                        .equalsIgnoreCase("black"))
                        && choice.equalsIgnoreCase(getColour(winner))
                        && winner != 0) {
                    winamount = 2;
                    win = true;
                } else if (((choice.equalsIgnoreCase("even") && isEven) || (choice
                        .equalsIgnoreCase("odd") && !isEven)) && winner != 0) {
                    win = true;
                    winamount = 2;
                } else if (winner != 0
                        && (choice.equalsIgnoreCase("1st")
                                || choice.equalsIgnoreCase("2nd") || choice
                                    .equalsIgnoreCase("3rd"))
                        && choice.equalsIgnoreCase(getRow(winner))) {
                    win = true;
                    winamount = 3;
                } else if (choicenum != null && winner == choicenum) {
                    win = true;
                    winamount = ((winner == 0) ? 12 : 36);
                }

                if (win) {
                    if (!nameList.contains(user)) nameList.add(user);

                    db.adjustChips(user, bet.getAmount() * winamount, profile,
                            GamesType.ROULETTE, TransactionType.WIN);
                }
                Rake.getRake(user, bet.getAmount(), profile);

                double amount = bet.getAmount();
                if (Rake.checkJackpot(amount)) {
                    ArrayList<String> players = new ArrayList<String>();
                    players.add(user);
                    Rake.jackpotWon(profile, GamesType.ROULETTE, players, bot,
                            null);
                }
            }
            db.deleteBet(bet.getUser(), GamesType.ROULETTE);
        }

        // check if they win the jackpot
        for (ProfileType profile : profiles) {
            Integer amount = profbets.get(profile);
            List<String> users = profusers.get(profile);
            if (amount != null && amount != 0 && users.size() > 0) {
                // Check if jackpot won
            }
        }

        // Construct the winning string
        String win_line = WIN_LINE.replaceAll("%colour", getColour(winner));
        win_line = win_line.replaceAll("%number", Integer.toString(winner));

        String board = "";
        if (getColour(winner).equalsIgnoreCase("red")) board = "%c00,04 ";
        else if (getColour(winner).equalsIgnoreCase("black")) board = "%c00,01 ";
        else
            board = "%c00,03 ";
        board += winner + " ";

        win_line = win_line.replaceAll("%board", board);
        bot.sendIRCMessage(channel, win_line);

        // announce winners
        if (nameList.size() > 0) {
            String names = "";
            for (String user : nameList) {
                names += user + " ";
            }
            bot.sendIRCMessage(channel,
                    CONGRATULATIONS.replaceAll("%names", names));
        }

        bot.sendIRCMessage(channel, NEW_GAME);
        printBoard();
        bot.sendIRCMessage(channel, PLACE_BETS);

        // clear the bets
        allBets.clear();

        this.state = OPEN;
    }

    class AnnounceEnd extends TimerTask {
        private final IrcBot bot;
        private final String channel;

        public AnnounceEnd(IrcBot bot, String channel) {
            this.bot = bot;
            this.channel = channel;
        }

        @Override
        public void run() {
            state = CLOSE;
            bot.sendIRCMessage(channel, SPINNING);
            gameTimer.schedule(new End(bot, channel), NoMoreBetsDelay * 1000);
        }
    }

    class End extends TimerTask {
        private final IrcBot bot;
        private final String channel;

        public End(IrcBot bot, String channel) {
            this.bot = bot;
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                endGame(bot);
            } catch (Exception e) {
                EventLog.log(e, "Roulette", "timerTask");
            }
            gameTimer
                    .schedule(new AnnounceEnd(bot, channel), delay * 60 * 1000);
        }
    }
}
