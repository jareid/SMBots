/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot;

import org.smokinmils.Database;
import org.smokinmils.SMBaseBot;
import org.smokinmils.logging.EventLog;
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
	public static void main(String[] args) throws InterruptedException {
		// Set up debug output
		boolean debug = true;
        
        // Restore failed bets
        try {
			Database.getInstance().restorePokerBets();
		} catch (Exception e) {
			EventLog.fatal(e, "GameBot", "main");
			System.exit(0);
		}
        
    	SMBaseBot basebot = SMBaseBot.getInstance();
    	basebot.initialise(Variables.Nick, Variables.NickServPassword, Variables.Login, debug);
    	String swift_irc = "SwiftIRC";
    	basebot.addServer(swift_irc, "irc.swiftirc.net", 6667);
    	
        Client swift_lobby = new Client( swift_irc, Variables.LobbyChan );
    	basebot.addListener(swift_irc, swift_lobby);
    	
    	while(true) { Thread.sleep(10); }
	}
}
