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
    /** The trade ID. */
	private int id;
	
	/** The trade user. */
	private String user;

    /** The amount. */
    private double amount;
    
    /** The profile. */
    private ProfileType profile;
    
    /** The amount. */
    private double wantedAmount;
    
    /** The profile. */
    private ProfileType wantedProfile;
	
    /**
     * Constructor.
     * @param num The id number.
     * @param usr The user who set the trade.
     * @param amnt The amount he is trading.
     * @param prof The profile traded.
     * @param wamnt The amount requested.
     * @param wprof The profile requested.
     */
    public Trade(final int num, final String usr,
                 final double amnt, final ProfileType prof,
                 final double wamnt, final ProfileType wprof) {
        setId(num);
        setUser(usr);
        setAmount(amnt);
        setProfile(prof);
        setWantedAmount(wamnt);
        setWantedProfile(wprof);
    }

    /**
     * @return the id
     */
    public final int getId() {
        return id;
    }

    /**
     * @param num the id to set
     */
    private void setId(final int num) {
        this.id = num;
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
        this.user = usr;
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
        this.amount = amnt;
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
        this.profile = prof;
    }

    /**
     * @return the wanted_amount
     */
    public final double getWantedAmount() {
        return wantedAmount;
    }

    /**
     * @param wamount the wanted_amount to set
     */
    private void setWantedAmount(final double wamount) {
        this.wantedAmount = wamount;
    }

    /**
     * @return the wanted_profile
     */
    public final ProfileType getWantedProfile() {
        return wantedProfile;
    }

    /**
     * @param wprofile the wanted_profile to set
     */
    private void setWantedProfile(final ProfileType wprofile) {
        this.wantedProfile = wprofile;
    }
}
