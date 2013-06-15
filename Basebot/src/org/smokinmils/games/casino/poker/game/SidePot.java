/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.game;

import java.util.ArrayList;
import java.util.List;

import org.smokinmils.settings.PokerVars;

/**
 * The poker functionality for side pots.
 * 
 * @author Jamie Reid
 */
public class SidePot implements Comparable<SidePot> {
    /** The list of players entered into this pot. */
    private final List<Player> players;

    /** The size of this pot. */
    private int                pot;

    /** The current bet size for this pot. */
    private int                bet;

    /**
     * Constructor.
     * 
     * @param betsize the size of the bet for this pot
     */
    public SidePot(final int betsize) {
        pot = 0;
        bet = betsize;
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
     * Adds a player to the sidepot and adds them to the possible winners.
     * 
     * @param plyr the person who called.
     */
    public final void call(final Player plyr) {
        players.add(plyr);
        pot += bet;
    }

    /**
     * Adds a bet amount to the pot.
     * 
     * @param amount the amount to add to the pot.
     */
    public final void add(final int amount) {
        pot += amount;
    }

    /**
     * Adds a player who can win the pot.
     * 
     * @param player the person who called.
     */
    public final void addPlayer(final Player player) {
        players.add(player);
    }

    /**
     * Checks if a player is in this pot already.
     * 
     * @param plyr the person who we need to check.
     * @return true if the player exists
     */
    public final boolean hasPlayer(final Player plyr) {
        return players.contains(plyr);
    }

    /**
     * The size of the maximum bet for this pot.
     * 
     * @return the bet size
     */
    public final int getBet() {
        return bet;
    }

    /**
     * Sets the size of the maximum bet for this pot.
     * 
     * @param size the bet size
     */
    public final void setBet(final int size) {
        bet = size;
    }

    /**
     * The size of this side pot.
     * 
     * @return the pot size
     */
    public final int getPot() {
        return pot;
    }

    /**
     * The list of players entered into this side pot.
     * 
     * @return the lsit of players
     */
    public final List<Player> getPlayers() {
        return players;
    }

    /**
     * Takes the rake from the pot size.
     * 
     * @return the amount of rake.
     */
    public final int rake() {
        int rake = PokerVars.MINRAKE;
        int perc = (int) Math.round(pot * PokerVars.RAKEPERCENT);
        if (perc < PokerVars.MINRAKE) {
            rake = PokerVars.MINRAKE;
            /*
             * removed maximum rake
             * 
             * else if (perc > Variables.MaximumRake) rake =
             * Variables.MaximumRake;
             */
        } else {
            rake = perc;
        }
        pot -= rake;
        return rake;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Object#equals(java.lang.Object)
     * @param obj the object to compare against for equality.
     * @return true if the object is equal.
     */
    @Override
    public final boolean equals(final Object obj) {
        boolean ret = false;
        if (obj instanceof SidePot) {
            return ((SidePot) obj).bet == this.bet;
        }
        return ret;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * @param sidepot the object to compare against for equality.
     * @return 0 if the objects are equal, -1 if this object is less than, 1
     *         otherwise.
     */
    @Override
    public final int compareTo(final SidePot sidepot) {
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
     * (non-Javadoc).
     * @see java.lang.Object#toString()
     * @return the string.
     */
    @Override
    public final String toString() {
        return Integer.toString(pot);
    }
}
