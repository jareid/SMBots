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

import org.smokinmils.games.casino.poker.enums.EventType;
import org.smokinmils.games.casino.poker.game.rooms.Table;

/**
 * A task that handles a poker table's timers.
 * 
 * @author Jamie Reid
 */
public class TableTask extends TimerTask {
    /** Task name for game start announce. */
    public static final String STARTGAME      = "StartGame";

    /** Task name for waiting for players announce. */
    public static final String WAITFORPLAYERS = "WaitForPlayers";

    /** Task name for action timer. */
    public static final String ACTION         = "Action";

    /** Task name for action warning timer. */
    public static final String ACTIONWARNING  = "ActionWarning";

    /** Task name for shpw card timers. */
    public static final String SHOWCARDS       = "ShowCards";

    /** Name of this timer used for when it completes. */
    private final String       taskName;

    /** The table this task is associated with. */
    private final Table        table;

    /**
     * Constructor.
     * 
     * @param tbl The Table
     * @param tname The task that this is for
     */
    public TableTask(final Table tbl, final String tname) {
        table = tbl;
        taskName = tname;
    }

    /**
     * (non-Javadoc).
     * @see java.util.TimerTask#run()
     */
    @Override
    public final void run() {
        table.addEvent(taskName, EventType.TIMER);
    }
}
