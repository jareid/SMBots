package org.smokinmils.games.casino.cards;


/**
 * Extends the Deck class such that we can have 8 decks in one.
 * @author cjc
 *
 */
public class Shoe extends Deck {

    /** Number of decks we want in the game. */
    private static final int NO_OF_DECKS    = 8;
    
    /** Cards in the deck. */
    private static final int   NO_OF_CARDS   = Card.NO_OF_RANKS
                                                     * Card.NO_OF_SUITS
                                                     * NO_OF_DECKS;

    /** The deck's cards. */
    private final Card[]       cards;

    /**
     * Constructor.
     * 
     * Starts as a full, ordered deck.
     */
    public Shoe() {
        cards = new Card[NO_OF_CARDS];
        int index = 0;
        for (int deck = NO_OF_DECKS - 1; deck >= 0; deck--) {
            for (int suit = Card.NO_OF_SUITS - 1; suit >= 0; suit--) {
                for (int rank = Card.NO_OF_RANKS - 1; rank >= 0; rank--) {
                    cards[index++] = new Card(rank, suit);
                }
            }
        }
    }
}
