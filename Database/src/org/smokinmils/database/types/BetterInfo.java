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
* A class used to return values about a transaction for a better
* 
* @author Jamie Reid
*/
public class BetterInfo {
	public String User;

	public GamesType Game;
	
	public long Amount;
	
	public int Position;
	
	public BetterInfo(String user, long amount) {
		User = user;
		Amount = amount;
		Game = null;
		Position = 0;
	}
	
	public BetterInfo(String user, GamesType game, long amount) {
		User = user;
		Game = game;
		Amount = amount;
		Position = 0;
	}
	
	public BetterInfo(String user, String game, long amount) {
		User = user;
		Game = GamesType.fromString(game);
		Amount = amount;
		Position = 0;
	}
	
	public BetterInfo(String user, int position, long amount) {
		User = user;
		Game = null;
		Amount = amount;
		Position = position;
	}
}
