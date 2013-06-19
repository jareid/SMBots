package org.smokinmils.games.rockpaperscissors;

/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.smokinmils.BaseBot;
import org.smokinmils.bot.Bet;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
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
 * Provides the functionality to give a user some chips.
 * 
 * @author Jamie
 */
public class RPSGame extends Event {
    /** The rps command. */
    public static final String  RPS_CMD        = "!rps";

    /** The rps command format. */
    public static final String  RPS_FORMAT     = "%b%c12" + RPS_CMD
                                                       + " <amount>";

    /** The call command. */
    public static final String  CALL_CMD       = "!rpscall";

    /** The call command format. */
    public static final String  CALL_FORMAT    = "%b%c12" + CALL_CMD
                                                       + " <who>";

    /** The cancel command. */
    public static final String  CXL_CMD        = "!rpscancel";

    /** The cancel command format. */
    public static final String  CXL_FORMAT     = "%b%c12" + CXL_CMD + " <who>";

    /** Message for an open wager existing. */
    private static final String OPENWAGER      = "%b%c04%who%c12: You already "
                                                       + "have a wager open, "
                                                       + "Type %c04"
                                                       + CXL_CMD
                                                       + "%c12 to cancel it";

    /** Message when a bet is cancelled. */
    private static final String CANCELLEDBET = "%b%c04%who%c12: Cancelled "
                                                            + "your open wager";

    /** Message when someone calls a non-existing bet. */
    private static final String NOBET          = "%b%c04%who%c12: I can't find "
                                                     + "a record of that wager";
    
    /** Message to say yyou can't bet against yourself. */
    private static final String SELFBET        = "%b%c04%who%c12: You can't "
                                                     + "play against yourself!";

    /** Message to say you opened a new bet. */
    private static final String OPENEDWAGER    = "%b%c04%who%c12: has opened a "
                   + "new RPS wager of %c04%amount%c12 %profile chips! To call "
                   + "this wager type %c04" + CALL_CMD + " %who";
    
    /** Message when user hasn't got enough chips. */
    private static final String NOCHIPS        = "%b%c12Sorry, you do not have "
           + "%c04%chips%c12 chips available for the %c04%profile%c12 profile.";
    
    /** Message for mixed profile bets. */
    private static final String REALCHIPSONLY  = "%b%c04%who%c12: : you need "
        + "to use %c04%profile%c12 chips to call a %c04%profile%c12 chips rps!";
    
    /** Message when someone wins. */
    private static final String WIN            = "%b%c12%winstring. " 
              + "%c04%loser%c12 loses and %c04%winner%c12 wins %c04%chips%c12!";
    
    /** Message when someone draws. */
    private static final String DRAW           = "%b%c04%better%c12 and "
         + "%c04%caller%c12 draw with %c04%choice%c12! Attempting to replay...";
    
    /** Message when a replay failed. */
    private static final String REPLAYFAIL     = "%b%c12Replay between "
           + "%c04%better%c12 and %c04%caller%c12 failed as %c04%who%c12 didn't"
              + " respond. Both users have been refunded %c04%chips%c12 chips!";
    
    /** Output of valid choices. */
    private static final String VALIDCHOICES   = "%b%c04%who%c12: Please "
                      + "choose an option and enter it here. Valid choices are:"
                      + "%c04%choices%c12!";
    
    /** Message to retrieve a user's hoice. */
    private static final String PLEASECHOOSE   = "%b%c12You have received " 
             + "a query asking for your choice. Please send your choice in the "
             + "query and not in this channel.";
    
    /** Message when someone choose correctly. */
    private static final String VALIDCHOICE    = "%b%c12You have chosen "
                                               + "%c04%choice%c12!";
    
    /** Message when someone choose incorrectly. */
    private static final String INVALIDCHOICE  = "%b%c04%what%c12 is invalid. "
                                       + "Valid choices are: %c04%choices%c12!";

    
    /** Message when someone doesn't choose. */
    private static final String NOCHOICE       = "%b%c04%who%c12: You didn't "
                    + "make a choice for the game, the bet has been cancelled.";
    
    /** Mesaage listing open bets. */
    private static final String OPENBETS       = "%c12%bCurrent open RPS "
             + "wagers: %bets To call a wager type %c04" + CALL_CMD + " <name>";

    /** Message for each bet. */
    private static final String EACHOPENBET    = "%c04%user%c12(%c04%amount"
                                               + " %profile%c12)";

    /** Number of minutes for open bet announce. */
    private static final int    ANNOUNCE_MINS   = 3;

    /** Number of seconds for a user to choose an option. */
    private static final int    CHOICE_SECS    = 25;

    /** The list of open bets. */
    private final List<Bet>     openBets;
    
    /** The list of pending bets. */
    private final List<String>  pendingBets;

    /**
     * Constructor.
     */
    public RPSGame() {
        openBets = new ArrayList<Bet>();
        pendingBets = new ArrayList<String>();
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
            if (Utils.startsWith(message, CXL_CMD)) {
                cancel(event);
            } else if (Utils.startsWith(message, CALL_CMD)) {
                call(event);
            } else if (Utils.startsWith(message, RPS_CMD)) {
                newGame(event);
            }
        }
    }

    /**
     * This method handles the cancel command.
     * 
     * @param event the message event.
     */
    private void cancel(final Message event) {
        synchronized (BaseBot.getLockObject()) {
            // try to locate and cancel the bet else ignore
            User user = event.getUser();
            String username = event.getUser().getNick();
            Bet found = null;
            for (Bet bet : openBets) {
                if (bet.getUser().compareTo(user) == 0) {
                    DB db = DB.getInstance();
                    found = bet;
                    try {
                        db.adjustChips(username,
                                bet.getAmount(),
                                bet.getProfile(),
                                GamesType.ROCKPAPERSCISSORS,
                                TransactionType.CANCEL);
    
                        db.deleteBet(username, GamesType.ROCKPAPERSCISSORS);
                    } catch (Exception e) {
                        EventLog.log(e, "RPSGame", "cancel");
                    }
                    // Announce
                    event.getBot().sendIRCMessage(
                            event.getChannel(),
                            CANCELLEDBET.replaceAll("%who", username));
                    break;
                }
            }
    
            if (found != null) {
                openBets.remove(found);
            }
        }
    }

    /**
     * This method handles the call command.
     * 
     * @param event the message event.
     */
    private void call(final Message event) {
        boolean playbet = false;
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User caller = event.getUser();
        Channel chan = event.getChannel();
        String[] msg = message.split(" ");

        if (msg.length == 2) {
            String better = msg[1];
            User betteru = null;

            // check to see if someone is playing themselves...
            if (better.equalsIgnoreCase(caller.getNick())) {
               bot.sendIRCMessage(chan, SELFBET.replaceAll("%who",
                                           caller.getNick()));
            } else {
                DB db = DB.getInstance();
                ProfileType callerprof = null;
                ProfileType betterprof = null;
                Bet found = null;
                double amount = 0.0;
                synchronized (BaseBot.getLockObject()) {
                    for (Bet bet : openBets) {
                        if (bet.getUser().getNick().equalsIgnoreCase(better)) {
                            found = bet;
                            betteru = bet.getUser();
                            break;
                        }
                    }
    
                    if (found != null) {
                        try {
                            callerprof = db.getActiveProfile(caller.getNick());
                            betterprof = found.getProfile();
                            amount = found.getAmount();
        
                            if (callerprof != betterprof) {
                                String out = REALCHIPSONLY.replaceAll(
                                        "%who", caller.getNick());
                                out = out.replaceAll(
                                        "%profile", betterprof.toString());
                                bot.sendIRCMessage(chan, out);
                            } else if (db.checkCredits(
                                    caller.getNick()) < amount) {
                                String out = NOCHIPS.replaceAll(
                                     "%chips", Utils.chipsToString(amount));
                                out = out.replaceAll(
                                        "%profile", callerprof.toString());
                                bot.sendIRCMessage(chan, out);
                            } else {
                                db.adjustChips(caller.getNick(), -amount,
                                               callerprof,
                                               GamesType.ROCKPAPERSCISSORS,
                                               TransactionType.BET);
                                playbet = true;
                            }
                        } catch (Exception e) {
                            EventLog.log(e, "RPSGame", "call");
                        }
                    } else {
                        // if we reach here the game doesn't exist
                        String out = NOBET.replaceAll("%who", caller.getNick());
                        bot.sendIRCMessage(chan, out);
                    }
                }
                
                if (playbet) {
                    try {
                        GameLogic choice = getChoice(event.getUser(),
                                                     event.getBot());
                        if (choice != null) {
                            db.deleteBet(better,
                                    GamesType.ROCKPAPERSCISSORS);
                            openBets.remove(found);
    
                            GameLogic betterchoice = GameLogic
                                    .fromString(found.getChoice());
                            GameLogic callerchoice = choice;
    
                            endGame(betteru, betterprof, betterchoice,
                                    caller, callerprof, callerchoice,
                                    amount, bot, chan);
                        } else {
                            db.adjustChips(caller.getNick(), amount, callerprof,
                                           GamesType.ROCKPAPERSCISSORS,
                                           TransactionType.CANCEL);
                            bot.sendIRCMessage(chan, NOCHOICE
                                    .replaceAll("%who", event.getUser()
                                            .getNick()));
                        }
                    } catch (Exception e) {
                        EventLog.log(e, "RPSGame", "call");
                    }
                }
            }
        } else {
            bot.invalidArguments(caller, RPS_FORMAT);
        }
    }

    /**
     * This method handles the rps command.
     * 
     * @param event the message event.
     */
    private void newGame(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        String[] msg = message.split(" ");

        if (msg.length == 2) {
            boolean playbet = false;
            boolean hasopen = false;
            ProfileType profile = null;
            double betsize = 0.0;
            synchronized (BaseBot.getLockObject()) {
                for (Bet bet : openBets) {
                    if (bet.getUser().compareTo(sender) == 0) {
                        hasopen = true;
                        break;
                    }
                }
                
                if (hasopen || pendingBets.contains(sender.getNick())) {
                    String out = OPENWAGER.replaceAll("%who",
                            sender.getNick());
                    bot.sendIRCNotice(sender, out);
                } else {
                    Double amount = Utils.tryParseDbl(msg[1]);
                    if (amount == null || amount == 0) {
                        bot.invalidArguments(sender, RPS_FORMAT);
                    } else if (amount > Variables.MAXBET) {
                        bot.maxBet(sender, chan, Variables.MAXBET);
                    } else {
                        DB db = DB.getInstance();
                        try {
                            profile = db.getActiveProfile(sender.getNick());
                            betsize = db.checkCredits(sender.getNick(),
                                                      amount);
                            if (betsize > 0.0) {
                                playbet = true;
                                pendingBets.add(sender.getNick());
                                // Add a lock to a temp bet list
                            } else {
                                String out = NOCHIPS.replaceAll(
                                     "%chips", Utils.chipsToString(amount));
                                out = out.replaceAll("%profile",
                                                     profile.toString());
                                bot.sendIRCMessage(chan, out);
                            }
                        } catch (Exception e) {
                            EventLog.log(e, "RPSGame", "newGame");
                        }
                    }
                }
            }
            
            /* Play the bet outside of the synchronisation. */
            if (playbet) {
                DB db = DB.getInstance();
                try {
                    // add bet, remove chips, notify channel
                    GameLogic choice = getChoice(event.getUser(),
                                                 event.getBot());
                    if (choice != null) {
                        Bet bet = new Bet(sender, profile, betsize,
                                choice.toString());
                        openBets.add(bet);
                        db.adjustChips(sender.getNick(), -betsize, profile,
                                GamesType.ROCKPAPERSCISSORS,
                                TransactionType.BET);
                        db.addBet(sender.getNick(), choice.toString(),
                                  betsize,
                                  profile, GamesType.ROCKPAPERSCISSORS);

                        String out = OPENEDWAGER.replaceAll("%who",
                                                          sender.getNick());
                        out = out.replaceAll("%profile",
                                             profile.toString());
                        out = out.replaceAll("%amount",
                                        Utils.chipsToString(betsize));
                        bot.sendIRCMessage(chan, out);
                    } else {
                        db.adjustChips(sender.getNick(), betsize, profile,
                                GamesType.ROCKPAPERSCISSORS,
                                TransactionType.CANCEL);
                        bot.sendIRCMessage(chan, NOCHOICE.replaceAll(
                                "%who", event.getUser().getNick()));
                    }
                    pendingBets.remove(sender.getNick());
                } catch (Exception e) {
                    EventLog.log(e, "RPSGame", "newGame");
                }
            }
        } else {
            bot.invalidArguments(sender, RPS_FORMAT);
        }
    }

    /**
     * Retrieves a user's response.
     * 
     * @param user The user to ask for a choice from
     * @param bot The bot to get the choice
     * 
     * @return The GameLogic object
     */
    private GameLogic getChoice(final User user,
                                final IrcBot bot) {
        GameLogic choice = null;
        ExecutorService executor = Executors.newFixedThreadPool(1);
        FutureTask<GameLogic> choicetask = new FutureTask<GameLogic>(
                new GetChoice(bot, user));
        executor.execute(choicetask);
        try {
            choice = choicetask.get(CHOICE_SECS, TimeUnit.SECONDS);
            bot.sendIRCMessage(user, 
                    VALIDCHOICE.replaceAll("%choice", choice.toString()));
        } catch (TimeoutException e) {
            // Do nothing, we expect this.
            choice = null;
        } catch (InterruptedException | ExecutionException e) {
            EventLog.log(e, "RPSGame", "getChoice");
        }
        executor.shutdown();

        return choice;
    }

    /**
     * Ends a game between two users.
     * 
     * @param better The better.
     * @param bprof The better's profile.
     * @param bchoice The better's choice.
     * @param caller The caller.
     * @param cprof The caller's profile.
     * @param cchoice The caller's choice.
     * @param amount The amount.
     * @param bot The bot used.
     * @param chan The channel it was performed in.
     */
    private void endGame(final User better,
                         final ProfileType bprof,
                         final GameLogic bchoice,
                         final User caller,
                         final ProfileType cprof,
                         final GameLogic cchoice,
                         final double amount,
                         final IrcBot bot,
                         final Channel chan) {
        GameLogicComparator c = new GameLogicComparator();
        int order = c.compare(bchoice, cchoice);
        String winstr = c.getWinString();

        if (order == -1) {
            // better won
            doWin(
                    better, bprof, bchoice, caller, cprof,
                    cchoice, amount, winstr, bot, chan);
        } else if (order == 1) {
            // caller won
            doWin(
                    caller, cprof, cchoice, better, bprof,
                    bchoice, amount, winstr, bot, chan);
        } else {
            doDraw(
                    better, bprof, caller, cprof, amount, bot,
                    chan, cchoice);
        }
    }

    /**
     * Performs a win between two users.
     * 
     * @param winner The winner.
     * @param wprof The winner's profile.
     * @param wchoice The winner's choice.
     * @param loser The loser.
     * @param lprof The loser's profile.
     * @param lchoice The loser's choice.
     * @param amount The amount.
     * @param winstring The win string.
     * @param bot The bot used.
     * @param chan The channel it was performed in.
     */
    private void doWin(final User winner,
                       final ProfileType wprof,
                       final GameLogic wchoice,
                       final User loser,
                       final ProfileType lprof,
                       final GameLogic lchoice,
                       final double amount,
                       final String winstring,
                       final IrcBot bot,
                       final Channel chan) {
        DB db = DB.getInstance();
        // Take the rake and give chips to winner
        double rake = Rake.getRake(winner.getNick(), amount, wprof)
                + Rake.getRake(loser.getNick(), amount, lprof);
        double win = (amount * 2) - rake;

        try {
            db.adjustChips(
                    winner.getNick(), win, wprof, GamesType.ROCKPAPERSCISSORS,
                    TransactionType.WIN);

            // Announce winner and give chips
            String out = WIN.replaceAll("%winstring", winstring);
            out = out.replaceAll("%winner", winner.getNick());
            out = out.replaceAll("%loser", loser.getNick());
            out = out.replaceAll("%chips", Utils.chipsToString(win));
            bot.sendIRCMessage(chan, out);
        } catch (Exception e) {
            EventLog.log(e, "RPSGame", "updateJackpot");
        }

        // jackpot stuff
        if (Rake.checkJackpot(amount)) {
            ArrayList<String> players = new ArrayList<String>();
            players.add(loser.getNick());
            Rake.jackpotWon(wprof, GamesType.ROCKPAPERSCISSORS, players, bot,
                            chan);
        } else if (Rake.checkJackpot(amount)) {
            ArrayList<String> players = new ArrayList<String>();
            players.add(winner.getNick());
            Rake.jackpotWon(wprof, GamesType.ROCKPAPERSCISSORS, players, bot,
                            chan);
        }
    }

    /**
     * Performs a draw between two users.
     * 
     * @param better The better.
     * @param bprof The better's profile.
     * @param caller The caller.
     * @param cprof The caller's profile.
     * @param amount The amount.
     * @param bot The bot used.
     * @param chan The channel it was performed in.
     * @param choice The choice.
     */
    private void doDraw(final User better,
                        final ProfileType bprof,
                        final User caller,
                        final ProfileType cprof,
                        final double amount,
                        final IrcBot bot,
                        final Channel chan,
                        final GameLogic choice) {
        // Announce winner and give chips
        String out = DRAW.replaceAll("%choice", choice.toString());
        out = out.replaceAll("%better", better.getNick());
        out = out.replaceAll("%caller", caller.getNick());
        bot.sendIRCMessage(chan, out);

        boolean cxld = false;
        String who = "";
        GameLogic bchoice = getChoice(better, bot);
        if (bchoice != null) {
            GameLogic cchoice = getChoice(caller, bot);
            if (cchoice != null) {
                endGame(caller, cprof, cchoice, better, bprof,
                        bchoice, amount, bot, chan);
            } else {
                who = caller.getNick();
                cxld = true;
            }
        } else {
            who = better.getNick();
            cxld = true;
        }

        if (cxld) {
            DB db = DB.getInstance();
            // cancel bets.
            try {
                db.adjustChips(better.getNick(), amount, bprof,
                        GamesType.ROCKPAPERSCISSORS, TransactionType.CANCEL);

                db.adjustChips(caller.getNick(), amount, cprof,
                        GamesType.ROCKPAPERSCISSORS, TransactionType.CANCEL);

                String fail = REPLAYFAIL;
                fail = fail.replaceAll("%better", better.getNick());
                fail = fail.replaceAll("%caller", caller.getNick());
                fail = fail.replaceAll("%who", who);
                fail = fail.replaceAll("%chips", Utils.chipsToString(amount));
                bot.sendIRCMessage(chan, fail);
            } catch (Exception e) {
                EventLog.log(e, "RPSGame", "doDraw");
            }
        }
    }

    /**
     * Adds a channel that announces open bets.
     * 
     * @param channel the channel name
     * @param bot the IRC bot object
     */
    public final void addAnnounce(final String channel,
                                  final IrcBot bot) {
        Timer chantimer = new Timer(true);
        chantimer.scheduleAtFixedRate(new OpenBetsAnnounce(bot, channel),
                ANNOUNCE_MINS * Utils.MS_IN_MIN,
                ANNOUNCE_MINS * Utils.MS_IN_MIN);
    }

    /**
     * A callable class that is used to get a user's choice or time out.
     * 
     * @author Jamie
     */
    class GetChoice implements Callable<GameLogic> {
        /** The bot to get the choice. */
        private final IrcBot irc;
        /** The user to get the choice from. */
        private final User user;

        /**
         * Constructor.
         * 
         * @param bot The bot to get the choice.
         * @param usr The user to get the choice from.
         */
        public GetChoice(final IrcBot bot, final User usr) {
            irc = bot;
            user = usr;
        }

        @Override
        @SuppressWarnings("unchecked")
        public GameLogic call() {
            GameLogic choice = null;
            WaitForQueue queue = new WaitForQueue(irc);
            boolean received = false;
            String choices = Arrays.asList(GameLogic.values()).toString();
            choices = choices.substring(1, choices.length() - 1);

            irc.sendIRCMessage(
                    user, VALIDCHOICES.replaceAll("%choices", choices)
                            .replaceAll("%who", user.getNick()));
            irc.sendIRCNotice(user, PLEASECHOOSE);

            // Loop until we receive the correct message
            while (!received) {
                // Use the waitFor() method to wait for a MessageEvent.
                // This will block (wait) until a message event comes in,
                // ignoring
                // everything else
                PrivateMessageEvent<IrcBot> currentEvent = null;
                try {
                    currentEvent = queue.waitFor(PrivateMessageEvent.class);
                } catch (InterruptedException ex) {
                    EventLog.log(ex, "RPSGame", "getChoice");
                }

                // Check if this message is the response
                String msg = currentEvent.getMessage().toLowerCase();
                if (currentEvent.getUser().getNick()
                        .equalsIgnoreCase(user.getNick())) {
                    // get and store choice
                    choice = GameLogic.fromString(msg);
                    if (choice == null) {
                        String out = INVALIDCHOICE.replaceAll("%what", msg);
                        out = out.replaceAll(
                                "%choices", Arrays.asList(GameLogic.values())
                                        .toString());
                        irc.sendIRCMessage(user, out);
                    } else {
                        queue.close();
                        received = true;
                    }
                }
            }
            return choice;
        }
    }

    /**
     * A task to announce open bets for this game.
     * 
     * @author Jamie
     */
    public class OpenBetsAnnounce extends TimerTask {
        /** The bot used for announcing. */
        private final IrcBot irc;

        /** The channel to announce on. */
        private final String channel;

        /**
         * Constructor.
         * 
         * @param bot The bot to announce with.
         * @param chan The channel to announce on.
         */
        public OpenBetsAnnounce(final IrcBot bot, final String chan) {
            irc = bot;
            channel = chan;
        }

        /**
         * (non-Javadoc).
         * @see java.util.TimerTask#run()
         */
        @Override
        public final void run() {
            if (openBets.size() > 0) {
                String bets = "";
                for (Bet bet : openBets) {
                    String betstr = EACHOPENBET.replaceAll(
                            "%user", bet.getUser().getNick());
                    betstr = betstr.replaceAll(
                            "%amount", Utils.chipsToString(bet.getAmount()));
                    betstr = betstr.replaceAll("%profile", bet.getProfile()
                            .toString());
                    bets += betstr;
                }
                String out = OPENBETS.replaceAll("%bets", bets);
                
                Channel chan = irc.getUserChannelDao().getChannel(channel);
                irc.sendIRCMessage(chan, out);
            }
        }
    }
}
