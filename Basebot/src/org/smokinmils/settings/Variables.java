/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.settings;

/**
 * Database settings used with the database class.
 * 
 * @author Jamie Reid
 */
public final class Variables {
    /**
     * Hiding the default constructor.
     */
    private Variables() {
    }

    /** The Server name of the database. */
    public static final String SERVER  = "199.101.50.187";

    /** The message for maximum bet size. */
    public static final String MAXBETMSG = "%b%c04%sender%c12: The maximum size"
        + " for this bet is %c04%amount%c12 chips! Please place a smaller bet.";

    /** The maximum bet size. */
    public static final int MAXBET = 50000;

    /** The maximum bet size for over under 7. */
    public static final int MAXBET_OU_7 = 20000;

    /** The maximum bet size numbers on roulette. */
    public static final int MAXBET_ROUL_NUM = 5000;

}
