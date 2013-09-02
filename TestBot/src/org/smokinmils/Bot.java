/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

import org.smokinmils.bot.XMLLoader;
import org.smokinmils.settings.DBSettings;

import com.sun.jna.platform.win32.Kernel32;

/**
 * Starts the Cashier bot with the correct servers and channels.
 * 
 * @author Jamie Reid
 */
public class Bot {
    public static void main(String[] args) throws Exception { 
    	XMLLoader xl = XMLLoader.getInstance();
    	if (args.length == 2) {
    		xl.loadDocument(args[1]);
    	} 
    	
    	if (args.length == 0) {
    	    System.out.println("Arguments are: type ?xml?");
            System.out.println("Where type is either bot or restart");    	    
    	} else if (args[0].equalsIgnoreCase("restart")) {
    	    doRestart();
    	} else if (args[0].equalsIgnoreCase("bot")) {
    	    doBot(xl);
    	} else {
            System.out.println(args[0] + " is an invalid type");
    	}
    }
    
    /**
     * Run the bot.
     * 
     * @param xml
     * @throws InterruptedException 
     * @throws IOException 
     */
    public static void doBot(XMLLoader xml) throws Exception {            
        // Store the process PID. note only windows.
        String pidfile = xml.getBotNick() + ".pid";
        
        int pid = Kernel32.INSTANCE.GetCurrentProcessId();
        Writer wr = new FileWriter(pidfile);
        wr.write(Integer.toString(pid));
        wr.close();
        
        // load and initialise the bot
        xml.loadBotSettings();
        
        while (true) {
            Thread.sleep(10);
        }
    }
    
    /**
     * Handles the restart program.
     * @param args
     */
    public static void doRestart() {
        String url = "jdbc:mysql://"
                   + DBSettings.SERVER + ":"
                   + DBSettings.PORT + "/"
                   + DBSettings.DB_NAME
                   + "?autoReconnect=true&"
                   + "user=" + DBSettings.DB_USER + "&"
                   + "password=" + DBSettings.DB_PASS;
        
        while (true) {
            try {
                if (checkRestart(url)) {
                    System.out.println("Restart table was true. Restarting the bot...");
                    restartBot(url);
                }
                Thread.sleep(30000);
            } catch (Exception e) {
                System.out.println("Something went wrong while restarting!");
                e.printStackTrace();
            }
        }
    }

    /**
     * Handles the db check.
     * 
     * @param url the db url.
     * @return true if we should restart
     * @throws Exception on DB error.
     */
    private static boolean checkRestart(String url) throws Exception {
            Connection connect = null;
            Statement statement = null;
            ResultSet resultSet = null;
            boolean ret = false;
    
       try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            
            // Setup the connection with the DB
            connect = DriverManager.getConnection(url);
            statement = connect.createStatement();
            
            // Result set get the result of the SQL query
            resultSet = statement.executeQuery("SELECT restart FROM restart LIMIT 1");
            if (resultSet.next()) {
                ret = resultSet.getBoolean(1);
            }
        } catch (Exception e) {
          throw e;
        } finally {
            if (resultSet != null) resultSet.close();            
            if (statement != null) statement.close();
            if (connect != null) connect.close();
        }
        return ret;
    }
    
    /**
     * Retsrats the bot.
     * 
     * @param url
     * @throws Exception
     */
    private static void restartBot(String url) throws Exception {
        // Read PID
        File file = new File("SM_BOT.pid");
        if (file.exists()) {
            Scanner scanner = new Scanner(file);
            
            if(scanner.hasNextInt()) {
                int pid = scanner.nextInt();
                
                // Kill
                System.out.println("Killing PID: " + Integer.toString(pid));
                Runtime.getRuntime().exec("taskkill /F /PID " + Integer.toString(pid));
    
            }
            scanner.close();
        }
        
        Thread.sleep(2000);
        
        // Start
        System.out.println("Starting SMGamer.exe");
        Runtime.getRuntime().exec("cmd /c start SMGamer.exe bot settings\\livesettings.xml");
        
        Connection connect = null;
        Statement statement = null;
        ResultSet resultSet = null;
        
        try {
            // Setup the connection with the DB
            connect = DriverManager.getConnection(url);
            statement = connect.createStatement();
        
            // Update the table to stop restarts
            statement.executeUpdate("UPDATE restart SET restart = '0'");
        } catch (Exception e) {
            throw e;
        } finally {
            if (resultSet != null) resultSet.close();            
            if (statement != null) statement.close();
            if (connect != null) connect.close();
        }
    }

}
