/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.testbots;

import java.util.Timer;

import org.smokinmils.BaseBot;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.cashier.ManagerSystem;
import org.smokinmils.cashier.commands.Coins;
import org.smokinmils.cashier.commands.Referrals;
import org.smokinmils.cashier.commands.UserCommands;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.cashier.tasks.BetDetails;
import org.smokinmils.cashier.tasks.Competition;
import org.smokinmils.cashier.tasks.JackpotAnnounce;
import org.smokinmils.cashier.tasks.ManagerAnnounce;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.DiceDuel;
import org.smokinmils.games.casino.OverUnder;
import org.smokinmils.games.casino.blackjack.BJGame;
import org.smokinmils.games.rockpaperscissors.RPSGame;
import org.smokinmils.games.timedrollcomp.CreateTimedRoll;
import org.smokinmils.games.timedrollcomp.TimedRollComp;
import org.smokinmils.help.Help;

/**
 * Starts the Cashier bot with the correct servers and channels
 * 
 * @author Jamie Reid
 */
public class Bot {
    public static void main(String[] args)
        throws Exception {        
        String server = "irc.SwiftIRC.net";
        if (args.length > 0) {
            try {
                server = args[0];
            } catch (NumberFormatException e) {
                System.err.println("Argument" + " must be an integer");
                System.exit(1);
            }
        }

        //TODO: read from args.
        String nick = "TESTSM_BOT";
        
        // Store the process PID. note only windows.
        //int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        //Writer wr = new FileWriter("nick.pid");
        //wr.write(Integer.toString(pid));
        //wr.close();
        
        BaseBot basebot = BaseBot.getInstance();
        boolean debug = true;
        boolean refund = false;
        basebot.initialise(nick, "5w807", "smokinmils", debug, refund);
        String swift_irc = "SwiftIRC";
        basebot.addServer(swift_irc, server, 6667);
        IrcBot swift_bot = basebot.getBot(swift_irc);

        String[] all_swift_chans = { "#testeroo" };
        String[] oudd_swift_chans = { "#testeroo" };
        String[] host_swift_chans = { "#testeroo" };
        String[] mgrs_swift_chans = { "#testeroo" };
        String poker_lobby_swift = "#testeroo";
        
        Thread.sleep(250); // wait for some time to allow bot to connect.

        for (String chan : all_swift_chans) {
            basebot.addChannel(swift_irc, chan);
        }

        //Client poker = new Client(swift_irc, poker_lobby_swift);
        //poker.initialise();
        //basebot.addListener(swift_irc, poker);

        // Set up jackpot chan
        Rake.init("#testeroo");

        basebot.addListener(swift_irc, new Referrals(mgrs_swift_chans, host_swift_chans),
                all_swift_chans);

       // basebot.addListener(swift_irc, new Roulette(2, "#testeroo", swift_bot));

        basebot.addListener(swift_irc, new BJGame(swift_bot), oudd_swift_chans);
        basebot.addListener(swift_irc, new OverUnder(), oudd_swift_chans);
        basebot.addListener(
                swift_irc, new DiceDuel(swift_bot, "#testeroo"),
                oudd_swift_chans);
        basebot.addListener(swift_irc, new UserCommands(), all_swift_chans);
        basebot.addListener(swift_irc, new Help(), all_swift_chans);
        // basebot.addListener(swift_irc, new Lottery(), all_swift_chans);
        basebot.addListener(swift_irc,
                new ManagerSystem("#testeroo", "#testeroo", swift_bot),
                all_swift_chans);
        basebot.addListener(swift_irc, new Coins(), all_swift_chans);
        basebot.addListener(swift_irc, new CreateTimedRoll(), host_swift_chans);

        RPSGame rps_event = new RPSGame();
        rps_event.addValidChan(all_swift_chans);
        rps_event.addAnnounce("#testeroo", swift_bot);
        basebot.addListener(swift_irc, rps_event);

        // add timed roll for Smoking_Dice every 24hours with a 100chip prize
        @SuppressWarnings("unused")
        /* suppresed as this doesn't need to be refered to */
        TimedRollComp trc_event = new TimedRollComp(swift_bot,
                "#testeroo", ProfileType.EOC,
                2, 5, -1, null);

        ManagerAnnounce mgr_ano = new ManagerAnnounce(
                basebot.getBot(swift_irc), "#testeroo");
        mgr_ano.begin(0);

        Timer bet_timer = new Timer(true);
        bet_timer.scheduleAtFixedRate(new BetDetails(basebot.getBot(swift_irc),
                "#testeroo"), 5 * 60 * 1000, 60 * 60 * 1000);

        Timer comp_timer = new Timer(true);
        comp_timer.scheduleAtFixedRate(
                new Competition(basebot.getBot(swift_irc), "#testeroo"),
                60 * 1000, 60 * 1000);

        Timer jkpt_timer = new Timer(true);
        jkpt_timer.scheduleAtFixedRate(
                new JackpotAnnounce(basebot.getBot(swift_irc), "#testeroo"),
                2 * 60 * 1000, 60 * 60 * 1000);

        while (true) {
            Thread.sleep(10);
        }
    }
}
