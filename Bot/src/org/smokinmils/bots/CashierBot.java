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
import org.smokinmils.bot.IrcBot;
import org.smokinmils.cashier.ManagerSystem;
import org.smokinmils.cashier.commands.*;
import org.smokinmils.cashier.tasks.*;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.*;
import org.smokinmils.games.poker.*;
import org.smokinmils.games.rockpaperscissors.*;
import org.smokinmils.games.timedrollcomp.*;
import org.smokinmils.help.Help;
import org.smokinmils.logging.EventLog;

/**
 * Starts the Cashier bot with the correct servers and channels
 * 
 * @author Jamie Reid
 */
public class CashierBot {
    public static void main(String[] args) throws Exception {
	    try {
	        DB.getInstance().processRefunds();
	    } catch (Exception e) {
	        EventLog.fatal(e, "Casino", "Casino");
	        System.exit(0);
	    }
    	
    	SMBaseBot basebot = SMBaseBot.getInstance();
    	boolean debug = true;
    	basebot.initialise("SM_BOT", "5w807", "smokinmils", debug);
    	String swift_irc = "SwiftIRC";
    	basebot.addServer(swift_irc, "irc.SwiftIRC.net", 6667);
    	IrcBot swift_bot = basebot.getBot(swift_irc);
    	
    	String[] all_swift_chans = {"#smokin_dice", "#sm_tournaments", "#sm_overunder",
    								"#sm_roulette", "#sm_ranks", "#managers"};
		String[] oudd_swift_chans = {"#smokin_dice", "#sm_tournaments"};
		String[] host_swift_chans = {"#sm_ranks", "#managers"};
		String[] mgrs_swift_chans = {"#managers"};
		String   poker_lobby_swift = "#smokin_dice";
		// To test uncomment below/comment above and change other channel references below.
    	//String[] all_swift_chans = {"#testeroo"};
    	//String[] oudd_swift_chans = {"#testeroo"};
    	//String[] host_swift_chans = {"#testeroo"};
		//String[] mgrs_swift_chans = {"#testeroo"};
		//String   poker_lobby_swift = "#testeroo";
    	
    	for (String chan: all_swift_chans) {
    		basebot.addChannel(swift_irc, chan);
    	}
    	
    	basebot.addListener(swift_irc, new Referral(), all_swift_chans);
    	basebot.addListener(swift_irc, new GroupReferal(), host_swift_chans);
    	basebot.addListener(swift_irc, new RankGroups(), mgrs_swift_chans);
    	
    	basebot.addListener(swift_irc, new Roulette(5, "#smokin_dice", swift_bot) );
    	basebot.addListener(swift_irc, new Roulette(1, "#sm_roulette", swift_bot) );
    	basebot.addListener(swift_irc, new Roulette(2, "#sm_tournaments", swift_bot) );
		
    	basebot.addListener(swift_irc, new OverUnder(), oudd_swift_chans);	
    	basebot.addListener(swift_irc, new DiceDuel(swift_bot, "#smokin_dice"), oudd_swift_chans);
    	basebot.addListener(swift_irc, new CheckChips(), all_swift_chans);	
    	basebot.addListener(swift_irc, new CompPosition(), all_swift_chans);	
    	basebot.addListener(swift_irc, new GiveChips(), all_swift_chans);	
    	basebot.addListener(swift_irc, new Help(), all_swift_chans);	
    	basebot.addListener(swift_irc, new Jackpots(), all_swift_chans);
    	//basebot.addListener(swift_irc, new Lottery(), all_swift_chans);	
    	basebot.addListener(swift_irc,
    					    new ManagerSystem("#smokin_dice", "#managers", swift_bot),
    					    all_swift_chans);
    	basebot.addListener(swift_irc, new Client(swift_irc, poker_lobby_swift));
    	basebot.addListener(swift_irc, new Payout(), all_swift_chans);	
    	basebot.addListener(swift_irc, new Profile(), all_swift_chans);	
    	basebot.addListener(swift_irc, new Profiles(), all_swift_chans);	
    	basebot.addListener(swift_irc, new TransferChips(), all_swift_chans);	
    	basebot.addListener(swift_irc, new CreateTimedRoll(), host_swift_chans);
    	
    	RPSGame rps_event = new RPSGame();
    	rps_event.addValidChan(all_swift_chans);
    	rps_event.addAnnounce("#smokin_dice", swift_bot);
    	basebot.addListener(swift_irc, rps_event);
    	
    	// add timed roll for Smoking_Dice every 24hours with a 100chip prize
		@SuppressWarnings("unused") /* suppresed as this doesn't need to be refered to */
		TimedRollComp trc_event = new TimedRollComp(basebot.getBot(swift_irc),
    												"#smokin_dice", ProfileType.EOC,
    												2, 5, -1, null);
    	
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
