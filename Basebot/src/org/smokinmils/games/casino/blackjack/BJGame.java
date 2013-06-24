package org.smokinmils.games.casino.blackjack;

import java.sql.SQLException;
import java.util.ArrayList;

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
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.games.casino.blackjack.game.Game;
import org.smokinmils.games.casino.carddeck.Card;
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

    /** String to show a natural draw / push. */
    private static final String NATURAL_PUSH = "%b%c04%who%c12: you have drawn with the dealer!";

    /** String to show a natural win. */
    private static final String NATURAL_WIN = "%b%c04%who%c12: BLACKJACK! You win!";
    
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


    
    /** List of open games. */
    private ArrayList<Game> openGames;
    
    /**
     * Constructor.
     */
    public BJGame() {
        openGames = new ArrayList<Game>();
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
                deal(event);
            } else if (Utils.startsWith(message, HIT_CMD)) {
                hit(event);
            } else if (Utils.startsWith(message, STAND_CMD)) {
                stand(event);
            }
        }
        
    }
    
    /**
     * This method handles the stand command.
     * @param event the message event
     */
    private void stand(final Message event) {
        // find game, then let the dealer play it out
        User sender = event.getUser();
        IrcBot bot = event.getBot();
        //String[] msg = message.split(" ");
        Game usergame = null;
        synchronized (BaseBot.getLockObject()) {
            for (Game game : openGames) {
                if (game.getUser().compareTo(sender.getNick()) == 0) {
                    usergame = game;
                    break;
                }
            }
            
            if (usergame != null) {
                dealerPlay(event, usergame);
            } else {
                String out = NO_OPEN_GAME.replaceAll("%who", sender.getNick());
                bot.sendIRCNotice(sender, out);
            }
            
        }
           
    }
    
    /**
     * This method handles the hit command.
     * @param event the message event
     */
    private void hit(final Message event) {
        // find game, add card, if bust gameover, if 21 send to dealer, else let them carry on
        IrcBot bot = event.getBot();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        //String[] msg = message.split(" ");
        Game usergame = null;
        synchronized (BaseBot.getLockObject()) {
            for (Game game : openGames) {
                if (game.getUser().compareTo(sender.getNick()) == 0) {
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
                    dealerPlay(event, usergame);
                } else if (score > MAX_POINTS) {
                    // they lose, BAM!
                    doLose(event, usergame);      
                } else {
                    // they are < 21, can still play, inform them of such.
                    String out = STATE.replaceAll("%who", sender.getNick());
                    
                    out = out.replaceAll("%hand", handToString(phand, false));
                    out = out.replace("%pscore", scorelist);
                    bot.sendIRCNotice(sender, out);
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
     * @param event the event that it was started which we can derive game etc
     * @param usergame 
     */
    private void dealerPlay(final Message event, 
                            final Game usergame) {
        // dealer keep taking cards until 17 or >
        Game game = usergame;
        if (natural(game.getDealerHand())) {
            // instant win, game over since we check for natural push at the start of the game
            doLose(event, game);
        } else {
            while (countHand(game.getDealerHand()) < HOUSE_STICK_VALUE) {
                game.dealDealerCard();
            }
            // if bust cry and player wins, else compare player and dealer scores
            if (bust(game.getDealerHand())) {
            // player wins    
                doWin(event, game, NORMAL_WIN);
            } else {
                // no one has busy, winner == highest score
                int pscore = countHand(game.getPlayerHand());
                int dscore = countHand(game.getDealerHand());
                if (pscore > dscore) {
                    doWin(event, game, NORMAL_WIN);
                } else if (dscore > pscore) {
                    doLose(event, game);
                } else { // draw
                    doDraw(event, game);
                }
            }
        }

        
    }

    /**
     * Removes a game from the opengames.
     * @param user the username who is playing the game.
     */
    private void removeGame(final String user) {
        Game endgame = null;
        for (Game game : openGames) {
            if (game.getUser().compareTo(user) == 0) {
                endgame = game;
                break;
            }
        }
        if (endgame != null) {
            openGames.remove(endgame);
            DB db = DB.getInstance();
            try {
                db.deleteBet(user, GamesType.BLACKJACK);
            } catch (SQLException e) {
               EventLog.log(e, "BJGame", "removeGame");
            }
            
        }
        
    }

    /**
     * The game was a draw, refund the player.
     * @param event the event from which we can get channel sender etc from
     * @param usergame the game that was drawn
     */
    private void doDraw(final Message event, 
                        final Game usergame) {
        // draw, refund them the monies
        IrcBot bot = event.getBot();
        User player = event.getUser();
        Channel chan = event.getChannel();
 
        DB db = DB.getInstance();
        double amount = usergame.getAmount();
        ProfileType wprof = usergame.getProfile();
        
        
        try {
            // TODO transaction type for draws? 
            db.adjustChips(
                    player.getNick(), amount * PUSH_WIN, wprof, GamesType.BLACKJACK,
                    TransactionType.CANCEL);

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
        } catch (Exception e) {
            EventLog.log(e, "BJGame", "playerDraw");
        }
        // remove the game from the list dummy
        removeGame(usergame.getUser());
        
    }

    /**
     * The player won, give them winnings!
     * @param event the event from which we can get channel sender etc from
     * @param usergame the game that was won
     */
    private void doWin(final Message event, 
                       final Game usergame,
                       final double multiplier) {
        // winner, inform them and then give them the chips
        IrcBot bot = event.getBot();
        User winner = event.getUser();
        Channel chan = event.getChannel();
 
        DB db = DB.getInstance();
        /* TODO rake?
        // Take the rake and give chips to winner
        double rake = Rake.getRake(winner.getNick(), amount, wprof)
                + Rake.getRake(loser.getNick(), amount, lprof);
        double win = (amount * 2) - rake;
        */
        // remove the game from the list dummy
        removeGame(usergame.getUser());
        
        ProfileType wprof = usergame.getProfile();
        double amount = usergame.getAmount();
        double win = amount * multiplier;
        
        @SuppressWarnings("unused") // we don't actually use the rake atm
        double rake = Rake.getRake(winner.getNick(), amount, wprof);

        try {
            db.adjustChips(
                    winner.getNick(), win, wprof, GamesType.BLACKJACK,
                    TransactionType.WIN);
            
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
     * @param event the event from which we can get channel sender etc from
     * @param usergame the game which was lost
     */
    private void doLose(final Message event, 
                        final Game usergame) {
        // Inform the user that they have lost. simple
        IrcBot bot = event.getBot();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        
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
        // remove the game from the list dummy
        removeGame(usergame.getUser());
   
    }

    /**
     * This method handles the deal command.
     * @param event the message event
     */
    private void deal(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        String[] msg = message.split(" ");
        
        if (msg.length == 2) {
            boolean playbet = false;
            boolean hasopen = false;
            ProfileType profile = null;
            Double betsize = 0.0;
            synchronized (BaseBot.getLockObject()) {
                for (Game game : openGames) {
                    if (game.getUser().compareTo(sender.getNick()) == 0) {
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
                            if (betsize > 0.0) {
                                playbet = true;
                            } else {
                                String out = NOCHIPS.replaceAll(
                                     "%chips", Utils.chipsToString(amount));
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
                DB db = DB.getInstance();
                try {
                    // deal game, remove chips, check if auto win (natural)
                    
                    // deal game
                    Game game = new Game(sender.getNick(), betsize, profile);
                    
                    // remove chips
                    db.adjustChips(sender.getNick(), -betsize, profile,
                            GamesType.BLACKJACK,
                            TransactionType.BET);
                    // TODO instead of choice!?
                    db.addBet(sender.getNick(), "Blackjack Hand?",
                            betsize,
                            profile, GamesType.BLACKJACK);

                    ArrayList<Card> phand = game.getPlayerHand();
                    ArrayList<Card> dhand = game.getDealerHand();
                    
                    //print to channel the hands
                    String out = DEALT_HANDS.replaceAll("%who", sender.getNick());
                    
                    out = out.replaceAll("%phand", handToString(phand, false));
                    out = out.replaceAll("%dhand", handToString(dhand, true)); 
                    
                    out = out.replaceAll("%pscore", allHands(phand).toString());
                    out = out.replaceAll("%dscore", allHands(dhand).toString());
                    
                    bot.sendIRCNotice(sender, out);
                    
                    // check for natural win / both 21 so push
                    if (natural(dhand) && natural(phand)) {
                        // both natural, push
                        doDraw(event, game);
                    } else if (natural(phand)) {
                        // player auto win
                        doWin(event, game, BJ_WIN);
                    } else {
                        // no 21 (or unknown dealer 21) add the game to the openGames
                        openGames.add(game);
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
        return (hand.size() == Game.START_HAND_SIZE && countHand(hand) == MAX_POINTS);
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
}
