/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.poker;

/**
 * A utility class that provides useful functions to the entire project
 * 
 * @author Jamie Reid
 */
public class Utils {
	
	/*
	 * A method that will handle parsing of integers without throwing an exception
	 * 
	 * @param text The input string
	 * 
	 * @return The resulting integer
	 */
	public static Integer tryParse(String text) {
		try {
			return new Integer(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
