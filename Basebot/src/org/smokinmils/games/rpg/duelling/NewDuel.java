package org.smokinmils.games.rpg.duelling;

/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
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
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.SpamEnforcer;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
//import org.smokinmils.cashier.rake.Rake; TODO Rake
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.Variables;

/**
 * Provides the functionality to give a user some coins.
 * 
 * @author cjc
 */
public class NewDuel extends Event {
    /** The rps command. */
    public static final String  DUEL_CMD        = "!duel";

    /** The rps command format. */
    public static final String  DUEL_FORMAT     = "%b%c12" + DUEL_CMD
                                                       + " <amount>";

    /** The call command. */
    public static final String  CALL_CMD       = "!duelcall";

    /** The call command format. */
    public static final String  CALL_FORMAT    = "%b%c12" + CALL_CMD
                                                       + " <who>";

    /** The cancel command. */
    public static final String  CXL_CMD        = "!duelcancel";

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
                                                            + "your open duel";

    /** Message when someone calls a non-existing bet. */
    private static final String NOBET          = "%b%c04%who%c12: I can't find "
                                                     + "a record of that duel";
    
    /** Message to say yyou can't bet against yourself. */
    private static final String SELFBET        = "%b%c04%who%c12: You can't "
                                                     + "fight against yourself!";

    /** Message to say you opened a new bet. */
    private static final String OPENEDWAGER    = "%b%c04%who%c12: has opened a "
                   + "new Duel wager of %c04%amount%c12 %profile chips! To call "
                   + "this wager type %c04" + CALL_CMD + " %who";
    
    /** Message when user hasn't got enough coins. */
    private static final String NOCOINS        = "%b%c12Sorry, you do not have "
           + "%c04%coins%c12 chips available for the %c04%profile%c12 profile.";
    
    /** Message for mixed profile bets. */
    private static final String WRONGPROFILE  = "%b%c04%who%c12: : This duel is "
        + "using %c04%profile%c12 chips!";
    
   /** Message for a win. */
    private static final String WINNER = "%b%c12The duel between %c04%winner%c12 and "
            + "%c04%loser%c12 is over, %c04%winner%c12 has won!";
    
    /** Message for a draw! */
    private static final String DRAW = "%b%c12The duel is over, %c04%p1%c12 and %c04%p2%c12 drew!";
    
    /** Output of valid choices. */
    private static final String VALIDCHOICES   = "%b%c04%who%c12: [ROUND %c04%round%c12]"
                      + " You currently have "
                      +  "%c04%hpHP%c12, and %c04%enemy%c12 has %c04%ehpHP%c12. Please "
                      + "choose an option and enter it here. Valid choices are: "
                      + "%c04%choices%c12!";
    
    /** Message to retrieve a user's hoice. */
    private static final String QUERYNOTIFICATION   = "%b%c12You will receive " 
             + "queries asking for your choice. Please send your choices in the "
             + "query and not in this channel for the duration of the duel!";
    
    /** Message when someone choose correctly. */
    private static final String VALIDCHOICE    = "%b%c12You have chosen "
                                               + "%c04%choice%c12!";
    
    /** Message when someone choose incorrectly. */
    private static final String INVALIDCHOICE  = "%b%c04%what%c12 is invalid. "
                                       + "Valid choices are: %c04%choices%c12!";

    
    /** Mesaage listing open bets. */
    private static final String OPENBETS       = "%c12%bCurrent open Duel "
             + "wagers: %bets To call a wager type %c04" + CALL_CMD + " <name>";

    /** Message for each bet. */
    private static final String EACHOPENBET    = "%c04%user%c12(%c04%amount"
                                               + " %profile%c12)";

    /** Number of minutes for open bet announce. */
    private static final int    ANNOUNCE_MINS   = 3;

    /** Number of seconds for a user to choose an option. */
    private static final int    CHOICE_SECS    = 25;

    /** The list of pending bets containing users who are waiting to start a duel. */
    private final List<NewDuelState>  games;

    /** The fast channel for the game. */
    private static final String FAST_CHAN = "#SM_Express";
    
    /** Temp var that represents damage. */
    private static final int DAMAGE = 40;
    
    // def is the inverse ie 1.0 is 100% damage taken, 0.0  is 100% def
    
    /** The damage dealt by a jab attack. */
    private static final double JAB_DMG = 0.5;
    /** The damage taken by a jab attack. */
    private static final double JAB_DEF = 0.8;
    
    /** The damage done by an uppercut attack. */
    private static final double UPC_DMG = 1.0;
    /** The damage taken with a uppercut attack. */
    private static final double UPC_DEF = 1.0;
    
    /** The damage dealt with a block attack. */
    private static final double BLK_DMG = 0.0;
    /** The damage taken with a block attack. */
    private static final double BLK_DEF = 0.2;
    
    /** Mapping of GameLogic to damage. */
    private EnumMap<GameLogic, Double> damage;
    
    /** Mapping of Gamelogic to defence. */
    private EnumMap<GameLogic, Double> defence;

    /** the default choice for when they don't make one! :( */
    private static GameLogic DEFAULTCHOICE = GameLogic.JAB;
    
    /** Message to notify the user that they are on the default choice. */
    private static final String TIMEOUT = "%c12You timed out to make a move. "
            + "You have been set the default of %c04" + DEFAULTCHOICE.toString();
    
    
    
    /**
     * Constructor.
     */
    public NewDuel() {
        games = new ArrayList<NewDuelState>();
        damage = new EnumMap<GameLogic, Double>(GameLogic.class);
        defence = new EnumMap<GameLogic, Double>(GameLogic.class);

        damage.put(GameLogic.BLOCK, BLK_DMG);
        damage.put(GameLogic.JAB, JAB_DMG);
        damage.put(GameLogic.UPPERCUT, UPC_DMG);
        
        defence.put(GameLogic.BLOCK, BLK_DEF);
        defence.put(GameLogic.JAB, JAB_DEF);
        defence.put(GameLogic.UPPERCUT, UPC_DEF);
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
        SpamEnforcer se = SpamEnforcer.getInstance();

        if (isValidChannel(chan.getName()) && bot.userIsIdentified(sender)) {
            if (Utils.startsWith(message, CXL_CMD)) {
               cancel(event);
            } else if (Utils.startsWith(message, CALL_CMD)) {
                call(event);
            } else if (Utils.startsWith(message, DUEL_CMD)) {
                if (se.check(event, FAST_CHAN)) { newGame(event); }
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
            NewDuelState found = null;
            for (NewDuelState nds : games) {
                if (nds.getP1().equals(user) && !nds.hasPlayer2()) {
                    found = nds;
                    break;
                }
            }
    
            if (found != null) {
                try {
                    // cancel / refund
                    games.remove(found);
                    found.cancel();
                    event.getBot().sendIRCMessage(event.getChannel(), 
                                                  CANCELLEDBET.replaceAll("%who", user.getNick()));
                } catch (Exception e) {
                    EventLog.log(e, "NewDuel", "cancel");
                }
            }
        }
    }

    
    /**
     * This method handles the call command.
     * 
     * @param event the message event.
     */
    private void call(final Message event) {
        // call needs to: locate game, make sure caller has money and isn't calling themselves
        // start a loop that will check if the game is over, and keep asking for choices (maybe 
        // show hp etc)
        
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        
        User p1 = null;
        User p2 = event.getUser();
        Channel chan = event.getChannel();
        String[] msg = message.split(" ");
        DB db = DB.getInstance();
        ProfileType p2Prof = null;
        try {
            p2Prof = db.getActiveProfile(p2.getNick());
        } catch (SQLException e) {
            EventLog.log(e, "NewDuel", "call");
        }
        
        if (msg.length == 2) {
            String p1nick = msg[1];
            NewDuelState theGame = null;
            // check if the bet has already been called
            for (NewDuelState nds : games) {
                p1 = nds.getP1();
                if (p1.getNick().equalsIgnoreCase(p1nick)) {
                    theGame = nds;
                    break;
                }
            }
            try {
                // check to see if someone is playing themselves...
                if (p2.getNick().equalsIgnoreCase(p1nick)) {
                   bot.sendIRCMessage(chan, SELFBET.replaceAll("%who", p2.getNick()));
                } else if (theGame.hasPlayer2()) {
                    // the bet has already been called. 
                    // this should never happen, but no harm in checking
                    String out = NOBET.replaceAll("%who", p2.getNick());
                    bot.sendIRCMessage(chan, out);
                } else if (theGame.getProfile() != p2Prof) {
                    String out = WRONGPROFILE.replaceAll("%who", p2.getNick());
                    out = out.replaceAll("%profile", theGame.getProfile().toString());
                    bot.sendIRCMessage(chan, out);
                } else if (db.checkCredits(p2.getNick()) < theGame.getAmount()) {
                    String out = NOCOINS.replaceAll("%who", p2.getNick());
                    out = out.replaceAll("%coins", Utils.chipsToString(theGame.getAmount()));
                    out = out.replaceAll("%profile", theGame.getProfile().toString());
                } else {
                
                    theGame.start(p2);
                    // send the notification about queries only once
                    bot.sendIRCNotice(p1, QUERYNOTIFICATION);
                    bot.sendIRCNotice(p2, QUERYNOTIFICATION);
                    
                   while (theGame.getWinner() > 2) {
                       // get p1, then get p2, sequentially? :/
                       GameLogic p1move = getChoice(theGame.getP1(), event.getBot(), theGame);
                       if (p1move == null) {
                           // they haven't made a choice, chose the default and then tell them.
                           p1move = DEFAULTCHOICE;
                           bot.sendIRCMessage(p1, TIMEOUT);
                       }    
                       GameLogic p2move = getChoice(theGame.getP2(), event.getBot(), theGame);
                       if (p2move == null) {
                           // they haven't made a choice, chose the default and then tell them.
                           p2move = DEFAULTCHOICE;
                           bot.sendIRCMessage(p2, TIMEOUT);
                       }
                       double p1dmg = DAMAGE * damage.get(p1move) * defence.get(p2move);
                       double p2dmg = DAMAGE * damage.get(p2move) * defence.get(p1move);
                       
                       theGame.endRound(p1dmg, p2dmg);
                   }
                   
                   // DUN
                   int result = theGame.getWinner();
                   String out = "";
                   if (result == 0) {
                       // draw
                       out = DRAW.replaceAll("%p1", theGame.getP1().getNick());
                       out = out.replaceAll("%p2", theGame.getP2().getNick());
                   } else if (result == 1) {
                       // p1 winner
                       out = WINNER.replaceAll("%winner", theGame.getP1().getNick());
                       out = out.replaceAll("%loser", theGame.getP2().getNick());
                   } else {
                       out = WINNER.replaceAll("%winner", theGame.getP2().getNick());
                       out = out.replaceAll("%loser", theGame.getP1().getNick());
                   }
                   bot.sendIRCMessage(chan, out);
                   games.remove(theGame);
                }   
            } catch (Exception e) {
                EventLog.log(e, "NewDuel", "call");
            }
        } else {
            bot.invalidArguments(p2, DUEL_FORMAT);
        } 
    }

    /**
     * This method handles the newduel command.
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
            boolean hasopen = false;
            ProfileType profile = null;
            double betsize = 0.0;
            synchronized (BaseBot.getLockObject()) {
                // check for pending games, or open games.
                for (NewDuelState nds : games) {
                    if (nds.contains(sender)) {
                        hasopen = true;
                        break;
                    }
                }
                if (hasopen) {
                    String out = OPENWAGER.replaceAll("%who", sender.getNick());
                    bot.sendIRCNotice(sender, out);
                } else {
                    Double amount = Utils.tryParseDbl(msg[1]);
                    if (amount == null || amount == 0) {
                        bot.invalidArguments(sender, DUEL_FORMAT);
                    } else if (amount > Variables.MAXBET) {
                        bot.maxBet(sender, chan, Variables.MAXBET);
                    } else {
                        DB db = DB.getInstance();
                        try {
                            profile = db.getActiveProfile(sender.getNick());
                            betsize = Utils.checkCredits(sender, amount, bot, chan);
                            if (betsize > 0.0) {
                                NewDuelState nds = new NewDuelState(sender, profile, betsize);
                                games.add(nds);
                                String out = OPENEDWAGER.replaceAll("%who", sender.getNick());
                                out = out.replaceAll("%profile", nds.getProfile().toString());
                                out = out.replaceAll("%amount", 
                                        Utils.chipsToString(nds.getAmount()));
                                bot.sendIRCMessage(chan, out);
                            } else {
                                String out = NOCOINS.replaceAll("%coins", 
                                                                Utils.chipsToString(amount));
                                out = out.replaceAll("%profile", profile.toString());
                                bot.sendIRCMessage(chan, out);
                            }
                        } catch (Exception e) {
                            EventLog.log(e, "NewDuel", "newGame");
                        }
                    }
                }
            }
        } else {
            bot.invalidArguments(sender, DUEL_FORMAT);
        }
    }

    /**
     * Retrieves a user's response.
     * 
     * @param user The user to ask for a choice from
     * @param bot The bot to get the choice
     * @param nds the game state
     * 
     * @return The GameLogic object
     */
    private GameLogic getChoice(final User user,
                                final IrcBot bot,
                                final NewDuelState nds) {
        GameLogic choice = null;
        ExecutorService executor = Executors.newFixedThreadPool(1);
        FutureTask<GameLogic> choicetask = new FutureTask<GameLogic>(
                new GetChoice(bot, user, nds));
        executor.execute(choicetask);
        try {
            choice = choicetask.get(CHOICE_SECS, TimeUnit.SECONDS);
            bot.sendIRCMessage(user, 
                    VALIDCHOICE.replaceAll("%choice", choice.toString()));
        } catch (TimeoutException e) {
            // Do nothing, we expect this.
            choice = null;
        } catch (InterruptedException | ExecutionException e) {
            EventLog.log(e, "NewDuel", "getChoice");
        }
        executor.shutdown();
        
        return choice;
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
        /** The game state to get HP info. */
        private NewDuelState nds;
        
        /**
         * Constructor.
         * 
         * @param bot The bot to get the choice.
         * @param usr The user to get the choice from.
         * @param n the state we are tracking 
         */
        public GetChoice(final IrcBot bot, final User usr, final NewDuelState n) {
            irc = bot;
            user = usr;
            nds = n;
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
                            .replaceAll("%who", user.getNick())
                            .replaceAll("%hp", Utils.chipsToString(nds.getHP(user)))
                            .replaceAll("%ehp", Utils.chipsToString(nds.getEnemyHP(user)))
                            .replaceAll("%round", String.valueOf(nds.getRound()))
                            .replaceAll("%enemy", nds.getEnemy(user).getNick()));

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
                    EventLog.log(ex, "NewDuel", "getChoice");
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
            int count = 0;
            if (games.size() > 0) {
                String bets = "";
                for (NewDuelState nds : games) {
                    if (!nds.hasPlayer2()) {
                        String betstr = EACHOPENBET.replaceAll(
                                "%user", nds.getP1().getNick());
                        betstr = betstr.replaceAll(
                                "%amount", Utils.chipsToString(nds.getAmount()));
                        betstr = betstr.replaceAll("%profile", nds.getProfile()
                                .toString());
                        bets += betstr;
                        count++;
                    }
                }
                
                if (count > 0) {
                    String out = OPENBETS.replaceAll("%bets", bets);
                    
                    Channel chan = irc.getUserChannelDao().getChannel(channel);
                    irc.sendIRCMessage(chan, out);
                }
            }
        }
    }
}

