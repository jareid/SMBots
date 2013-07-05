/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.settings;

import org.smokinmils.bot.XMLLoader;

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
    public static final String SERVER  = XMLLoader.getInstance().getDBSetting("server");

    /** The port number of the database (MySQL is typically 3306). */
    public static final int    PORT    = Integer.parseInt(XMLLoader.getInstance()
                                                            .getDBSetting("port"));

    /** The database name of the database. */
    public static final String DB_NAME = XMLLoader.getInstance().getDBSetting("dbname");

    /** The username of the database. */
    public static final String DB_USER = XMLLoader.getInstance().getDBSetting("dbuser");

    /** The password of the database. */
    public static final String DB_PASS = XMLLoader.getInstance().getDBSetting("dbpass");
    
 // forum / smforum / SM_f0ru^^_2013$
}
