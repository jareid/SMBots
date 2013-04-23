/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */ 
package org.smokinmils.database;

import java.sql.SQLException;

public class DBException extends SQLException {
	private static final long serialVersionUID = 1L;
	private String Query;
	
	public DBException(String message, String query) {
		super(message);
		Query = query;
	}
	
	public DBException(SQLException ex, String query) {
		super("SQLException: " + ex.getMessage() + 
				", error code: " + ex.getErrorCode());
		Query = query;
	}
	
	public String getQuery() { return Query; }
}
