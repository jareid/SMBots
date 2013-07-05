/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.testbots;

import java.io.FileWriter;
import java.io.Writer;
import java.util.Timer;

import org.smokinmils.BaseBot;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.XMLLoader;
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
import org.smokinmils.games.rockpaperscissors.RPSGame;
import org.smokinmils.games.timedrollcomp.CreateTimedRoll;
import org.smokinmils.games.timedrollcomp.TimedRollComp;
import org.smokinmils.help.Help;

import com.sun.jna.platform.win32.Kernel32;

/**
 * Starts the Cashier bot with the correct servers and channels
 * 
 * @author Jamie Reid
 */
public class Bot {
    public static void main(String[] args) throws Exception {        
        String server = "irc.SwiftIRC.net";
        if (args.length > 0) {
            try {
                server = args[0];
            } catch (NumberFormatException e) {
                System.err.println("Argument" + " must be an integer");
                System.exit(1);
            }
        }
        
        // TODO: Test only on server or address will fail.
        //InetAddress local = InetAddress.getByName("bot.smgamer.com");
        
        // Store the process PID. note only windows.
        //int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        //Writer wr = new FileWriter("nick.pid");
        //wr.write(Integer.toString(pid));
        //wr.close();
        
        XMLLoader.getInstance().loadBotSettings();

        while (true) {
            Thread.sleep(10);
        }
    }
}
