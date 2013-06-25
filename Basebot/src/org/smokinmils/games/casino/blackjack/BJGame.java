package org.smokinmils.games.casino.blackjack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Channel;
import org.pircbotx.User;

import org.smokinmils.BaseBot;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.cards.Card;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.Variables;

/** 
 * Provides the functionality for BlackJack via IRC.
 * 
 * @author cjc
 */
public class BJGame extends Event {

    /** The stand command. */
    public static final String BJ_CMD = "!deal";
    
    /** The hit command. */
    public static final String HIT_CMD = "!hit";
    
    /** The stand command. */
    public static final String STAND_CMD = "!stand";
    
    /** Time in minutes for the warning notice to be sent. */
    private static final int WARNING_TIME = 4 * Utils.MS_IN_MIN;
    
    /** Time in minutes for the game to be auto standed. */
    private static final int AUTO_STAND_TIME = 5 * Utils.MS_IN_MIN;
    
    /** Winning x for a push. */
    private static final double PUSH_WIN = 0.7; 
    
    /** Winning x for normal win. */
    private static final double NORMAL_WIN = 2;
    
    /** Winning x for BJ win. */
    private static final double BJ_WIN = 2.5;
    
    /** Max points before bust. */ 
    public static final int MAX_POINTS = 21;
    
    /** Rank -> value conversion. */
    public static final int RANK_TO_VALUE = 2;
    
    /** Ace value in BJ. */
    public static final int ACE_CARD_VALUE = 11;
    
    /** Max value besides ACE in BJ. */
    public static final int MAX_CARD_VALUE = 10;
    
    /** ACE difference (ie Can be 11 or 1, therefore 10 difference. */
    public static final int ACE_DIFFERENCE = 10;
    
    /** The value that the house will stick at. */
    public static final int HOUSE_STICK_VALUE = 17;

    /** String warning of a time out. */
    private static final String TIMEOUT_WARNING = "%b%c04%who%c12: Your open Blackjack game is"
                                                                          + " about to time out!";
    /** String letting user know they have an open game already. */
    private static final String OPENGAME = "%b%c04%who%c12: You already "
                                                   + "have a game open, "
                                                   + "Type %c04"
                                                   + HIT_CMD
                                                   + "%c12 to take another card or %c04"
                                                   + STAND_CMD 
                                                   + "%c12 to stand";

    /** The BJ Command format. */
    private static final String BJ_FORMAT = "%b%c12" + BJ_CMD + " <amount>";

    /** Message when user hasn't got enough chips. */
    private static final String NOCHIPS        = "%b%c12Sorry, you do not have "
           + "%c04%chips%c12 chips available for the %c04%profile%c12 profile.";

    /** String to show hand that has been dealt. */
    private static final String DEALT_HANDS = "%b%c04%who%c12: You have been dealt %c04%phand %c12%b" 
                                                    + "%pscore and the dealer has been dealt "
                                                    +  "%c04%dhand";
    
    /** String informing user of their options. */
    private static final String BJ_OPTIONS = "%b%c04%who%c12: You now "
            + "have 2 options, "
            + "Type %c04"
            + HIT_CMD
            + "%c12 to take another card or %c04"
            + STAND_CMD 
            + "%c12 to stand";
    
    /** Out come string for a game. used with one of the 3 below! */
    private static final String OUTCOME = "%b%c04%who%c12: You %outcome! You had %c04%phand"
            + "%b(%pscore)%c12 and the dealer had %c04%dhand %b(%dscore) ";
    
    /** String to inform the user they lost and hands involved. */
    private static final String PLAYER_LOSE = "lost";
    
    /** String to inform the user they win and hands involved. */
    private static final String PLAYER_WIN = "won";

    /** String to inform the user that it was a draw. */
    private static final String PLAYER_DRAW = "drew!";

    /** String to inform the user they don't have a game open. */
    private static final String NO_OPEN_GAME = "Sorry %who, you don't have an open game";
    
    /** String to inform the user of their current hand. */
    private static final String STATE = "%b%c04%who%c12: You have %c04%hand %c12%pscore";
    
    /** String to notify on winnings / amount returned for a draw. */
    private static final String WINNINGS = "%b%c04%who%c12: You receive %coins coins";

    /** timer that is used to check for idle games. */
    private Timer gameTimer;
    
    
    /** List of open games. */
    private ArrayList<BJBet> openGames;
    
    /**
     * Constructor.
     * @param irc the irc bot so that the timer can send messages
     */
    public BJGame(final IrcBot irc) {
        openGames = new ArrayList<BJBet>();
        
        gameTimer = new Timer(true);
        gameTimer.schedule(new BetTimeoutCheck(irc), Utils.MS_IN_MIN, Utils.MS_IN_MIN);
    }
    
    /* (non-Javadoc)
     * @see org.smokinmils.bot.Event#message(org.smokinmils.bot.events.Message)
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
    
        if (isValidChannel(chan.getName())
                && bot.userIsIdentified(sender)) {
            if (Utils.startsWith(message, BJ_CMD)) {
                newGame(event);
            } else if (Utils.startsWith(message, HIT_CMD)) {
                hit(sender, bot, chan);
            } else if (Utils.startsWith(message, STAND_CMD)) {
                stand(sender, bot, chan);
            }
        }
        
    }
    
    /**
     * This method handles the stand command.
     * @param sender the user who sent the command
     * @param bot the irc bot so we can reply etc
     * @param chan the channel where this is all taking place
     */
    private void stand(final User sender, 
                       final IrcBot bot,
                       final Channel chan) {
        BJBet usergame = null;
        synchronized (BaseBot.getLockObject()) {
            for (BJBet game : openGames) {
                if (game.getUser().getNick().compareTo(sender.getNick()) == 0) {
                    usergame = game;
                    break;
                }
            }
            
            if (usergame != null) {
                dealerPlay(sender, bot, chan, usergame);
            } else {
                String out = NO_OPEN_GAME.replaceAll("%who", sender.getNick());
                bot.sendIRCNotice(sender, out);
            }
            
        }
           
    }
    
    /**
     * This method handles the hit command.
     * @param sender sender who intiated the command
     * @param bot the bot that we can reply with
     * @param chan the channel where this is happening
     */
    private void hit(final User sender, 
                     final IrcBot bot,
                     final Channel chan) {
        // find game, add card, if bust gameover, if 21 send to dealer, else let them carry on
        //String[] msg = message.split(" ");
        BJBet usergame = null;
        synchronized (BaseBot.getLockObject()) {
            for (BJBet game : openGames) {
                if (game.getUser().getNick().compareTo(sender.getNick()) == 0) {
                    usergame = game;
                    break;
                }
            }
            
            if (usergame != null) {
                // this is our game, lets hit
                
                // deal a card
                usergame.dealPlayerCard();
                
                ArrayList<Card> phand = usergame.getPlayerHand();
                int score = countHand(phand);
                String scorelist = allHands(phand).toString();
                
                if (score == MAX_POINTS) {
                    // they have 21, don't let them carry on
                    dealerPlay(sender, bot, chan, usergame);
                } else if (score > MAX_POINTS) {
                    // they lose, BAM!
                    doLose(sender, bot, chan, usergame); 
                } else {
                    // they are < 21, can still play, inform them of such.
                    String out = STATE.replaceAll("%who", sender.getNick());
                    
                    out = out.replaceAll("%hand", handToString(phand, false));
                    out = out.replace("%pscore", scorelist);
                    bot.sendIRCNotice(sender, out);
                    bot.sendIRCMessage(sender, out);
                    out = BJ_OPTIONS.replaceAll("%who", sender.getNick());
                    bot.sendIRCNotice(sender, out);
                }
                
            }  else {
                String out = NO_OPEN_GAME.replaceAll("%who", sender.getNick());
                bot.sendIRCNotice(sender, out);
            }
        }
        
        
    }
     
    /**
     * Player has finished playing, let the dealer play out and finish the game.
     * @param sender sender who has initiated this
     * @param bot the irc bot to reply with
     * @param chan the channel
     * @param usergame the game we are letting the dealer play
     */
    private void dealerPlay(final User sender, 
                            final IrcBot bot,
                            final Channel chan, 
                            final BJBet usergame) {
        
        // dealer keep taking cards until 17 or >
        BJBet game = usergame;
        if (natural(game.getDealerHand())) {
            // instant win, game over since we check for natural push at the start of the game
            doLose(sender, bot, chan, game);
        } else {
            while (countHand(game.getDealerHand()) < HOUSE_STICK_VALUE) {
                game.dealDealerCard();
            }
            // if bust cry and player wins, else compare player and dealer scores
            if (bust(game.getDealerHand())) {
            // player wins    
                doWin(sender, bot, chan, game, NORMAL_WIN);
            } else {
                // no one has busy, winner == highest score
                int pscore = countHand(game.getPlayerHand());
                int dscore = countHand(game.getDealerHand());
                if (pscore > dscore) {
                    doWin(sender, bot, chan, game, NORMAL_WIN);
                } else if (dscore > pscore) {
                    doLose(sender, bot, chan, game);
                } else { // draw
                    doDraw(sender, bot, chan, game);
                }
            }
        }

        
    }

    /**
     * Removes a game from the opengames.
     * @param user the username who is playing the game.
     */
    private void removeGame(final User user) {
        BJBet endgame = null;
        for (BJBet game : openGames) {
            if (game.getUser().getNick().compareTo(user.getNick()) == 0) {
                endgame = game;
                break;
            }
        } //TODO change when we bets auto delete themselves
        if (endgame != null) {
            openGames.remove(endgame);
           
            try {
               endgame.close();
            } catch (SQLException e) {
               EventLog.log(e, "BJGame", "removeGame");
            }
            
        }
        
    }

    /**
     * The game was a draw, refund the player by a set amount.
     * @param player the player we refund
     * @param bot the bot we send messages with
     * @param chan the channel to which we say stuff
     * @param usergame the game we are dealing with
     */
    private void doDraw(final User player, 
                        final IrcBot bot,
                        final Channel chan, 
                        final BJBet usergame) {

        double amount = usergame.getAmount();        
        
        try { 
            usergame.win(amount * PUSH_WIN);
         // remove the game from the list dummy
            removeGame(usergame.getUser());
        
            ArrayList<Card> phand = usergame.getPlayerHand();
            ArrayList<Card> dhand = usergame.getDealerHand();
            
            // Announce winner and give chips
            String out = OUTCOME.replaceAll("%who", player.getNick());
            out = out.replaceAll("%pscore", Integer.toString(countHand(phand)));
            out = out.replaceAll("%dscore", Integer.toString(countHand(dhand)));
            out = out.replaceAll("%outcome", PLAYER_DRAW);
            
            out = out.replaceAll("%phand", handToString(phand, false));
            out = out.replaceAll("%dhand", handToString(dhand, false));
            
            bot.sendIRCMessage(chan, out);
            
            out = WINNINGS.replaceAll("%coins", Double.toString(amount * PUSH_WIN));
            out = out.replaceAll("%who", player.getNick());
            bot.sendIRCNotice(player, out);
            
        } catch (Exception e) {
            EventLog.log(e, "BJGame", "playerDraw");
        }
        
        
    }

    /**
     * The player won, give them winnings!
     * @param winner the person who won
     * @param bot the bot we are using to send messages
     * @param chan the channel to which we send messages
     * @param usergame the game we are winning
     * @param multiplier the multiplier for winning
     */
    private void doWin(final User winner, 
                       final IrcBot bot,
                       final Channel chan, 
                       final BJBet usergame,
                       final double multiplier) {

 

      
        
        ProfileType wprof = usergame.getProfile();
        double amount = usergame.getAmount();
        double win = amount * multiplier;
        
        @SuppressWarnings("unused") // we don't actually use the rake atm
        double rake = Rake.getRake(winner.getNick(), amount, wprof);

        try {
            usergame.win(win);
            // remove the game from the list dummy
            removeGame(usergame.getUser());
            
            ArrayList<Card> phand = usergame.getPlayerHand();
            ArrayList<Card> dhand = usergame.getDealerHand();
            
            // Announce winner and give chips
            String out = OUTCOME.replaceAll("%who", winner.getNick());
            out = out.replaceAll("%pscore", Integer.toString(countHand(usergame.getPlayerHand())));
            out = out.replaceAll("%dscore", Integer.toString(countHand(usergame.getDealerHand())));
            out = out.replaceAll("%outcome", PLAYER_WIN);
            
            out = out.replaceAll("%phand", handToString(phand, false));
            out = out.replaceAll("%dhand", handToString(dhand, false));
            
            bot.sendIRCMessage(chan, out);
           
            out = WINNINGS.replaceAll("%coins", Double.toString(win));
            out = out.replaceAll("%who", winner.getNick());
            bot.sendIRCNotice(winner, out);
            
        } catch (Exception e) {
            EventLog.log(e, "BJGame", "playerWin");
        }

        // jackpot stuff only one person, so no need for losers!
       if (Rake.checkJackpot(amount)) {
            ArrayList<String> players = new ArrayList<String>();
            players.add(winner.getNick());
            Rake.jackpotWon(wprof, GamesType.BLACKJACK, players, bot,
                            chan);
        }
       
       
      
        
    }

    /** Player has lost the game.
     * @param sender the loser
     * @param bot the bot that is sending messages
     * @param chan the channel we are sending messages to
     * @param usergame the game that we are losing :(
     */
    private void doLose(final User sender, 
                        final IrcBot bot,
                        final Channel chan, 
                        final BJBet usergame) {
        // Inform the user that they have lost. simple
        
        // remove the game from the list dummy
        removeGame(usergame.getUser());
        
        ArrayList<Card> phand = usergame.getPlayerHand();
        ArrayList<Card> dhand = usergame.getDealerHand();
        
        String out = OUTCOME.replaceAll("%who", sender.getNick());
        out = out.replaceAll("%pscore", Integer.toString(countHand(usergame.getPlayerHand())));
        out = out.replaceAll("%dscore", Integer.toString(countHand(usergame.getDealerHand())));
        out = out.replaceAll("%outcome", PLAYER_LOSE);
        
        out = out.replaceAll("%phand", handToString(phand, false));
        out = out.replaceAll("%dhand", handToString(dhand, false));
        
        bot.sendIRCMessage(chan, out);
        
        
        // jackpot stuff only one person, so no need for losers!
       if (Rake.checkJackpot(usergame.getAmount())) {
            ArrayList<String> players = new ArrayList<String>();
            players.add(sender.getNick());
            Rake.jackpotWon(usergame.getProfile(), GamesType.BLACKJACK, players, bot,
                            chan);
        } 
       
   
    }

    /**
     * This method handles the deal command.
     * @param event the message event
     */
    private void newGame(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        String[] msg = message.split(" ");
        BJBet game = null;
        if (msg.length == 2) {
            boolean playbet = false;
            boolean hasopen = false;
            ProfileType profile = null;
            Double betsize = 0.0;
            synchronized (BaseBot.getLockObject()) {
                for (BJBet g : openGames) {
                    if (g.getUser().getNick().compareTo(sender.getNick()) == 0) {
                        hasopen = true;
                        break;
                    }
                }
                
                if (hasopen) {
                    String out = OPENGAME.replaceAll("%who",
                            sender.getNick());
                    bot.sendIRCNotice(sender, out);
                } else {
                    Double amount = Utils.tryParseDbl(msg[1]);
                    if (amount == null || amount == 0) {
                        bot.invalidArguments(sender, BJ_FORMAT);
                    } else if (amount > Variables.MAXBET) {
                        bot.maxBet(sender, chan, Variables.MAXBET);
                    } else {
                        DB db = DB.getInstance();
                        try {
                            profile = db.getActiveProfile(sender.getNick());
                            betsize = db.checkCredits(sender.getNick(),
                                    amount);
                            if (amount < 0.0) {
                                bot.invalidArguments(sender, BJ_FORMAT);
                            } else if (betsize > 0.0) {
                                playbet = true;
                                game = new BJBet(sender, betsize, profile, chan);
                                openGames.add(game);
                            } else {
                                String out = NOCHIPS.replaceAll(
                                     "%chips", Utils.chipsToString(betsize));
                                out = out.replaceAll("%profile",
                                                     profile.toString());
                                bot.sendIRCNotice(sender, out);
                            }
                        } catch (Exception e) {
                            EventLog.log(e, "BJGame", "deal");
                        }
                    }
                }
            }
            
            if (playbet) {
                try {
                    // deal game, remove chips, check if auto win (natural)
                    

                    ArrayList<Card> phand = game.getPlayerHand();
                    ArrayList<Card> dhand = game.getDealerHand();
                    
                    //print to channel the hands
                    String out = DEALT_HANDS.replaceAll("%who", sender.getNick());
                    
                    out = out.replaceAll("%phand", handToString(phand, false));
                    out = out.replaceAll("%dhand", handToString(dhand, true)); 
                    
                    out = out.replaceAll("%pscore", allHands(phand).toString());
                    out = out.replaceAll("%dscore", allHands(dhand).toString());
                    
                    bot.sendIRCNotice(sender, out);
                    bot.sendIRCMessage(sender, out);
                    
                    // check for natural win / both 21 so push
                    if (natural(dhand) && natural(phand)) {
                        // both natural, push
                        doDraw(sender, bot, chan, game);
                    } else if (natural(phand)) {
                        // player auto win
                        doWin(sender, bot, chan, game, BJ_WIN);
                    } else {
                        // no 21 (or unknown dealer 21) 
                        out = BJ_OPTIONS.replaceAll("%who", sender.getNick());
                        bot.sendIRCNotice(sender, out);  
                    }
                } catch (Exception e) {
                    EventLog.log(e, "BJGame", "deal/playbet");
                }
            }
        }
        
    }
    
    
    /**
     * Checks to see if a hand is a natural 21.
     * @param hand the hand we want to check
     * @return true if natural, false otherwise
     */
    private boolean natural(final ArrayList<Card> hand) {
        boolean tenCheck = true;
        for (Card card : hand) {
            if (card.getRank() == Card.TEN) {
                tenCheck = false;
            }
        }
        return (hand.size() == BJBet.START_HAND_SIZE && countHand(hand) == MAX_POINTS
                && tenCheck);
    }
    
    /** 
     * Takes a hand to check if they have bust.
     * @param hand The hand we want to check
     * @return true if bust, false otherwise
     */
    private boolean bust(final ArrayList<Card> hand) {
        return (countHand(hand) > MAX_POINTS);
    }
    
    /**
     * @param hand the hand of which we want to discover the best hand
     * @return the best possible score we can get without going bust
     */
    private int countHand(final ArrayList<Card> hand) {
       int besthand = 0;
       for (int score : allHands(hand)) {
           if (score > besthand && score <= MAX_POINTS) {
               besthand = score;
           }
       }
       
       // if we haven't found a "best hand" they must be bust!, fine the least worst hand...
       if (besthand == 0) {
           besthand = allHands(hand).get(0); 
           for (int score : allHands(hand)) {
               if (score < besthand && score > MAX_POINTS) {
                   besthand = score;
               }
           }
       }
       return besthand;
    }

    /** 
     * This method takes a hand and gives a set of possible values.
     * @param hand the hand we need valuing
     * @return the total values of the hand depending on aces
     */
    private ArrayList<Integer> allHands(final ArrayList<Card> hand) {
        ArrayList<Integer> retList = new ArrayList<Integer>();
        int total = 0;
        // track aces so if they go over 21, we can subtract 10 per ace
        int aceCount = 0;
        
        for (Card card : hand) {
            // ranks are -2 from value we are using
            int value = card.getRank() + RANK_TO_VALUE;
            // check if ACE
            if (card.getRank() == Card.ACE) {
               value = ACE_CARD_VALUE; 
               aceCount += 1;
            } else if (value > MAX_CARD_VALUE) {
                value = MAX_CARD_VALUE;
            }
            total += value;
        }
        retList.add(total);
        while (aceCount > 0) {
            total -= ACE_DIFFERENCE;
            aceCount -= 1;
            retList.add(total);
        }
        return retList;
    }
    
    /**
     * Takes a hand and returns a string representation of it.
     * @param hand the hand we want as a string
     * @param obs do we want to obscure the first card?
     * @return the hand as a string
     */
    private String handToString(final ArrayList<Card> hand, 
                                final boolean obs) {
        String out = "";
        for (Card card : hand) {
            if (obs && hand.indexOf(card) == 1) {
                // do thing :/
                out += " ";
            } else {
                out += card.toIRCString();
            }
        }
        return out;
    }
    
    /**
     * A task to announce open bets for this game.
     * 
     * @author Jamie / cjc
     */
    public class BetTimeoutCheck extends TimerTask {
        /** The bot used for announcing. */
        private final IrcBot irc;
        
        /**
         * Constructor.
         * 
         * @param bot The bot to announce with.
         */
        public BetTimeoutCheck(final IrcBot bot) {
            irc = bot;
           

        }

        /**
         * (non-Javadoc).
         * @see java.util.TimerTask#run()
         */
        @Override
        public final void run() {
            
            if (openGames.size() > 0) {
               
                for (BJBet bet : openGames) {
                   // if they are older than TIMEOUT auto stand them,
                    // else if they are older than WARNING message them
                    long diff = System.currentTimeMillis() - bet.getTime();
                    if (diff > AUTO_STAND_TIME) {
                        
                        stand(bet.getUser(), irc, bet.getChannel());
                        
                    } else if (diff > WARNING_TIME) {
                        String out = TIMEOUT_WARNING.replace("%who", bet.getUser().getNick());
                        irc.sendIRCNotice(bet.getUser(), out);
                    }
                }
            }
        }
    }
}
