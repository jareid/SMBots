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
import org.smokinmils.bot.SpamEnforcer;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.cards.Card;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.Variables;
//import org.smokinmils.settings.XMLSettings;

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
    
    /** the double command. */
    private static final String DOUBLE_CMD = "!double";
    
    /** the insurance command. */
    private static final String INSURE_CMD = "!insure";
    
    /** the cards command. */
    private static final String CARDS_CMD = "!cards";
    
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

    /** 10 for checking for doubling. */
    private static final int VALID_10 = 10;

    /** 10 for checking for doubling. */
    private static final int VALID_9 = 9;

    /** 10 for checking for doubling. */
    private static final int VALID_11 = 11;
    
    /** String warning of a time out. */
    private static final String TIMEOUT_WARNING = "%b%c04%who%c12: Your open Blackjack game is"
                                                                          + " about to time out!";
    /** String letting user know they have an open game already. */
    private static final String OPENGAME = "%b%c04%who%c12: You already have a game open, Type %c04"
                                         + HIT_CMD + "%c12 to take another card or %c04" + STAND_CMD
                                         + "%c12 to stand";

    /** The BJ Command format. */
    private static final String BJ_FORMAT = "%b%c12" + BJ_CMD + " <amount>";

    /** Message when user hasn't got enough chips. */
    private static final String NOCHIPS        = "%b%c12Sorry, you do not have "
           + "%c04%chips%c12 chips available for the %c04%profile%c12 profile.";

    /** String to show hand that has been dealt. */
    private static final String DEALT_HANDS = "%b%c04%who%c12: You have been dealt %c04%phand " 
                                            + "%c12%b %pscore and the dealer has been dealt "
                                            +  "%c04%dhand";
    
    /** String informing user of their options. */
    private static final String BJ_OPTIONS = "%b%c04%who%c12: You now "
            + "have the following options, "
            + "Type ";
    
    /** String to let the user know they can hit. */
    private static final String CAN_HIT = "%c04" + HIT_CMD
            + "%c12 to take another card "; 
    
    /** String to let the user know they can stand. */
    private static final String CAN_STAND = "%c04" + STAND_CMD 
            + "%c12 to stand ";
   
    /** String to let the user know they can double. */
    private static final String CAN_DOUBLE = "%c04" + DOUBLE_CMD 
    + " <amount>%c12 to double down your bet for amount (upto your original bet)"
    + " and instantly get only one more card (You must insure before this if applicable) ";
    
    /** String to let the user know they can insure. */
    private static final String CAN_INSURE = "%c04" + INSURE_CMD 
            + " <amount>%c12 to insure against dealer having BlackJack" 
            + " (upto 50% of your original bet)";
    
    /** Out come string for a game. used with one of the 3 below! */
    private static final String OUTCOME = "%b%c04%who%c12: You %outcome! You had %c04%phand"
            + "%b(%pscore)%c12 and the dealer had %c04%dhand %b(%dscore) ";
    
    /** String to inform the user they lost and hands involved. */
    private static final String PLAYER_LOSE = "lost";
    
    /** String to inform the user they win and hands involved. */
    private static final String PLAYER_WIN = "won";

    /** String to inform the user that it was a draw. */
    private static final String PLAYER_DRAW = "drew";

    /** String to inform the user they don't have a game open. */
    private static final String NO_OPEN_GAME = "Sorry %who, you don't have an open game";
    
    /** String to inform the user of their current hand. */
    private static final String STATE = "%b%c04%who%c12: You have %c04%hand %c12%pscore";
    
    /** String to notify on winnings / amount returned for a draw. */
    private static final String WINNINGS = "%b%c04%who%c12: You receive %coins coins";

    /** String for points when they have Blackjack! */
    private static final String BLACKJACK_STRING = "BlackJack!";

    /** String to tell them they can't afford to double.*/
    private static final String DOUBLE_NOT_ENOUGH = "%b%c04%who%c12: Not enough chips to double";

    /** String to tell them they can't double.*/
    private static final String DOUBLE_NOT_VALID = "%b%c04%who%c12: Game not valid for doubleing";

    /** String to tell them they can't insure.*/
    private static final String INSURE_NOT_VALID = "%b%c04%who%c12: You can't insure this game";

    /** String to tell them they don't have the chipcoins to insure.*/
    private static final String INSURE_NOT_ENOUGH = "%b%c04%who%c12: You don't have the chips "
    		                                                + "to insure";

    /** String to tell the user they have taken out insurance! */
    private static final String INSURE_TAKEN = "%b%c04%who%c12: You have taken insurance out" 
            + " against the dealer having BlackJack for %c04%coins%c12 coins!";
    
    /** String letting the user know they have got insurance cash dollar back. */
    private static final String INSURANCE_PAID = "%b%c04%who%c12: Your insurance has paid";
    
  
    /** timer that is used to check for idle games. */
    private final Timer gameTimer;
    
    /** List of open games. */
    private final ArrayList<BJBet> openGames;
   
    /** The fast channel for the game. */
    private static final String FAST_CHAN = "#SM_Express";
    
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
        String[] msg = message.split(" ");
                
        SpamEnforcer se = SpamEnforcer.getInstance();
        
        if (isValidChannel(chan.getName())
                && bot.userIsIdentified(sender)) {
            if (Utils.startsWith(message, BJ_CMD)) {
                if (se.check(event, FAST_CHAN)) { newGame(event); }
            } else if (Utils.startsWith(message, HIT_CMD)) {
               hit(sender, bot, chan);
            } else if (Utils.startsWith(message, STAND_CMD)) {
                stand(sender, bot, chan); 
            } else if (Utils.startsWith(message, DOUBLE_CMD)) {
                doubleDown(sender, bot, chan, msg);
            } else if (Utils.startsWith(message, INSURE_CMD)) {
               insure(sender, bot, chan, msg);
            } else if (Utils.startsWith(message, CARDS_CMD)) {
                cards(sender, bot, chan, msg);
            }
        }
    }
    
    /**
     * This function handles the !cards command.
     * @param sender person checking cards
     * @param bot the bot to reply with
     * @param chan the channel to reply to
     * @param msg the message to get values from
     */
    private void cards(final User sender,
                       final IrcBot bot,
                       final Channel chan,
                       final String[] msg) {
        BJBet usergame = null;
        synchronized (BaseBot.getLockObject()) {
            for (BJBet game : openGames) {
                if (game.getUser().getNick().compareTo(sender.getNick()) == 0) {
                    usergame = game;
                    break;
                }
            }
            
            if (usergame != null) {
                
                ArrayList<Card> phand = usergame.getPlayerHand();
                ArrayList<Card> dhand = usergame.getDealerHand();

                String out = DEALT_HANDS.replaceAll("%who", sender.getNick());
                
                out = out.replaceAll("%phand", handToString(phand, false));
                out = out.replaceAll("%dhand", handToString(dhand, true)); 
                
                out = out.replaceAll("%pscore", allHands(phand).toString());
                out = out.replaceAll("%dscore", allHands(dhand).toString());
                
                bot.sendIRCNotice(sender, out);
                bot.sendIRCMessage(sender, out);
            } else {
                String out = NO_OPEN_GAME.replaceAll("%who", sender.getNick());
                bot.sendIRCNotice(sender, out);
            }
        }
        
    }

    /**
     * This function handles the !insure command.
     * @param sender person insuring
     * @param bot the bot to reply with
     * @param chan the channel to reply to
     * @param msg the message to get values from
     */
    private void insure(final User sender,
                        final IrcBot bot,
                        final Channel chan,
                        final String[] msg) {
        synchronized (BaseBot.getLockObject()) {
            BJBet usergame = null;
            for (BJBet game : openGames) {
                if (game.getUser().getNick().compareTo(sender.getNick()) == 0) {
                    usergame  = game;
                    break;
                }
            }
            
            if (usergame != null) {
                if (!canInsure(usergame.getDealerHand()) || usergame.isInsured()) {
                    String out = INSURE_NOT_VALID.replaceAll("%who", sender.getNick());
                    bot.sendIRCNotice(sender, out);
                } else {
                    double userTotalCoins = 0.0;
                    try {
                        userTotalCoins = DB.getInstance().checkCredits(sender.getNick(), 
                                        usergame.getProfile());
                    } catch (SQLException e) {
                       EventLog.log(e, "BJGame", "insure");
                    }
                    
                    Double amount = Utils.tryParseDbl(msg[1]);
                    if (amount == null || amount == 0 || amount > usergame.getAmount() / 2) {
                        bot.invalidArguments(sender, CAN_INSURE); 
                    } else {
                        if (userTotalCoins >= amount) {
                            usergame.insure(amount);
                            String out = INSURE_TAKEN.replaceAll("%who", sender.getNick());
                            out = out.replaceAll("%coins", Utils.chipsToString(amount));
                            bot.sendIRCNotice(sender, out);
                        
                        } else {
                            String out = INSURE_NOT_ENOUGH.replaceAll("%who", sender.getNick());
                            bot.sendIRCNotice(sender, out);
                        }  
                    }
                }   
            } else {
                String out = NO_OPEN_GAME.replaceAll("%who", sender.getNick());
                bot.sendIRCNotice(sender, out); 
            }
            
        }
        
    }

    /**
     * processes the double command.
     * @param sender the person who initiated the command
     * @param bot the bot to reply with
     * @param chan the channel to reply to
     * @param msg the message to get the amount from
     */
    private void doubleDown(final User sender,
                          final IrcBot bot,
                          final Channel chan,
                          final String[] msg) {
        
        BJBet usergame = null;
        synchronized (BaseBot.getLockObject()) {
            for (BJBet game : openGames) {
                if (game.getUser().getNick().compareTo(sender.getNick()) == 0) {
                    usergame = game;
                    break;
                }
            }
            if (usergame != null) {
                // if the game isn't valid, or is already doubleDowned
                if (!canDouble(usergame.getPlayerHand()) || usergame.isDoubleGame()) {
                    String out = DOUBLE_NOT_VALID.replaceAll("%who", sender.getNick());
                    bot.sendIRCNotice(sender, out);
                } else {
                    Double amount = Utils.tryParseDbl(msg[1]);
                    if (amount == null || amount == 0 || amount > usergame.getAmount()) {
                        bot.invalidArguments(sender, CAN_DOUBLE); 
                    } else {
                        double userTotalCoins = 0.0;
                        try {
                            userTotalCoins = DB.getInstance().checkCredits(sender.getNick(), 
                                                                usergame.getProfile());
                        } catch (SQLException e) {
                            EventLog.log(e, "BJGame", "doubleDown");
                        }
                        if (userTotalCoins >= amount) {
                            usergame.doubleDown(amount);
                            usergame.dealPlayerCard();
                            dealerPlay(sender, bot, chan, usergame);
                        } else {
                            String out = DOUBLE_NOT_ENOUGH.replaceAll("%who", sender.getNick());
                            bot.sendIRCNotice(sender, out);
                        } 
                    }
                }
            } else {
                String out = NO_OPEN_GAME.replaceAll("%who", sender.getNick());
                bot.sendIRCNotice(sender, out); 
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
                    out += CAN_HIT;
                    out += CAN_STAND;
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
       
        while (countHand(game.getDealerHand()) < HOUSE_STICK_VALUE) {
            game.dealDealerCard();
        }
        // if bust cry and player wins, else compare player and dealer scores
        if (bust(game.getDealerHand())) {
        // player wins    
            doWin(sender, bot, chan, game, NORMAL_WIN);
        } else {
            // no one has bust, winner == highest score
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
        } 
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
        double win = 0.0;
        if (usergame.isDoubleGame()) {
            win = (amount + usergame.getDouble()) * PUSH_WIN;
        } else {
            win = amount * PUSH_WIN;
        }
        Rake.getRake(player.getNick(), amount, usergame.getProfile());
        
        try { 
            usergame.win(win);
         // remove the game from the list dummy
            removeGame(usergame.getUser());
               
            ArrayList<Card> phand = usergame.getPlayerHand();
            ArrayList<Card> dhand = usergame.getDealerHand();
            
            // Announce winner and give chips
            String out = OUTCOME.replaceAll("%who", player.getNick());
            
            out = out.replaceAll("%outcome", PLAYER_DRAW);
            
            out = out.replaceAll("%phand", handToString(phand, false));
            out = out.replaceAll("%dhand", handToString(dhand, false));
            
            if (natural(usergame.getPlayerHand())) {
                out = out.replaceAll("%pscore", BLACKJACK_STRING);
            } else {
                out = out.replaceAll("%pscore", Integer.toString(countHand(phand)));
            }
            if (natural(usergame.getDealerHand())) {
                out = out.replaceAll("%dscore", BLACKJACK_STRING);
            } else {
                out = out.replaceAll("%dscore", Integer.toString(countHand(dhand)));

            }
            
            bot.sendIRCMessage(chan, out);
            
            out = WINNINGS.replaceAll("%coins", Utils.chipsToString(win));
            out = out.replaceAll("%who", player.getNick());
            bot.sendIRCNotice(player, out);
            
        } catch (Exception e) {
            EventLog.log(e, "BJGame", "playerDraw");
        }

        // jackpot stuff only one person, so no need for losers!
       if (Rake.checkJackpot(amount)) {
            ArrayList<String> players = new ArrayList<String>();
            players.add(player.getNick());
            Rake.jackpotWon(usergame.getProfile(), GamesType.BLACKJACK, players, bot,
                            chan);
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
        double win = 0.0;
        if (usergame.isDoubleGame()) {
            win = (usergame.getDouble() * multiplier) + (amount * multiplier);
        } else {
            win = amount * multiplier;
        }
        
       
        Rake.getRake(winner.getNick(), amount, wprof);

        try {
            usergame.win(win);
            // remove the game from the list dummy
            removeGame(usergame.getUser());
            
            ArrayList<Card> phand = usergame.getPlayerHand();
            ArrayList<Card> dhand = usergame.getDealerHand();
            
            // Announce winner and give chips
            String out = OUTCOME.replaceAll("%who", winner.getNick());
            
            if (natural(usergame.getPlayerHand())) {
                out = out.replaceAll("%pscore", BLACKJACK_STRING);
            } else {
                out = out.replaceAll("%pscore", Integer.toString(countHand(phand)));
            }
            if (natural(usergame.getDealerHand())) {
                out = out.replaceAll("%dscore", BLACKJACK_STRING);
            } else {
                out = out.replaceAll("%dscore", Integer.toString(countHand(dhand)));

            }
            out = out.replaceAll("%outcome", PLAYER_WIN);
            
            out = out.replaceAll("%phand", handToString(phand, false));
            out = out.replaceAll("%dhand", handToString(dhand, false));
            
            bot.sendIRCMessage(chan, out);
           
            out = WINNINGS.replaceAll("%coins", Utils.chipsToString(win));
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
        
        Rake.getRake(sender.getNick(), usergame.getAmount(), usergame.getProfile());
        
        String out = OUTCOME.replaceAll("%who", sender.getNick());
        
        if (natural(usergame.getPlayerHand())) {
            out = out.replaceAll("%pscore", BLACKJACK_STRING);
        } else {
            out = out.replaceAll("%pscore", Integer.toString(countHand(phand)));
        }
        if (natural(usergame.getDealerHand())) {
            out = out.replaceAll("%dscore", BLACKJACK_STRING);

        } else {
            out = out.replaceAll("%dscore", Integer.toString(countHand(dhand)));
        }
        out = out.replaceAll("%outcome", PLAYER_LOSE);
        
        out = out.replaceAll("%phand", handToString(phand, false));
        out = out.replaceAll("%dhand", handToString(dhand, false));
        
        bot.sendIRCMessage(chan, out);
        
        // if dealer had natural and we lost (either via bj insta loss or going over 21
        if (natural(usergame.getDealerHand())) {
            // check insurance.
            if (usergame.isInsured()) {
                usergame.payInsurance();
                out = INSURANCE_PAID.replaceAll("%who", sender.getNick());
                bot.sendIRCMessage(chan, out);   
            } 
            
        }
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
                            betsize = Utils.checkCredits(sender, amount, bot, chan);
                            if (amount < 0.0) {
                                bot.invalidArguments(sender, BJ_FORMAT);
                            } else if (betsize > 0.0) {
                                playbet = true;
                                game = new BJBet(sender, betsize, profile, chan);
                                openGames.add(game);
                            } else {
                                String out = NOCHIPS.replaceAll("%chips",
                                                                Utils.chipsToString(betsize));
                                out = out.replaceAll("%profile", profile.toString());
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
                    } else  {
                        out = BJ_OPTIONS.replaceAll("%who", sender.getNick());
                        out += CAN_HIT;
                        out += CAN_STAND;
                        if (canInsure(dhand)) {
                            // if the player is allowed to double
                            out += CAN_INSURE;
                        }    
                        if (canDouble(phand)) {
                            out += CAN_DOUBLE;
                        } 
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
        
        return (hand.size() == BJBet.START_HAND_SIZE && countHand(hand) == MAX_POINTS);
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
     * Checks if we can insure on a hand.
     * @param hand the hand we want to check
     * @return true if we offer insurance, false otherwise
     */
    private boolean canInsure(final ArrayList<Card> hand) {
        return (hand.get(0).getRank() == Card.ACE);
    }
    
    /**
     * Checks whether we can double on this hand.
     * @param hand the hand we are checking
     * @return true if valid to double, else false
     */
    private boolean canDouble(final ArrayList<Card> hand) {
        boolean valid = false;
        for (int score : allHands(hand)) {
            if (score == VALID_9 || score == VALID_10 || score == VALID_11) {
                valid = true;
                break;
            }
        }
        return (hand.size() == BJBet.START_HAND_SIZE && valid);
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
        if (obs) {
            out += hand.get(0).toIRCString();
        } else {
            for (Card card : hand) {
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
            try {
                // create a shallow clone of openGames so that we don't run into a
                // ConcurrentModificationException
                ArrayList<BJBet> games = new ArrayList<BJBet>(openGames);
                if (games.size() > 0) {
                    for (BJBet bet : games) {
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
            } catch (Exception e) {
                // if a game is added or removed when this is running?
                EventLog.log(e, "BJGame", "Timer.run");
            }
        }
    }
}
