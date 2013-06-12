/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.database;

import java.sql.SQLException;

/**
 * An exception that stores the query executed.
 * 
 * @author Jamie
 */
public class DBException extends SQLException {
    /** Serial version. */
    private static final long serialVersionUID = 1L;

    /** The Exception query. */
    private final String      query;

    /**
     * Constructor.
     * @param message The exception message.
     * @param qry The query executed.
     */
    public DBException(final String message, final String qry) {
        super(message);
        query = qry;
    }

    /**
     * Constructor.
     * @param ex The exception.
     * @param qry The query executed.
     */
    public DBException(final SQLException ex, final String qry) {
        super("SQLException: " + ex.getMessage() + ", error code: "
                + ex.getErrorCode());
        query = qry;
    }

    /**
     * @return the query from this exception
     */
    public final String getQuery() {
        return query;
    }
}
