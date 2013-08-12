package org.smokinmils.games.casino.blackjack;

import java.sql.SQLException;
import java.util.ArrayList;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Bet;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.games.casino.cards.Card;
import org.smokinmils.games.casino.cards.Shoe;
import org.smokinmils.logging.EventLog;

/**
 * Keeps track of the state of a game of blackjack.
 * Player, player cards, dealer cards
 * @author cjc
 *
 */
/**
 * @author cjc
 *
 */
public class BJBet extends Bet {

    /** Starting hand size. */
    public static final int START_HAND_SIZE = 2;
    
    /** The deck used for this game. */
    private Shoe deck;
    
    /** The channel we are playing in. */
    private Channel channel;
    
    /** The player's Cards. */
    private ArrayList<Card> playerHand;
    
    /** the Dealer's Cards. */
    private ArrayList<Card> dealerHand;
    
    /** is this a double game? */
    private boolean doubled = false;
    
    /** is this game insured? */
    private boolean insured = false;
    
    /** the amount that we have double downeded. */
    private double doubleAmount = 0.0;
    
    /** the amount that we have insured for. */
    private double insureAmount = 0.0;
    
    /**
     * Constructor for a Bj hand, gets a new deck and deals the cards to player / dealer.
     * @param user the user who is playing this game
     * @param betamount the amount the user has bet
     * @param prof the profile they have used to bet with
     * @param chan the channel....
     * @throws SQLException 
     */
    public BJBet(final User user,
                final double betamount,
                final ProfileType prof,
                final Channel chan) throws SQLException {
        super(user, prof, GamesType.BLACKJACK, betamount, null);
        
        channel = chan;
        // init stuff
        deck = new Shoe();
        deck.shuffle();
        
        playerHand = new ArrayList<Card>();
        dealerHand = new ArrayList<Card>();
        
        // deal the cards
        while (playerHand.size() < START_HAND_SIZE) {
            playerHand.add(deck.deal());
            dealerHand.add(deck.deal());
        }
    }
    
    /**
     * Deals a card into the players hand.
     */
    public final void dealPlayerCard() {
        playerHand.add(deck.deal());
    }
    
    /**
     * Deals a card into the dealers hand.
     */
    public final void dealDealerCard() {
        dealerHand.add(deck.deal());
    }
    
    /**
     * Gets the players cards.
     * @return the players cards...
     */
    public final ArrayList<Card> getPlayerHand() {
        return playerHand;
    }
    
    /**
     * Gets the dealers cards.
     * @return the dealers cards...
     */
    public final ArrayList<Card> getDealerHand() {
        return dealerHand;
    }
    
    /**
     * Gets the channel the game is being played in for the sake of ease with timers.
     * @return the channel object
     */
    public final Channel getChannel() { return channel; };
    
    /**
     * Perform a double down. Assumes user has the coinchips
     * @param amount the amount we are double downing with
     */
    public final void doubleDown(final double amount) {
        DB db = DB.getInstance();
        try {
            //manually adjust since this isn't standard.
            doubled = true;
            doubleAmount = amount;
            db.adjustChips(getUser().getNick(), -doubleAmount, 
                    getProfile(), GamesType.BLACKJACK, TransactionType.BET);
        } catch (SQLException e) {
            
            EventLog.log(e, "BJBet", "doubleDown");
        }
    }

    /**
     * Checks if this is a game that has been doubled or not.
     * @return true if it is else, false.
     */
    public final boolean isDoubleGame() {
        return doubled;
    }

    /**
     * insures a game Assumes user has chips and the amount is valid.
     * TODO make this boolean for integration.
     * @param amount the amount we are insuring for
     */
    public final void insure(final double amount) {
        DB db = DB.getInstance();
        insured = true;
        insureAmount = amount;
        try {
            //manually adjust since this isn't standard.
            db.adjustChips(getUser().getNick(), -amount, 
                    getProfile(), GamesType.BLACKJACK, TransactionType.BJ_INSURE);
        } catch (SQLException e) { 
            EventLog.log(e, "BJBet", "insure");
        }
        
    }
    
    /**
     * checks if the game has been insured against dealer blackjack!
     * @return true if insured, false otherwise
     */
    public final boolean isInsured() {
        return insured;
    }

    /**
     * Pays out insurance!
     */
    public final void payInsurance() {
        if (insured) {
            DB db = DB.getInstance();
           
            try {
                //manually adjust since this isn't standard.
                db.adjustChips(getUser().getNick(), insureAmount * 2, 
                        getProfile(), GamesType.BLACKJACK, TransactionType.WIN);
            } catch (SQLException e) {
                
                EventLog.log(e, "BJBet", "insure");
            } 
        }
        
    }
    
    /**
     * Gets the amount double downed with.
     * @return the amount we double downed for
     */
    public final double getDouble() {
        return doubleAmount;
    }
    
    /**
     * Get's the amount we insured for.
     * @return the amount insured for
     */
    public final double getInsure() {
        return insureAmount;
    }

 
}
