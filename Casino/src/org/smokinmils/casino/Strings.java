package org.smokinmils.casino;
/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid + Carl Clegg
 */ 

/**
 * Strings used throughout the poker bot
 * 
 * All string specified in this file are allowed to use the following variable:
 * 
 * %c - Adds the IRC colour character, should be followed with # or #,#
 * %b - Adds the IRC bold character
 * %u - Adds the IRC underline character
 * %i - Adds the IRC italic character
 * %n - Removes all the IRC formatting specified prior to this
 * 
 * Each string may also use it's own specific variables
 * 
 * @author Jamie Reid
 */
public final class Strings {
	
	public static final String JackpotWonOverUnder = "%b%c12The %c04%profile%c12 jackpot of %c04%chips%c12 chips has been won in OverUnder! " +
											"Congratulations to the winner(s):%c04 %winners %c12who have shared the jackpot";
	
	public static final String JackpotWonDiceDuel = "%b%c12The %c04%profile%c12 jackpot of %c04%chips%c12 chips has been won in a Dice Duel! " +
			"Congratulations to the winner(s):%c04 %winners %c12who have shared the jackpot";
	
	public static final String JackpotWonRoulette = "%b%c12The %c04%profile%c12 jackpot of %c04%chips%c12 chips has been won in Roulette! " +
			"Congratulations to the winner(s):%c04 %winners %c12who have shared the jackpot";
}
