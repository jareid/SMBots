/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

import java.security.SecureRandom;

/**
 * The random number generator.
 * 
 * @author Jamie Reid
 */
public final class Random {
	/** The random number generator. */
	private static SecureRandom secureRand;
	
	/**
	 * Hiding the default constructor.
	 */
	private Random() { }

	/**
	 * Returns an integer up to the maximum.
	 * 
	 * @param max maximum value
	 * 
	 * @return The random number
	 */
	public static Integer nextInt(final Integer max) {
		secureRand = new SecureRandom();
		return secureRand.nextInt(max);
	}
	
	/**
     * Returns a double between 0 and 1.
     * 
     * @return the random number
     */
	public static Double nextDouble() {
        return secureRand.nextDouble();
	}
}
