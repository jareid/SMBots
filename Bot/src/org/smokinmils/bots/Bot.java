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

import org.smokinmils.BaseBot;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.cashier.ManagerSystem;
import org.smokinmils.cashier.commands.*;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.cashier.tasks.*;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.*;
import org.smokinmils.games.poker.*;
import org.smokinmils.games.rockpaperscissors.*;
import org.smokinmils.games.timedrollcomp.*;
import org.smokinmils.help.Help;

/**
 * Starts the Cashier bot with the correct servers and channels
 * 
 * @author Jamie Reid
 */
public class Bot {
    public static void main(String[] args) throws Exception {
    	BaseBot basebot = BaseBot.getInstance();
    	boolean debug = true;
    	boolean refund = true;
    	basebot.initialise("SM_BOT", "5w807", "smokinmils", debug, refund);
    	String swift_irc = "SwiftIRC";
    	basebot.addServer(swift_irc, "irc.SwiftIRC.net", 6667);
    	IrcBot swift_bot = basebot.getBot(swift_irc);
    	
    	String[] all_swift_chans = {"#SMGamer", "#sm_tournaments", "#sm_overunder",
    								"#sm_roulette", "#sm_ranks", "#managers",
    								"#sm_express", "#sm_vip"};
		String[] dd_swift_chans = {"#SMGamer", "#sm_tournaments", "#sm_vip", "#sm_express"};
		String[] ou_swift_chans = {"#sm_overunder", "#sm_tournaments", "#sm_vip", "#sm_express"};
		String[] host_swift_chans = {"#sm_ranks", "#managers"};
		String[] mgrs_swift_chans = {"#managers"};
		String   poker_lobby_swift = "#SMGamer";
    	
    	Client poker = new Client(swift_irc, poker_lobby_swift);
    	poker.initialise();
    	basebot.addListener(swift_irc, poker);
    	
    	for (String chan: all_swift_chans) {
    		basebot.addChannel(swift_irc, chan);
    	}
    	
    	// Set up jackpot chan
    	Rake.init("#SMGamer");
    	
    	basebot.addListener(swift_irc, new Referral(), all_swift_chans);
    	basebot.addListener(swift_irc, new GroupReferal(), host_swift_chans);
    	basebot.addListener(swift_irc, new RankGroups(), mgrs_swift_chans);
    	
    	basebot.addListener(swift_irc, new Roulette(5, "#SMGamer", swift_bot) );
    	basebot.addListener(swift_irc, new Roulette(1, "#sm_roulette", swift_bot) );
    	basebot.addListener(swift_irc, new Roulette(1, "#sm_express", swift_bot) );
    	basebot.addListener(swift_irc, new Roulette(2, "#sm_tournaments", swift_bot) );
    	basebot.addListener(swift_irc, new Roulette(3, "#sm_vip", swift_bot) );
		
    	basebot.addListener(swift_irc, new OverUnder(), ou_swift_chans);	
    	basebot.addListener(swift_irc, new DiceDuel(swift_bot, "#SMGamer"), dd_swift_chans);
    	basebot.addListener(swift_irc, new CheckChips(), all_swift_chans);	
    	basebot.addListener(swift_irc, new CompPosition(), all_swift_chans);	
    	basebot.addListener(swift_irc, new GiveChips(), all_swift_chans);	
    	basebot.addListener(swift_irc, new Help(), all_swift_chans);	
    	basebot.addListener(swift_irc, new Jackpots(), all_swift_chans);
    	//basebot.addListener(swift_irc, new Lottery(), all_swift_chans);
    	basebot.addListener(swift_irc,
    					    new ManagerSystem("#SMGamer", "#managers", swift_bot),
    					    all_swift_chans);
    	basebot.addListener(swift_irc, new Payout(), all_swift_chans);	
    	basebot.addListener(swift_irc, new Profile(), all_swift_chans);	
    	basebot.addListener(swift_irc, new Profiles(), all_swift_chans);	
    	basebot.addListener(swift_irc, new TransferChips(), all_swift_chans);	
    	basebot.addListener(swift_irc, new CreateTimedRoll(), host_swift_chans);
    	
    	RPSGame rps_event = new RPSGame();
    	rps_event.addValidChan(all_swift_chans);
    	rps_event.addAnnounce("#SMGamer", swift_bot);
    	basebot.addListener(swift_irc, rps_event);
    	
    	// add timed roll for Smoking_Dice every 24hours with a 100chip prize
		@SuppressWarnings("unused") /* suppresed as this doesn't need to be refered to */
		TimedRollComp trc_event = new TimedRollComp(basebot.getBot(swift_irc),
    												"#SMGamer", ProfileType.EOC,
    												5, 10, -1, null);
    	
    	ManagerAnnounce mgr_ano = new ManagerAnnounce( basebot.getBot(swift_irc), "#SMGamer" );
    	mgr_ano.begin(0);
    	
    	Timer bet_timer = new Timer(true);
    	bet_timer.scheduleAtFixedRate( new BetDetails( basebot.getBot(swift_irc), "#SMGamer" ), 5*60*1000, 60*60*1000);
    	
    	Timer comp_timer = new Timer(true);
    	comp_timer.scheduleAtFixedRate( new Competition( basebot.getBot(swift_irc), "#SMGamer" ), 60*1000, 60*1000);
    	
    	Timer jkpt_timer = new Timer(true);
    	jkpt_timer.scheduleAtFixedRate( new JackpotAnnounce( basebot.getBot(swift_irc), "#SMGamer" ), 2*60*1000, 60*60*1000);
    	
    	while(true) { Thread.sleep(1000); }
    }
}
