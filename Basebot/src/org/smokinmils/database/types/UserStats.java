/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.database.types;

/**
* A class used to return values about a transaction for a better.
* 
* @author Jamie Reid
*/
public class UserStats {
    /** The total amount won by this user. */ 
    private final double wintotal;
    
    /** The total number of bets won by this user. */ 
    private final int wincount;
    
    /** The total amount cancelled. */
    private final double canceltotal;
    
    /** The total number of bets cancelled. */
    private final int cancelcount;
    
    /** The total amount made from referral fees. */
    private final double refertotal;
    
    /** The totall amount bet. */
    private final double bettotal;
    
    /** The total number of bets. */
    private final int betcount;

    /**
     * Consturctor.
     * 
     * @param win total won
     * @param winno total wins
     * @param cxl total cancelled
     * @param cxlno total cancels
     * @param refer refer earnings.
     * @param bet total bet
     * @param betno total bets
     */
    public UserStats(final double win,
                    final int winno,
                    final double cxl,
                    final int cxlno,
                    final double refer,
                    final double bet,
                      final int betno) {
        wintotal = win;
        wincount = winno;
        canceltotal = cxl;
        cancelcount = cxlno;
        refertotal = refer;
        bettotal = bet;
        betcount = betno;
    }
    
    /**
     * @return the betcount
     */
    public final int getBetcount() {
        return betcount;
    }

    /**
     * @return the bettotal
     */
    public final double getBettotal() {
        return bettotal;
    }

    /**
     * @return the refertotal
     */
    public final double getRefertotal() {
        return refertotal;
    }
    /**
     * @return the cancelcount
     */
    public final int getCancelcount() {
        return cancelcount;
    }

    /**
     * @return the canceltotal
     */
    public final double getCanceltotal() {
        return canceltotal;
    }

    /**
     * @return the wincount
     */
    public final int getWincount() {
        return wincount;
    }

    /**
     * @return the wintotal
     */
    public final double getWintotal() {
        return wintotal;
    }
}
