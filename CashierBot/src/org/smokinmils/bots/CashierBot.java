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

import org.smokingmils.help.Help;
import org.smokinmils.SMBaseBot;
import org.smokinmils.cashier.*;
import org.smokinmils.casino.Casino;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.rockpaperscissors.RPSGame;
import org.smokinmils.timedrollcomp.CreateTimedRoll;
import org.smokinmils.timedrollcomp.TimedRollComp;

/**
 * Starts the Cashier bot with the correct servers and channels
 * 
 * @author Jamie Reid
 */
public class CashierBot {
    public static void main(String[] args) throws Exception {
    	SMBaseBot basebot = SMBaseBot.getInstance();
    	boolean debug = true;
    	basebot.initialise("SM_BOT", "5w807", "smokinmils", debug);
    	String swift_irc = "SwiftIRC";
    	basebot.addServer(swift_irc, "irc.SwiftIRC.net", 6667);
    	
    	String[] all_swift_chans = {"#smokin_dice", "#sm_tournaments", "#sm_overunder",
    								"#sm_roulette", "#sm_hosts", "#managers"};
    	//String[] all_swift_chans = {"#testeroo"};
    	
    	for (String chan: all_swift_chans) {
    		basebot.addChannel(swift_irc, chan);
    	}
    	
    	Casino casino = new Casino(basebot.getBot(swift_irc));
    	casino.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, casino);
    	
    	CheckChips cc_event = new CheckChips();
    	cc_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, cc_event);
    	
    	CompPosition cp_event = new CompPosition();
    	cp_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, cp_event);
    	
    	RPSGame rps_event = new RPSGame("#smokin_dice");
    	rps_event.addValidChan(all_swift_chans);
    	rps_event.addAnnounce("#smokin_dice", basebot.getBot(swift_irc));
    	basebot.addListener(swift_irc, rps_event); 	
    	
    	GiveChips gc_event = new GiveChips();
    	gc_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, gc_event); 	

    	Help h_event = new Help();
    	h_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, h_event);
    	
    	Jackpots jp_event = new Jackpots();
    	jp_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, jp_event);
    	
    	Lottery lottery_event = new Lottery();
    	lottery_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, lottery_event);
    	
    	ManagerSystem ms_event = new ManagerSystem("#smokin_dice", "#managers",
    												basebot.getBot(swift_irc));
    	ms_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, ms_event);
    	
    	Payout p_event = new Payout();
    	p_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, p_event);
    	
    	Profile prf_event = new Profile();
    	prf_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, prf_event);
    	
    	Profiles prfs_event = new Profiles();
    	prfs_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, prfs_event);
    	
    	TransferChips tc_event = new TransferChips();
    	tc_event.addValidChan(all_swift_chans);
    	basebot.addListener(swift_irc, tc_event);
    	
    	CreateTimedRoll ctr_event = new CreateTimedRoll();
    	ctr_event.addValidChan("#sm_hosts");
    	basebot.addListener(swift_irc, ctr_event);  
    	
    	// add timed roll for Smoking_Dice every 24hours with a 100chip prize
    	@SuppressWarnings("unused")
		TimedRollComp trc_event = new TimedRollComp(basebot.getBot(swift_irc),
    												"#smokin_dice", ProfileType.EOC,
    												100, 60*24);
    	
    	ManagerAnnounce mgr_ano = new ManagerAnnounce( basebot.getBot(swift_irc), "#smokin_dice" );
    	mgr_ano.begin(0);
    	
    	Timer bet_timer = new Timer(true);
    	bet_timer.scheduleAtFixedRate( new BetDetails( basebot.getBot(swift_irc), "#smokin_dice" ), 5*60*1000, 60*60*1000);
    	
    	Timer comp_timer = new Timer(true);
    	comp_timer.scheduleAtFixedRate( new Competition( basebot.getBot(swift_irc), "#smokin_dice" ), 60*1000, 60*1000);
    	
    	Timer jkpt_timer = new Timer(true);
    	jkpt_timer.scheduleAtFixedRate( new JackpotAnnounce( basebot.getBot(swift_irc), "#smokin_dice" ), 2*60*1000, 60*60*1000);
    	
    	while(true) { Thread.sleep(10); }
    }
}
