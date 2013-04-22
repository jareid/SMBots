/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

/**
 * Provides the functionality to automatically verify users.
 * 
 * @author Jamie
 */
public class CheckIdentified extends Event {
	/** A string to store the response from a nickserv status command */
	private static final String NickServStatus = "STATUS";
	
	// TODO, onJoins
	// TODO, onParts
	// TODO, onMessage
	// TODO, everything a user could do should check if they are identify and if not, do it.
}
