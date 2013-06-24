package org.smokinmils.games.casino.blackjack;

import java.sql.SQLException;
import java.util.ArrayList;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Bet;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.cards.Card;
import org.smokinmils.games.casino.cards.Shoe;

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
 
}
