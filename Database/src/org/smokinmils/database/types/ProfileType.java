/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.database.types;

import java.sql.SQLException;

import org.smokinmils.Database;
import org.smokinmils.database.DBException;

/**
* An enumerate for the DB's credits transaction types
* 
* @author Jamie Reid
*/
public enum ProfileType {
	_07("07"),
	EOC("eoc"),
	PLAY("play"),
	;
	
	/** The text. */
	private final String text;
	
	/**
	 * Constructor.
	 * @param text  textual representation.
	 */
	ProfileType(String text) {
		this.text = text;
	}
	
	/**
	 * Returns a long textual form of this action.
	 * 
	 * @return The textual representation.
	 */
	public String getText() { return text; }
	
	/**
	 * (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() { return text;	}
	
	/** 
	 * Converts a String to the correct ProfileType
	 * 
	 * @param text the string to check
	 * @return the correct enumerate object
	 */
    public static ProfileType fromString(String text) {
        if (text != null) {
        	text = text.toLowerCase();
        	for (ProfileType pt : ProfileType.values()) {
        		if ( pt.getText().compareTo(text) == 0 ) {
        			return pt;
        		}
        	}
        }
        return null;
    }
    
	/**
	 * Converts the type to it's integer from the database
	 * 
	 * @return the integer that represents this ProfileType
	 */
	public int toInteger() throws DBException, SQLException {
		return Database.getInstance().getProfileID( this );
	}
}
