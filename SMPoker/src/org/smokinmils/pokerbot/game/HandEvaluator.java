/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.game;

import org.smokinmils.pokerbot.enums.HandValueType;

/**
 * Calculates the value of a poker hand
 * 
 * @author Jamie Reid
 */
public class HandEvaluator {
    
    /** The number of hand rankings. */
    private static final int NO_OF_RANKINGS  = 6;
    
    /** The maximum number of counting pairs. */
    private static final int MAX_NO_OF_PAIRS = 2;
    
    /** The ranking factors (powers of 13, the number of ranks). */
    private static final int[] RANKING_FACTORS = {371293, 28561, 2197, 169, 13, 1};
    
    /** The hand value type. */
    private HandValueType type;
    
    /** The hand value as integer number. */
    private int value = 0;
    
    /** The cards. */
    private final Card[] cards;
    
    /** The rank distribution (number of cards for each rank). */
    private int[] rankDist = new int[Card.NO_OF_RANKS];
    
    /** The suit distribution (number of cards for each suit). */
    private int[] suitDist = new int[Card.NO_OF_SUITS];
    
    /** The number of pairs. */
    private int noOfPairs = 0;
    
    /** The ranks of the pairs. */
    private int[] pairs = new int[MAX_NO_OF_PAIRS];
    
    /** The suit of the Flush. */
    private int flushSuit = -1;
    
    /** The rank of the Flush. */
    private int flushRank = -1;
    
    /** The rank of the Straight. */
    private int straightRank = -1;
    
    /** Whether we have a Straight with a wheeling Ace. */
    private boolean wheelingAce = false;
    
    /** The rank of the Three-of-a-Kind. */
    private int tripleRank = -1;
    
    /** The rank of the Four-of-a-Kind. */
    private int quadRank = -1;
    
    /** The weighed components of the hand value (highest first). */
    private int[] rankings = new int[NO_OF_RANKINGS];

    /**
     * Constructor.
     *
     * @param hand  The hand to evaulate.
     */
    public HandEvaluator(Hand hand) {
        cards = hand.getCards();
        
        // Find patterns.
        calculateDistributions();
        findStraight();
        findFlush();
        findDuplicates();
        
        // Find special values.
        boolean isSpecialValue =
                (isStraightFlush() ||
                 isFourOfAKind()   ||
                 isFullHouse()     ||
                 isFlush()         ||
                 isStraight()      ||
                 isThreeOfAKind()  ||
                 isTwoPairs()      ||
                 isOnePair());
        
        if (!isSpecialValue) {
            calculateHighCard();
        }
        
        // Calculate value.
        for (int i = 0; i < NO_OF_RANKINGS; i++) {
            value += rankings[i] * RANKING_FACTORS[i];
        }
    }
    
    /**
     * Returns the hand value type.
     *
     * @return  hand value
     */
    public HandValueType getType() {
        return type;
    }
    
    /**
     * Returns the hand value as an integer.
     *
     * @return  the hand value
     */
    public int getValue() {
        return value;
    }
    
    /**
     * Calculates the rank and suit distributions.
     */
    private void calculateDistributions() {
        for (Card card : cards) {
            rankDist[card.getRank()]++;
            suitDist[card.getSuit()]++;
        }
    }
    
    /**
     * Looks for a flush
     */
    private void findFlush() {
        for (int i = 0; i < Card.NO_OF_SUITS; i++) {
            if (suitDist[i] >= 5) {
                flushSuit = i;
                for (Card card : cards) {
                    if (card.getSuit() == flushSuit) {
                        if (!wheelingAce || card.getRank() != Card.ACE) {
                            flushRank = card.getRank();
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * Looks for a Straight
     * 
     * The Ace has the rank of One in case of a Five-high Straight (5-4-3-2-A).
     * The Ace has the rank of Thirteen in case of a Royal Straight (T-J-Q-K-A).
     */
    private void findStraight() {
        boolean inStraight = false;
        int rank = -1;
        int count = 0;
        for (int i = Card.NO_OF_RANKS - 1; i >= 0 ; i--) {
            if (rankDist[i] == 0) {
                inStraight = false;
                count = 0;
            } else {
                if (!inStraight) {
                    // First card of a possible Straight.
                    inStraight = true;
                    rank = i;
                }
                count++;
                if (count >= 5) {
                    // Straight!
                    straightRank = rank;
                    break;
                }
            }
        }
        // Special case for a 'Wheel'
        if ((count == 4) && (rank == Card.FIVE) && (rankDist[Card.ACE] > 0)) {
            wheelingAce = true;
            straightRank = rank;
        }
    }

    /**
     * Finds duplicates (pairs, triples and quads)
     */
    private void findDuplicates() {
        // Find quads, triples and pairs.
        for (int i = Card.NO_OF_RANKS - 1; i >= 0 ; i--) {
            if (rankDist[i] == 4) {
                quadRank = i;
            } else if (rankDist[i] == 3) {
                tripleRank = i;
            } else if (rankDist[i] == 2) {
                if (noOfPairs < MAX_NO_OF_PAIRS) {
                    pairs[noOfPairs++] = i;
                }
            }
        }
    }

    /**
     * Calculates the hand value based on the highest ranks.
     */
    private void calculateHighCard() {
        type = HandValueType.HIGH_CARD;
        rankings[0] = type.getValue();
        // Get the five highest ranks.
        int index = 1;
        for (Card card : cards) {
            rankings[index++] = card.getRank();
            if (index > 5) {
                break;
            }
        }
    }

    /**
     * Returns true if this hand contains One Pair.
     * 
     * Includes kicker ranking
     *
     * @return True if this hand contains One Pair.
     */
    private boolean isOnePair() {
    	boolean ret = false;
        if (noOfPairs == 1) {
            type = HandValueType.ONE_PAIR;
            rankings[0] = type.getValue();
            // Get the rank of the pair.
            int pairRank = pairs[0];
            rankings[1] = pairRank;
            // Get the three kickers.
            int index = 2;
            for (Card card : cards) {
                int rank = card.getRank();
                if (rank != pairRank) {
                    rankings[index++] = rank;
                    if (index > 4) {
                        // We don't need any more kickers.
                        break;
                    }
                }
            }
            ret = true;
        }
        return ret;
    }

    /**
     * Returns true if this hand contains Two Pairs.
     *
     * @return True if this hand contains Two Pairs.
     */
    private boolean isTwoPairs() {
    	boolean ret = false;
        if (noOfPairs == 2) {
            type = HandValueType.TWO_PAIRS;
            rankings[0] = type.getValue();
            // Get the value of the high and low pairs.
            int highRank = pairs[0];
            int lowRank  = pairs[1];
            rankings[1] = highRank;
            rankings[2] = lowRank;
            // Get the kicker card.
            for (Card card : cards) {
                int rank = card.getRank();
                if ((rank != highRank) && (rank != lowRank)) {
                    rankings[3] = rank;
                    break;
                }
            }

            ret = true;
        }
        return ret;
    }

    /**
     * Returns true if this hand contains a Three of a Kind.
     * 
     * Includes kickers
     *
     * @return True if this hand contains a Three of a Kind.
     */
    private boolean isThreeOfAKind() {
    	boolean ret = false;
        if (tripleRank != -1) {
            type = HandValueType.THREE_OF_A_KIND;
            rankings[0] = type.getValue();
            rankings[1] = tripleRank;
            // Get the remaining two cards as kickers.
            int index = 2;
            for (Card card : cards) {
                int rank = card.getRank();
                if (rank != tripleRank) {
                    rankings[index++] = rank;
                    if (index > 3) {
                        // We don't need any more kickers.
                        break;
                    }
                }
            }

            ret = true;
        }
        return ret;
    }

    /**
     * Returns if this hand is a straight
     *
     * @return True if this hand contains a Straight.
     */
    private boolean isStraight() {
    	boolean ret = false;
        if (straightRank != -1) {
            type = HandValueType.STRAIGHT;
            rankings[0] = type.getValue();
            rankings[1] = straightRank;
            ret = true;
        }
        return ret;
    }

    /**
     * Returns true if this hand contains a Flush.
     * 
     * @return True if this hand contains a Flush.
     */
    private boolean isFlush() {
    	boolean ret = false;
        if (flushSuit != -1) {
            type = HandValueType.FLUSH;
            rankings[0] = type.getValue();
            int index = 1;
            for (Card card : cards) {
                if (card.getSuit() == flushSuit) {
                    int rank = card.getRank();
                    if (index == 1) {
                        flushRank = rank;
                    }
                    rankings[index++] = rank;
                    if (index > 5) {
                        // We don't need more kickers.
                        break;
                    }
                }
            }
            ret = true;
        }
        return ret;
    }

    /**
     * Returns true if this hand contains a Full House.
     *
     * @return True if this hand contains a Full House.
     */
    private boolean isFullHouse() {
    	boolean ret = false;
        if ((tripleRank != -1) && (noOfPairs > 0)) {
            type = HandValueType.FULL_HOUSE;
            rankings[0] = type.getValue();
            rankings[1] = tripleRank;
            rankings[2] = pairs[0];
            ret = true;
        }
        return ret;
    }
    
    /**
     * Returns true if this hand contains a Four of a Kind.
     *
     * @return True if this hand contains a Four of a Kind.
     */
    private boolean isFourOfAKind() {
    	boolean ret = false;
        if (quadRank != -1) {
            type = HandValueType.FOUR_OF_A_KIND;
            rankings[0] = type.getValue();
            rankings[1] = quadRank;
            // Get the remaining card as kicker.
            int index = 2;
            for (Card card : cards) {
                int rank = card.getRank();
                if (rank != quadRank) {
                    rankings[index++] = rank;
                    break;
                }
            }

            ret = true;
        }
        return ret;
    }

    /**
     * Returns true if this hand contains a Straight Flush.
     * 
     * @return True if this hand contains a Straight Flush.
     */
    private boolean isStraightFlush() {
    	boolean ret = false;
        if (straightRank != -1 && flushRank == straightRank) {
            // Flush and Straight (possibly separate); check for Straight Flush.
            int straightRank2 = -1;
            int lastSuit = -1;
            int lastRank = -1;
            int inStraight = 1;
            int inFlush = 1;
            for (Card card : cards) {
                int rank = card.getRank();
                int suit = card.getSuit();
                if (lastRank != -1) {
                    int rankDiff = lastRank - rank;
                    if (rankDiff == 1) {
                        // Consecutive rank; possible straight!
                        inStraight++;
                        if (straightRank2 == -1) {
                            straightRank2 = lastRank;
                        }
                        if (suit == lastSuit) {
                            inFlush++;
                        } else {
                            inFlush = 1;
                        }
                        if (inStraight >= 5 && inFlush >= 5) {
                            // Straight!
                            break;
                        }
                    } else if (rankDiff == 0) {
                        // Duplicate rank; skip.
                    } else {
                        // Non-consecutive; reset.
                        straightRank2 = -1;
                        inStraight = 1;
                        inFlush = 1;
                    }
                }
                lastRank = rank;
                lastSuit = suit;
            }
            
            if (inStraight >= 5 && inFlush >= 5) {
                if (straightRank == Card.ACE) {
                    // Royal Flush.
                    type = HandValueType.ROYAL_FLUSH;
                    rankings[0] = type.getValue();
                    ret = true;
                } else {
                    // Straight Flush.
                    type = HandValueType.STRAIGHT_FLUSH;
                    rankings[0] = type.getValue();
                    rankings[1] = straightRank2;
                    ret = true;
                }
            } else if (wheelingAce && inStraight >= 4 && inFlush >= 4) {
                // Wheel
                type = HandValueType.STRAIGHT_FLUSH;
                rankings[0] = type.getValue();
                rankings[1] = straightRank2;
                ret = true;
            }
        }
        return ret;
    }
}