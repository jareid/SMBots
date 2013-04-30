/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.poker.tasks;

import java.util.TimerTask;

import org.smokinmils.poker.game.rooms.Player;
import org.smokinmils.poker.game.rooms.Table;

/**
 * A task that handles a poker table while the game is in progress
 * 
 * TODO: change to event?
 * 
 * @author Jamie Reid
 */
public class SittingOut extends TimerTask {
	/** The table this task is associated with */
	Table table;
	Player player;
	
    /**
     * Constructor.
     * 
     * @param tbl The Table
     */
	public SittingOut(Table tbl, Player plyr) {
		table = tbl;
		player = plyr;
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		table.playerLeaves(player, true);
	}
}
