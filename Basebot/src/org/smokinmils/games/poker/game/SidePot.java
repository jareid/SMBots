/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.games.poker.game;

import java.util.ArrayList;
import java.util.List;

import org.smokinmils.settings.poker.Variables;

/**
 * The poker functionality for sidepots
 * 
 * @author Jamie Reid
 */
public class SidePot implements Comparable<SidePot> {
	/** The list of players entered into this pot */
	private List<Player> players;
	
	/** The size of this pot */
	private int pot;
	
	/** The current bet size for this pot */
	private int bet;
	
    /**
     * Constructor.
     * 
     * @param bet the size of the bet for this pot
     */
	public SidePot(int bet) {
		pot = 0;
		this.bet = bet;
		players = new ArrayList<Player>();
	}
	
    /**
     * Constructor.
     */
	public SidePot() {
		pot = 0;
		this.bet = 0;
		players = new ArrayList<Player>();
	}
	
	/**
	 * Adds a player to the sidepot and adds them to the possible winners
	 */
	public void call(Player player) {
		players.add(player);
		pot += bet;		
	}
	
	/**
	 * Adds a bet amount to the pot
	 */
	public void add(int amount) {
		pot += amount;
	}
	
	/**
	 * Adds a player who can win the pot
	 */
	public void addPlayer(Player player) {
		players.add(player);
	}
	
	/**
	 * Checks if a player is in this pot already
	 */
	public boolean hasPlayer(Player player) {
		return players.contains(player);
	}
	
	/**
	 * The size of the maximum bet for this pot
	 * @return the bet size
	 */
	public int getBet() { return bet; }
	
	/**
	 * Sets the size of the maximum bet for this pot
	 * @param the bet size
	 */
	public void setBet(int bet) { this.bet = bet; }
	
	/**
	 * The size of this side pot
	 * @return the pot size
	 */
	public int getPot() { return pot; }
	
	/**
	 * The list of players entered into this side pot
	 * @return the lsit of players
	 */
	public List<Player> getPlayers() { return players; }	
	
	/**
	 * Takes the rake from the pot size
	 */
	public int rake() {
		int rake = Variables.MinimumRake;
		int perc = (int)Math.round(pot * (Variables.RakePercentage / 100.0));
		if (perc < Variables.MinimumRake)
			rake = Variables.MinimumRake;
		else if (perc > Variables.MaximumRake)
			rake = Variables.MaximumRake;
		else
			rake = perc;
		pot -= rake;
		return rake;
	}
	
    /**
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
    	boolean ret = false;
        if (obj instanceof SidePot) {
            return ((SidePot) obj).bet == this.bet;
        }
    	return ret;
    }

    /**
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(SidePot sidepot) {
        int value = bet;
        int other = sidepot.bet;
        
        int ret = 0;
        if (value < other) {
            ret = -1;
        } else if (value > other) {
            ret = 1;
        }
        
        return ret;
    }
    
    /**
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
	public String toString() {
		return Integer.toString(pot);
	}
}
