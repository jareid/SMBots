/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot;

import org.smokinmils.pokerbot.logging.EventLog;
import org.smokinmils.pokerbot.settings.Variables;

/**
 * An IRC Poker bot
 * 
 * @author Jamie Reid
 */
public class GameBot {
	/**
	 * This is the main executable
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Set up debug output
		boolean is_debug = false;
		
        // Now start our bot up.
        Client bot = new Client( Variables.LobbyChan, is_debug );
        
        // Create out log
        EventLog.create("pokerbot", is_debug);
        
        // Restore failed bets
        Database.getInstance().restoreBets();
        
        // Connect to the IRC server.
        try {
        	bot.connect( Variables.Server, Variables.Port );
	    	while(true) {}
        } catch (Exception e) {
        	EventLog.fatal(e, "GameBot", "main"); 
        	System.exit(0);
        } 
	}
}
