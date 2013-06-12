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
* An enumerate for the type of referrer.
* 
* @author Jamie Reid
*/
public enum ReferrerType {
    /** No referrer. */
	NONE,
    /** public referrer. */
	PUBLIC,
    /** Rank referrer. */
	GROUP;
}
