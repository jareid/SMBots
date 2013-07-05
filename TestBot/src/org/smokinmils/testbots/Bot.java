/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.testbots;

import org.smokinmils.bot.XMLLoader;

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
