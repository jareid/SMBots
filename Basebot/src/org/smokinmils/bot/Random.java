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
 * The random number generator
 * 
 * @author Jamie Reid
 */
public class Random {
	/** The random number generator */
	private static SecureRandom secureRand;

	/**
	 * Returns an integer up to the maximum
	 * 
	 * @param max maximum value
	 * @return
	 */
	public static Integer nextInt(Integer max) {
		secureRand = new SecureRandom();
		return secureRand.nextInt(max);
	}
}
