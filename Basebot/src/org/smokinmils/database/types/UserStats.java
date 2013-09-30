/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.database.types;

import java.util.HashMap;

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
    
    /** The map of all stats. */
    private final HashMap<ProfileType, HashMap<TransactionType, Double>> stats;
    
    /** The map of all counts. */
    private final HashMap<ProfileType, HashMap<TransactionType, Integer>> counts;

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
        
        stats = new HashMap<ProfileType, HashMap<TransactionType, Double>>();
        counts = new HashMap<ProfileType, HashMap<TransactionType, Integer>>();
    }
    
    /**
     * Constructor.
     */
    public UserStats() {
        wintotal = 0.0;
        wincount = 0;
        canceltotal = 0.0;
        cancelcount = 0;
        refertotal = 0.0;
        bettotal = 0.0;
        betcount = 0;
        
        stats = new HashMap<ProfileType, HashMap<TransactionType, Double>>();
        counts = new HashMap<ProfileType, HashMap<TransactionType, Integer>>();
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

    /**
     * Retrieves a stat value.
     * 
     * @param prof The profile we want stats for
     * @param tzx  The transaction type we want stats for.
     * 
     * @return the value of the stats
     */
    public final double getStat(final ProfileType prof, final TransactionType tzx) {
        HashMap<TransactionType, Double> profstats = stats.get(prof);
        double ret = 0.0;
        if (profstats != null) {
            Double statval = profstats.get(tzx);
            if (statval != null) {
                ret = statval;
            }
        }
        
        return ret;
    }
    
    /**
     * Sets a specifc stat for a specifc profile.
     * 
     * @param prof  The profile to set the stat for
     * @param tzx   The type of stat
     * @param value The actual stat.
     * @param count The number of times used.
     */
    public final void setStat(final ProfileType prof, final TransactionType tzx,
                              final double value, final int count) {
        HashMap<TransactionType, Double> profstats = stats.get(prof);
        if (profstats == null) {
            profstats = new HashMap<TransactionType, Double>();
        }
        profstats.put(tzx, value);
        stats.put(prof, profstats);
        
        HashMap<TransactionType, Integer> profcounts = counts.get(prof);
        if (profcounts == null) {
            profcounts = new HashMap<TransactionType, Integer>();
        }
        profcounts.put(tzx, count);
        counts.put(prof, profcounts);
    }
    
}
