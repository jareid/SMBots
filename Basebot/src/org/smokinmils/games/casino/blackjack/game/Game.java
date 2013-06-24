package org.smokinmils.games.casino.blackjack.game;

import java.util.ArrayList;

import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.carddeck.Card;

/**
 * Keeps track of the state of a game of blackjack.
 * Player, player cards, dealer cards
 * @author cjc
 *
 */
public class Game {

    /** Starting hand size. */
    public static final int START_HAND_SIZE = 2;
    
    /** The deck used for this game. */
    private BJDeck deck;
    
    /** The player's Cards. */
    private ArrayList<Card> playerHand;
    
    /** the Dealer's Cards. */
    private ArrayList<Card> dealerHand;
    
    /** the username in string format who is playing this game. */
    private String user;
    
    /** the amount they have bet on this game. */
    private double amount;
    
    /** the profile that they have bet with. */
    private ProfileType profile;
    
    /**
     * Constructor for a Bj hand, gets a new deck and deals the cards to player / dealer.
     * @param username the user who is playing this game
     * @param betamount the amount the user has bet
     * @param prof the profile they have used to bet with
     */
    public Game(final String username,
                final double betamount,
                final ProfileType prof) {
        
        // init stuff
        deck = new BJDeck();
        deck.shuffle();
        
        playerHand = new ArrayList<Card>();
        dealerHand = new ArrayList<Card>();
        
        user = username;
        amount = betamount;
        profile = prof;
        
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
     * Gets the user who is playing this game.
     * @return the username as a string
     */
    public final String getUser() {
        return user;
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
     * Get the amount this game was for.
     * @return the amount
     */
    public final double getAmount() {
        return amount;
    }
    
    /**
     * Get the profile this game was for.
     * @return the profile
     */
    public final ProfileType getProfile() {
        return profile;
    }
 
}
