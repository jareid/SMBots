package org.smokinmils.games.casino;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.Variables;

/**
 * Class the provide a roulette game.
 * 
 * @author Jamie
 */
public class Roulette extends Event {
    /** The delay for no more bets. */
    private static final int     NOBETSDELAYSECS = 10;

    /** The bet command. */
    private static final String  BET_CMD         = "!bet";
    
    /** The bet command. */
    private static final int     BET_CMD_LEN     = 3;

    /** The cancel command. */
    private static final String  CXL_CMD         = "!cancel";

    /** The board spinning message. */
    private static final String  SPINNING        = "%b%c12Bets closed! No more"
                                                 + " bets. Spinning...";
    
    /** The bets closed message. */
    private static final String  BETS_CLOSED     = "%b%c12Bets are now closed, "
                                            + "please wait for the next round!";
    
    /** The no chips message. */
    private static final String  NO_CHIPS        = "%b%c12You do not have "
                                                 + "enough coins for that!";
    
    /** The invalid bet choice message. */
    private static final String  INVALID_BET     = "%b%c12\"%c04!bet <amount> "
          + "<choice>%c12\" You have entered an invalid choice. Please enter "
          + "%c04black%c12, %c04red%c12, %c041-36%c12, %c041st%c12, %c042nd%c12"
          + ", %c043rd%c12, %c04even%c12 or %c04odd%c12 as your choice.";
    
    /** The invalid bet size message. */
    private static final String  INVALID_BETSIZE = "%b%c12You have to bet more"
                                                 + "than %c040%c12!";
    
    /** The successful bet message. */
    private static final String  BET_MADE        = "%b%c04%username%c12: You "
                                    + "have bet %c04%amount%c12 on %c04%choice";
    
    /** The message when someone cancels their bets. */
    private static final String  BETS_CANCELLED  = "%b%c12All bets cancelled "
                                                 + "for %c04%username";

    /** The message for a new round starting. */
    private static final String  NEW_GAME        = "%b%c12A new roulette game "
           + "is starting! Type %c04!info roulette%c12for instructions on how to play.";
    
    /** The invalid bet choice message. */
    private static final String  PLACE_BETS      = "%b%c12Place your bets now!";
    
    /** The message displaying the winning number. */
    private static final String  WIN_LINE        = "%b%c12The winning number "
                                       + "is: %board%n";
    
    /** The winners' message. */
    private static final String  CONGRATULATIONS = "%b%c12Congratulations to"
                                                 + " %c04%names";

    /** OPEN state - bets are allowed. */
    private static final int     OPEN            = 0;
    
    /** CLOSED state - bets are not allowed. */
    private static  final int    CLOSE           = 1;
    
    /** Highest number. */
    private static final int NUMBER             = 36;
    
    /** Random generated to. */
    private static final int RANDGENNUM         = 39;
    
    /** The odds for evens bets. */
    private static final int ODDS_EVENS          = 2;
    
    /** The odds for row bets. */
    private static final int ODDS_ROW            = 3;
     
    /** The odds for number bets. */
    private static final int ODDS_NUMBER         = 36;
    
   /** The odds for number bets. */
   private static final int ODDS_ZERO            = 12;
   
   // TODO: use these to generate output.
   /** 1st row. */
   private static final List<Integer> ROWONE  = Arrays.asList(28, 9, 26, 30, 11,
                                                     7, 20, 32, 17, 5, 22, 34);
   
   /** Board output line one. */
   private static final String BOARDL1 = "%c0,3 00 %c0,1 28 %c0,4 09 %c0,1 26 "
                                       + "%c0,4 30 %c0,1 11 %c0,4 07 %c0,1 20 "
                                       + "%c0,4 32 %c0,1 17 %c0,4 05 %c0,1 22 "
                                       + "%c0,4 34 %c0,1";
   
   /** 2nd row. */
   private static final List<Integer> ROWTWO = Arrays.asList(15, 3, 24, 36, 13,
                                                     1, 27, 10, 25, 29, 12, 8);
   
   /** Board output line two. */
   private static final String BOARDL2 = "%c0,3 00 %c0,4 15 %c0,1 03 %c0,4 24 "
                                       + "%c0,1 36 %c0,4 13 %c0,1 01 %c0,4 27 "
                                       + "%c0,1 10 %c0,4 25 %c0,1 29 %c0,4 12 "
                                       + "%c0,1 08 %c0,1";
   
   /** 3rd row. */
   private static final List<Integer> ROWTHREE =  Arrays.asList(19, 31, 18, 6,
                                                 21, 33, 16, 4, 23, 35, 14, 2);
   
   /** Board output line three. */
   private static final String BOARDL3 = "%c0,3 00 %c0,1 19 %c0,4 31 %c0,1 18 "
                                       + "%c0,4 06 %c0,1 21 %c0,4 33 %c0,1 16 "
                                       + "%c0,4 04 %c0,1 23 %c0,4 35 %c0,1 14 "
                                       + "%c0,4 02 %c0,1";

   /** Black numbers. */
   private static final List<Integer> BLACK =  Arrays.asList(1, 3, 8, 10, 11, 
                                     14, 16, 17, 18, 19, 20, 21, 22, 23, 26, 28,
                                       29, 36);
   
   /** Red numbers. */
   private static final List<Integer> RED   =  Arrays.asList(2, 4, 5, 6, 7, 9, 
                                           12, 13, 15, 24, 25, 27, 30, 31, 32,
                                       33, 34, 35);

    /** The game timer used for various task. */
    private final Timer          gameTimer;

    /** List of all the bets. */
    private final List<Bet>      allBets;

    /** The irc bot used for this game. */
    private final IrcBot         bot;
    
    /** The irc bot used for this game. */
    private final int            delay;

    /** The channel for this game. */
    private final String         channel;

    /** State variable. */
    private int                   state;

    
    /**
     * Constructor.
     * 
     * @param dly the delay between rounds.
     * @param chan the channel the game runs in.
     * @param irc the bot used.
     */
    public Roulette(final int dly, final String chan, final IrcBot irc) {
        // instantiate the bet lists
        allBets = new ArrayList<Bet>();
        channel = chan;
        state = OPEN;
        delay = dly;
        bot = irc;

        gameTimer = new Timer(true);
        gameTimer.schedule(new AnnounceEnd(bot, channel),
                           delay * Utils.MS_IN_MIN);
    }

    /**
     * This method handles the roulette commands.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        String message = event.getMessage();
        User sender = event.getUser();

        synchronized (BaseBot.getLockObject()) {
            if (channel.equalsIgnoreCase(event.getChannel().getName())
                    && bot.userIsIdentified(sender)) {
                try {
                    if (Utils.startsWith(message, BET_CMD)) {
                        bet(event);
                    } else if (Utils.startsWith(message, CXL_CMD)) {
                        cancel(event);
                    }
                } catch (Exception e) {
                    EventLog.log(e, "Roulette", "message");
                }
            }
        }
    }

    /**
     * This method handles the bet command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException when the system failed to perform db tasks
     */
    private void bet(final Message event) throws SQLException {
        // if we are not accepting bets
        DB db = DB.getInstance();
        User user = event.getUser();
        String username = user.getNick();
        String[] msg = event.getMessage().split(" ");

        if (state == CLOSE) {
            bot.sendIRCMessage(event.getChannel(), BETS_CLOSED);
        } else if (msg.length < BET_CMD_LEN) {
            bot.sendIRCNotice(user, INVALID_BET);
        } else {
            Double amount = Utils.tryParseDbl(msg[1]);
            double betsize = db.checkCredits(username, amount);
            String choice = msg[2].toLowerCase();
            Integer choicenum = Utils.tryParse(msg[2]);
            ProfileType profile = db.getActiveProfile(username);
            if (amount == null) {
                bot.sendIRCNotice(user, INVALID_BET);
            } else if (amount <= 0) {
                bot.sendIRCNotice(user, INVALID_BETSIZE);
            } else if (!((choice.equalsIgnoreCase("red")
                    || choice.equalsIgnoreCase("black")
                    || choice.equalsIgnoreCase("1st")
                    || choice.equalsIgnoreCase("2nd")
                    || choice.equalsIgnoreCase("3rd")
                    || choice.equalsIgnoreCase("even") || choice
                        .equalsIgnoreCase("odd")) || (choicenum != null
                    && choicenum >= 0 && choicenum <= NUMBER))) {
                bot.sendIRCNotice(user, INVALID_BET);
            } else if ((choicenum != null
                        && choicenum >= 0 && choicenum <= NUMBER)
                    && amount > Variables.MAXBET_ROUL_NUM) {
                bot.maxBet(user, event.getChannel(), Variables.MAXBET_ROUL_NUM);
            } else if (amount > Variables.MAXBET) {
                bot.maxBet(user, event.getChannel(), Variables.MAXBET);
            } else if (betsize <= 0.0) {
                bot.sendIRCNotice(user, NO_CHIPS);
            } else {
                Bet bet = new Bet(user, profile, GamesType.ROULETTE, betsize, choice);
                allBets.add(bet);

                String out = BET_MADE.replaceAll("%username", username);
                out = out.replaceAll("%choice", choice);
                out = out.replaceAll("%amount", Utils.chipsToString(betsize));
                bot.sendIRCMessage(event.getChannel(), out);
            }
        }
    }
    
    /**
     * This method handles the cancel command.
     * 
     * @param event the message event.
     * 
     * @throws SQLException if a bet failed to cancel.
     */
    private void cancel(final Message event) throws SQLException {
        Channel chan = bot.getUserChannelDao().getChannel(channel);
            
        if (state == CLOSE) {
            bot.sendIRCMessage(chan, BETS_CLOSED);
        } else {
            User user = event.getUser();
            List<Bet> found = new ArrayList<Bet>();
            for (Bet bet : allBets) {
                if (bet.getUser().compareTo(user) == 0) {
                    found.add(bet);
                }
            }
    
            if (found.size() > 0) {                
                for (Bet bet: found)  {
                    bet.cancel();
                    allBets.remove(bet);
                }
                bot.sendIRCMessage(chan, BETS_CANCELLED.replaceAll("%username", user.getNick()));
            }
        }
    }

    /**
     * Outputs the board to irc.
     * 
     * @param chan the channel to announce to.
     */
    private void printBoard(final Channel chan) {
        bot.sendIRCMessage(chan, BOARDL1);
        bot.sendIRCMessage(chan, BOARDL2);
        bot.sendIRCMessage(chan, BOARDL3);
    }
    
    /**
     * @param number the number to check
     * @return the colour this number is from.
     */
    private String getColour(final int number) {
        String ret = "ERROR";
        if (BLACK.contains(number)) {
            ret = "black";
        } else if (RED.contains(number)) {
            ret = "red";
        } else if (number == 0) {
            ret = "green";
        }
        return ret;
    }

    /**
     * @param number the number to check
     * @return the row this number is from.
     */
    private String getRow(final int number) {
        String ret = "ERROR";
        if (ROWONE.contains(number)) {
            ret = "1st";
        } else if (ROWTWO.contains(number)) {
            ret = "2nd";
        } else if (ROWTHREE.contains(number)) {
            ret = "3rd";
        }
        return ret;
    }

    /**
     * Ends the game and prints out winners.
     * 
     * @param ib the bot to output.
     * 
     * @throws SQLException when something goes wrong in the db.
     */
    private void endGame(final IrcBot ib) throws SQLException {
        Channel chan = bot.getUserChannelDao().getChannel(channel);
        
        // Let's "roll"
        int winner = Random.nextInt(RANDGENNUM);
        if (winner > NUMBER) {
            winner = 0;
        }
        boolean isEven = false;
        if (winner % 2 == 0 && winner != 0) {
            isEven = true;
        }

        List<String> nameList = new ArrayList<String>();

        for (Bet bet : allBets) {
            String choice = bet.getChoice();
            Integer choicenum = Utils.tryParse(choice);
            User user = bet.getUser();
            String username = user.getNick();
            
            int winamount = 0;
            boolean win = false;
            if ((choice.equalsIgnoreCase("red") || choice.equalsIgnoreCase("black"))
                    && choice.equalsIgnoreCase(getColour(winner))
                    && winner != 0) {
                winamount = ODDS_EVENS;
                win = true;
            } else if (((choice.equalsIgnoreCase("even") && isEven) 
                    || (choice.equalsIgnoreCase("odd") && !isEven)) && winner != 0) {
                win = true;
                winamount = ODDS_EVENS;
            } else if (winner != 0
                    && (choice.equalsIgnoreCase("1st")
                            || choice.equalsIgnoreCase("2nd") 
                            || choice.equalsIgnoreCase("3rd"))
                    && choice.equalsIgnoreCase(getRow(winner))) {
                win = true;
                winamount = ODDS_ROW;
            } else if (choicenum != null && winner == choicenum) {
                win = true;
                if (winner == 0) {
                    winamount = ODDS_ZERO;
                } else {
                    winamount = ODDS_NUMBER;
                }
            }

            if (win) {
                if (!nameList.contains(username)) {
                    nameList.add(username);
                }
                bet.win(winamount * bet.getAmount());
            }
            bet.getRake();
            bet.checkJackpot(ib);
            bet.close();
        }

        // Construct the winning string
        String winline = WIN_LINE;

        String board = "";
        if (getColour(winner).equalsIgnoreCase("red")) {
            board = "%c00,04 ";
        } else if (getColour(winner).equalsIgnoreCase("black")) {
            board = "%c00,01 ";
        } else {
            board = "%c00,03 ";
        }
        board += winner + " ";

        winline = winline.replaceAll("%board", board);
        bot.sendIRCMessage(chan, winline);

        // announce winners
        if (nameList.size() > 0) {
            String names = "";
            for (String user : nameList) {
                names += user + " ";
            }
            bot.sendIRCMessage(chan, CONGRATULATIONS.replaceAll("%names", names));
        }

        bot.sendIRCMessage(chan, NEW_GAME);
        printBoard(chan);
        bot.sendIRCMessage(chan, PLACE_BETS);

        // clear the bets
        allBets.clear();

        this.state = OPEN;
    }

    /**
     * Announces the end of the game (i.e no more bets).
     * @author Jamie
     */
    class AnnounceEnd extends TimerTask {
        /** The IRC bot. */
        private final IrcBot irc;
        
        /** The IRC channel. */
        private final String channel;

        /**
         * Constructor.
         * @param ib   The irc bot/server.
         * @param chan The channel.
         */
        public AnnounceEnd(final IrcBot ib,
                           final String chan) {
            irc = bot;
            channel = chan;
        }

        @Override
        public void run() {
            state = CLOSE;
            Channel chan = bot.getUserChannelDao().getChannel(channel);
            irc.sendIRCMessage(chan, SPINNING);
            gameTimer.schedule(new End(irc, channel),
                               NOBETSDELAYSECS * Utils.MS_IN_SEC);
        }
    }

    /**
     * Handles the ending of the roulette game.
     * 
     * @author Jamie
     */
    class End extends TimerTask {
        /** The IRC bot. */
        private final IrcBot irc;
        
        /** The IRC channel. */
        private final String channel;

        /**
         * Constructor.
         * @param ib   The irc bot/server.
         * @param chan The channel.
         */
        public End(final IrcBot ib,
                   final String chan) {
            irc = bot;
            channel = chan;
        }

        @Override
        public void run() {
            try {
                endGame(irc);
            } catch (Exception e) {
                EventLog.log(e, "Roulette", "timerTask");
            }
            gameTimer.schedule(new AnnounceEnd(irc, channel),
                            delay * Utils.MS_IN_MIN);
        }
    }
}
