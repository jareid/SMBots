/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.tasks;

import java.util.TimerTask;

import org.smokinmils.games.casino.poker.game.Player;
import org.smokinmils.games.casino.poker.game.rooms.Table;

/**
 * A task that handles a poker table while the game is in progress.
 * 
 * @author Jamie Reid
 */
public class SittingOut extends TimerTask {
    /** The table this task is associated with. */
    private Table  table;

    /** The player sitting out. */
    private Player player;

    /**
     * Constructor.
     * 
     * @param tbl The Table
     * @param plyr The player
     */
    public SittingOut(final Table tbl, final Player plyr) {
        setTable(tbl);
        setPlayer(plyr);
    }

    /**
     * (non-Javadoc).
     * @see java.util.TimerTask#run()
     */
    @Override
    public final void run() {
        getTable().playerLeaves(getPlayer(), true);
    }

    /**
     * @return the player
     */
    public final Player getPlayer() {
        return player;
    }

    /**
     * @param plyr the player to set
     */
    private void setPlayer(final Player plyr) {
        player = plyr;
    }

    /**
     * @return the table
     */
    public final Table getTable() {
        return table;
    }

    /**
     * @param tble the table to set
     */
    private void setTable(final Table tble) {
        table = tble;
    }
}
