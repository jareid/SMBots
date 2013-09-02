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
public class EscrowResult {
    /** The amount. */
	private double amount;

    /** The profile. */
    private ProfileType profile;
    
    /**
     * Constructor.
     * @param amnt The amount
     * @param prof The profile
     */
	public EscrowResult(final double amnt, final ProfileType prof) {
	    setAmount(amnt);
	    setProfile(prof);
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
    public final void setProfile(final ProfileType prof) {
        profile = prof;
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
    public final void setAmount(final double amnt) {
        amount = amnt;
    }
}
