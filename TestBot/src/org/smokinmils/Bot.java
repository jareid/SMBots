/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils;

import java.io.FileWriter;
import java.io.Writer;

import org.smokinmils.bot.XMLLoader;

import com.sun.jna.platform.win32.Kernel32;

/**
 * Starts the Cashier bot with the correct servers and channels.
 * 
 * @author Jamie Reid
 */
public class Bot {
    public static void main(String[] args) throws Exception {        
        
    	XMLLoader xl = XMLLoader.getInstance();
    	if (args.length == 1) {
    		xl.loadDocument(args[0]);
    	} 
    	// Store the process PID. note only windows.
    	String pidfile = xl.getBotNick() + ".pid";
    	
        int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        Writer wr = new FileWriter(pidfile);
        wr.write(Integer.toString(pid));
        wr.close();
        
        // load and initialise the bot
    	xl.loadBotSettings();
    	
        while (true) {
            Thread.sleep(10);
        }
    }
}