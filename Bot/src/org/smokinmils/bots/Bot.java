/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.bots;

import java.io.FileWriter;
import java.io.Writer;
import java.net.InetAddress;
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
import org.smokinmils.games.casino.Roulette;
import org.smokinmils.games.casino.blackjack.BJGame;
import org.smokinmils.games.casino.poker.Client;
import org.smokinmils.games.rockpaperscissors.RPSGame;
import org.smokinmils.games.timedrollcomp.TimedRollComp;
import org.smokinmils.help.Help;

import com.sun.jna.platform.win32.Kernel32;

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

        String nick = "SM_BOT";
        InetAddress local = InetAddress.getByName("bot.smgamer.com");
        
        // Store the process PID. note only windows.
        int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        Writer wr = new FileWriter(nick + ".pid");
        wr.write(Integer.toString(pid));
        wr.close();
        
        BaseBot basebot = BaseBot.getInstance();
        boolean debug = false;
        boolean refund = true;
        basebot.initialise(nick, "5w807", "smokinmils", debug, refund, true);
        String swift_irc = "SwiftIRC";
        basebot.addServer(swift_irc, server, 6667, local);
        IrcBot swift_bot = basebot.getBot(swift_irc);

        String[] all_swift_chans = { "#SMGamer", "#sm_tournaments",
                "#sm_overunder",  "#sm_roulette", "#sm_ranks", "#managers",
                "#sm_express", "#sm_vip", "#Private1"};
        String[] dd_swift_chans = { "#SMGamer", "#sm_tournaments", "#sm_vip",
                                    "#sm_express", "#Private1" };
        String[] ou_swift_chans = { "#sm_overunder", "#sm_tournaments",
                                    "#sm_vip", "#sm_express", "#Private1" };
        String[] host_swift_chans = { "#sm_ranks", "#managers" };
        String[] mgrs_swift_chans = { "#managers" };
        String poker_lobby_swift = "#SMGamer";
        
        Thread.sleep(250); // wait for some time to allow bot to connect.

        Client poker = new Client(swift_irc, poker_lobby_swift);
        poker.initialise();
        basebot.addListener(swift_irc, poker);

        for (String chan : all_swift_chans) {
            basebot.addChannel(swift_irc, chan);
        }

        // Set up jackpot chan
        Rake.init("#SMGamer");

        basebot.addListener(swift_irc, new Referrals(mgrs_swift_chans, host_swift_chans),
                            all_swift_chans);

        basebot.addListener(swift_irc, new Roulette(5, "#SMGamer", swift_bot));
        basebot.addListener(swift_irc,
                            new Roulette(1, "#sm_roulette", swift_bot));
        basebot.addListener(swift_irc,
                             new Roulette(1, "#sm_express", swift_bot));
        basebot.addListener(swift_irc,
                            new Roulette(2, "#sm_tournaments", swift_bot));
        basebot.addListener(swift_irc, new Roulette(3, "#sm_vip", swift_bot));
        basebot.addListener(swift_irc, new Roulette(3, "#Private1", swift_bot));

        basebot.addListener(swift_irc, new BJGame(swift_bot), dd_swift_chans);
        basebot.addListener(swift_irc, new OverUnder(), ou_swift_chans);
        basebot.addListener(swift_irc, new DiceDuel(swift_bot, "#SMGamer"),
                            dd_swift_chans);
        basebot.addListener(swift_irc, new UserCommands(), all_swift_chans);
        basebot.addListener(swift_irc, new Help(), all_swift_chans);
        // basebot.addListener(swift_irc, new Lottery(), all_swift_chans);
        basebot.addListener(swift_irc,
                new ManagerSystem("#SMGamer", "#managers", "#sm_ranks", swift_bot),
                all_swift_chans);
        basebot.addListener(swift_irc, new Coins(), all_swift_chans);
        
        RPSGame rps_event = new RPSGame();
        rps_event.addValidChan(all_swift_chans);
        rps_event.addAnnounce("#SMGamer", swift_bot);
        basebot.addListener(swift_irc, rps_event);

        // add timed roll for Smoking_Dice every 24hours with a 100chip prize
        @SuppressWarnings("unused")
        /* suppresed as this doesn't need to be refered to */
        TimedRollComp trc_event = new TimedRollComp(basebot.getBot(swift_irc),
                "#SMGamer", ProfileType.EOC,
                5, 10, -1, null);

        ManagerAnnounce mgr_ano = new ManagerAnnounce(
                basebot.getBot(swift_irc), "#SMGamer");
        mgr_ano.begin(0);

        Timer bet_timer = new Timer(true);
        bet_timer.scheduleAtFixedRate(new BetDetails(basebot.getBot(swift_irc),
                "#SMGamer"), 5 * 60 * 1000, 60 * 60 * 1000);

        Timer comp_timer = new Timer(true);
        comp_timer.scheduleAtFixedRate(
                new Competition(basebot.getBot(swift_irc), "#SMGamer"),
                60 * 1000, 60 * 1000);

        Timer jkpt_timer = new Timer(true);
        jkpt_timer.scheduleAtFixedRate(
                new JackpotAnnounce(basebot.getBot(swift_irc), "#SMGamer"),
                2 * 60 * 1000, 60 * 60 * 1000);

        while (true) {
            Thread.sleep(1000);
        }
    }
}
