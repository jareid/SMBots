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
public class BetterInfo {
    /** The better username. */
	private String user;

	/** The game. */
	private GamesType game;
	
	/** The amount. */
	private long amount;
	
	/** The position. */
	private int position;
	
	/** The output string. */
	private static final String USERLINE = "%c04%who%c01(%c04%chips%c01)";
	
	/**
	 * Constructor.
	 * 
	 * @param usr the user
	 * @param amnt the amount
	 */
	public BetterInfo(final String usr,
	                  final long amnt) {
		setUser(usr);
		setAmount(amnt);
		setGame(null);
		setPosition(0);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param usr the user
	 * @param gme the game
	 * @param amnt the amount
	 */
	public BetterInfo(final String usr,
	                  final GamesType gme,
	                  final long amnt) {
		setUser(usr);
		setGame(gme);
		setAmount(amnt);
		setPosition(0);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param usr the user
	 * @param gme the game
	 * @param amnt the amount
	 */
	public BetterInfo(final String usr,
	                  final String gme,
	                  final long amnt) {
		setUser(usr);
		setGame(GamesType.fromString(gme));
		setAmount(amnt);
		setPosition(0);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param usr the user
	 * @param posit the user's position
	 * @param amnt the amount
	 */
	public BetterInfo(final String usr,
	                  final int posit,
	                  final long amnt) {
		setUser(usr);
		setGame(null);
		setAmount(amnt);
		setPosition(posit);
	}
	
    /**
     * (non-Javadoc).
     * @see java.lang.Enum#toString()
     * @return the output
     */
	@Override
    public final String toString() {
		String out = USERLINE.replaceAll("%who", getUser());
		out = out.replaceAll("%chips", Long.toString(getAmount()));
		out = out.replaceAll("%position", Integer.toString(getPosition()));
		return out;
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
    public final void setUser(final String usr) {
        user = usr;
    }

    /**
     * @return the position
     */
    public final int getPosition() {
        return position;
    }

    /**
     * @param posit the position to set
     */
    public final void setPosition(final int posit) {
        position = posit;
    }

    /**
     * @return the amount
     */
    public final long getAmount() {
        return amount;
    }

    /**
     * @param amnt the amount to set
     */
    public final void setAmount(final long amnt) {
        amount = amnt;
    }

    /**
     * @return the game
     */
    public final GamesType getGame() {
        return game;
    }

    /**
     * @param gme the game to set
     */
    public final void setGame(final GamesType gme) {
        game = gme;
    }
}
