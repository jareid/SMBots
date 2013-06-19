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
public final class DBSettings {
    /**
     * Hiding the default constructor.
     */
    private DBSettings() {
    }

    /** The Server name of the database. */
    public static final String SERVER  = "199.101.50.187";

    /** The port number of the database (MySQL is typically 3306). */
    public static final int    PORT    = 3306;

    /** The database name of the database. */
    public static final String DB_NAME = "live";

    /** The username of the database. */
    public static final String DB_USER = "smbot";

    /** The password of the database. */
    public static final String DB_PASS = "SM_bot_2013$";

}
