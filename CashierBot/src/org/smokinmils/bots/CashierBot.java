/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */ 
package org.smokinmils.bots;

import java.util.Timer;

import org.smokinmils.SMBaseBot;
import org.smokinmils.cashier.*;

/**
 * Starts the Cashier bot with the correct servers and channels
 * 
 * @author Jamie Reid
 */
public class CashierBot {
    public static void main(String[] args) throws Exception {
    	SMBaseBot basebot = SMBaseBot.getInstance();
    	boolean debug = true;
    	basebot.initialise("SMCashier", "smokinmilsdev", "smokinmils", debug);
    	basebot.addServer("SwiftIRC", "irc.swiftirc.net", 6667);
    	
    	//String[] all_swift_chans = {"#smokin_dice", "#sm_hosts", "#sm_overunder", "#sm_roulette"};
    	String[] all_swift_chans = {"#testeroo"};
    	
    	for (String chan: all_swift_chans) {
    		basebot.addChannel("SwiftIRC", chan);
    		
    		Timer bet_timer = new Timer();
        	bet_timer.scheduleAtFixedRate( new BetDetails( basebot.getBot("SwiftIRC"), chan ), 5*60*1000, 20*60*1000);
    	}
    	
    	CheckChips cc_event = new CheckChips();
    	cc_event.addValidChan(all_swift_chans);
    	basebot.addListener("SwiftIRC", cc_event);
    	
    	GiveChips gc_event = new GiveChips();
    	gc_event.addValidChan(all_swift_chans);
    	basebot.addListener("SwiftIRC", gc_event);
    	
    	Payout p_event = new Payout();
    	p_event.addValidChan(all_swift_chans);
    	basebot.addListener("SwiftIRC", p_event);
    	
    	Profile prf_event = new Profile();
    	prf_event.addValidChan(all_swift_chans);
    	basebot.addListener("SwiftIRC", prf_event);
    	
    	Jackpots jp_event = new Jackpots();
    	jp_event.addValidChan(all_swift_chans);
    	basebot.addListener("SwiftIRC", jp_event);
    	
    	TransferChips tc_event = new TransferChips();
    	tc_event.addValidChan(all_swift_chans);
    	basebot.addListener("SwiftIRC", tc_event);
    	
    	while(true) { Thread.sleep(10); }
    }
}
