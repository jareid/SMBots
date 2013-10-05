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
* A class used to return values about a transaction for a referral user.
* 
* @author Jamie Reid
*/
public class Trade {	
	/** The trade rank. */
	private String rank;
	
    /** The trade user. */
    private String user;

    /** The amount. */
    private double amount;
    
    /** The profile. */
    private ProfileType profile;
	
    /**
     * Constructor.
     * 
     * @param usr The user of the trade
     * @param rnk The rank of the trade
     * @param prof The profile of the trade
     * @param amnt The trade amount
     */
    public Trade(final String usr, final String rnk, final ProfileType prof,
                 final double amnt) {
        setUser(usr);
        setRank(rnk);
        setAmount(amnt);
        setProfile(prof);
    }

    /**
     * @return the rank
     */
    public final String getRank() {
        return rank;
    }

    /**
     * @param rnk the rank to set
     */
    private void setRank(final String rnk) {
        rank = rnk;
    }

    /**
     * @return the user
     */
    public final String getUser() {
        return user;
    }

    /**
     * @param usr the user to set
     */
    private void setUser(final String usr) {
        user = usr;
    }

    /**
     * @return the amount
     */
    public final double getAmount() {
        return amount;
    }

    /**
     * @param amnt the amount to set
     */
    private void setAmount(final double amnt) {
        amount = amnt;
    }

    /**
     * @return the profile
     */
    public final ProfileType getProfile() {
        return profile;
    }

    /**
     * @param prof the profile to set
     */
    private void setProfile(final ProfileType prof) {
        profile = prof;
    }
}
