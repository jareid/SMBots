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
* An enumerate for the DB's credits transaction types.
* 
* @author Jamie Reid
*/
public enum ProfileType {
    /** 07 profile. */
	_07("07", true),
    /** eoc profile. */
	EOC("eoc", true),
    /** play profile. */
	PLAY("play", false);
	
	/** The text. */
	private final String text;
	
	/** Value that stores whether this has competitions. */
	private final boolean comps;
	
	/**
	 * Constructor.
	 * @param txt  textual representation.
     * @param hascomps  if this profile has competitions
	 */
	ProfileType(final String txt, final boolean hascomps) {
		text = txt;
		comps = hascomps;
	}
	
	/**
	 * Returns a long textual form of this action.
	 * 
	 * @return The textual representation.
	 */
	public String getText() { return text; }
	
	/**
     * @return true if this profile has competitions
     */
    public boolean hasComps() { return comps; }
	
	/**
	 * (non-Javadoc).
	 * @see java.lang.Enum#toString()
	 * @return the output
	 */
	@Override
	public String toString() { return text;	}
	
	/** 
	 * Converts a String to the correct ProfileType.
	 * 
	 * @param text the string to check
	 * @return the correct enumerate object
	 */
    public static ProfileType fromString(final String text) {
        if (text != null) {
        	String txtlc = text.toLowerCase();
        	for (ProfileType pt : ProfileType.values()) {
        		if (pt.getText().equals(txtlc)) {
        			return pt;
        		}
        	}
        }
        return null;
    }
}
