/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.games.poker.tasks;

import java.util.TimerTask;

import org.smokinmils.games.poker.enums.EventType;
import org.smokinmils.games.poker.game.rooms.Table;

/**
 * A task that handles a poker table's timers
 * 
 * @author Jamie Reid
 */
public class TableTask extends TimerTask {
	public static final String StartGameTaskName = "StartGame";
	public static final String WaitForPlayersTaskName = "WaitForPlayers";
	public static final String ActionTaskName = "Action";
	public static final String ActionWarningTaskName = "ActionWarning";
	public static final String ShowCardTaskName = "ShowCards";
	
	/**Name of this timer used for when it completes */
	private String taskName;
	
	/** The table this task is associated with */
	private Table table;
	
    /**
     * Constructor.
     * 
     * @param tbl The Table
     * @param tname The task that this is for
     */
	public TableTask(Table tbl, String tname) {
		table = tbl;
		taskName = tname;
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		table.addEvent(taskName, "", "", "", EventType.TIMER);
	}
}
