/**
 * This file is part of a commercial IRC bot that
 * allows users to play online IRC games.
 *
 * The project was commissioned by Julian Clark
 *
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.bot;

import org.pircbotx.User;
import org.smokinmils.database.types.ProfileType;

/**
 * A class to store the details of a bet for any of the games.
 *
 * @author Jamie
 */
public class Bet {
    /** The user who placed this bet. */
	private final User user;
	
	/** The decimal amount for this bet. */
	private final double amount;
	
	/** The choice made on the bet, may be blank, i.e for DD bets. */
	private final String choice;
	
	/** 
	 * Used to activate bets to stop double bets.
	 * @deprecated No longer used as bets are processed in a thread safe manner
	 */
	@Deprecated
    private boolean valid;
	
	/**
	 * The profile used for this bet.
	 * @see ProfileType
	 */
	private final ProfileType profile;
	
	/**
	 * Constructor. 
	 * 
	 * @param usr     The user for the bet.
	 * @param prof  The profile for the bet.
	 * @param amnt   The decimal amount of the bet.
	 * @param chce   The choice of the bet, can be null.
	 */
	public Bet(final User usr,
	           final ProfileType prof,
	           final double amnt,
	           final String chce) {
		this.user = usr;
		this.amount = amnt;
		this.choice = chce;
		this.valid = true;
		this.profile = prof;
	}
	
	/**
	 * Used to disable a bet to ensure bet's are only
	 * processed when they should be (i.e no double calls).
	 * 
	 * @deprecated
	 */
	@Deprecated
    public final void invalidate() {
		this.valid = false;
	}
	
	/**
     * Used to reactivate a bet to ensure bet's are only
     * processed when they should be (i.e no double calls).
     * 
     * @deprecated
     */
	@Deprecated
    public final void reset() {
		this.valid = true;
	}
	
	/**
	 * Returns this bet's user.
	 * 
	 * @return The user.
	 */
	public final User getUser() { return user; }
	
	/**
     * Returns this bet's amount.
     * 
     * @return The user.
     */
	public final double getAmount()	{ return amount; }
	
	/**
	 * Returns this bet's choice.
	 * 
	 * @return The choice.
	 */
	public final String getChoice() { return choice; }
	
	/**
     * Returns this bet's validity.
     * 
     * @return true if the bet is valid, false otherwise.
     * 
     * @deprecated
     */
	@Deprecated
    public final boolean isValid() { return valid; }
	
	/**
     * Returns this bet's profile.
     * 
     * @return The profile.
     */
	public final ProfileType getProfile() { return profile; }
}
