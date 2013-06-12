/**
 * This file is part of a commercial IRC bot that
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid 
 */
package org.smokinmils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;

import org.pircbotx.Channel;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.smokinmils.bot.AutoJoin;
import org.smokinmils.bot.CheckIdentified;
import org.smokinmils.bot.ConnectEvents;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.database.DB;
import org.smokinmils.logging.EventLog;

/**
 * The main bot that contains access to all the IrcBots for each server.
 * 
 * @author Jamie Reid
 */
public final class BaseBot {
	/** Instance variable. */
	private static BaseBot instance = new BaseBot();

	/**
	 * Static 'instance' method.
	 * 
	 * @return the BaseBot instance
	 */
	public static BaseBot getInstance() { return instance; }
   
	/** The pircbotx instance. */
	private static Map<String, IrcBot> bots;
   
	/** Boolean on whether the bot has been initialised. */
 	private static boolean initialised = false;
   
	/** Boolean on whether debug output is on. */
	private static boolean debug = false;
   
	/** The bot nickname. */
	private static String nick;

	/** The bot nickserv password. */
	private static String password;
   
	/** The bot ident string. */
	private static String ident;

	/** The current version of the bot. */
	private static final String VERSION = "SmokinMils Bot System 0.1";
	
	/** The message to return on a CTCP FINGER request. */
	private static final String FINGER_MSG = "Leave me alone, kthx!";
	
	/** The default IRC port. */
	private static final int DEFAULT_PORT = 6667;
	
	/** The number of ms to try to auto join. */
    private static final int REJOIN_MS = 30000;
	
	/** An object to provide synchronisation functionality in the bot. */
	private static Object lockObject = new Object();
   
   /**
    * Constructor.
    */
   private BaseBot() {
	   bots = new HashMap<String, IrcBot>();
   }
   
   /**
    * Sets up the bot with the correct servers and channels.
    * 
    * @param nickname a list of all the servers the bot should connect to
    * @param pswd the nickserv password for the bot
    * @param login    the ident name for this bot
    * @param dbg    if we should turn the debug on
    * @param refund   used to check if we should execute refunds when started
    * 
    * @return true if the bot hasn't already been initialised
    */
   public boolean initialise(final String nickname,
                             final String pswd,
                             final String login,
                             final boolean dbg,
                             final boolean refund) {
	   boolean ret = false;
	   if (!initialised) {
		   debug = dbg;
		   nick = nickname;
		   password = pswd;
		   ident = login;
	       initialised = true;
	       EventLog.create(nick, debug);
	       ret = true;
	       
	       if (refund) {
    	       try {
    	           DB.getInstance().processRefunds();
    	       } catch (Exception e) {
    	           EventLog.fatal(e, "SMBaseBot", "initialise");
    	           System.exit(0);
    	       }
	       }
	   }
	   return ret;
   }
	   
   /**
    * @return the lockObject
    */
    public static Object getLockObject() {
        return lockObject;
    }

   /**
    * Creates a new connection to a server.
    * 
    * @param name	The name of the server to add
    * @param addr	The address for the server
    */
   public void addServer(final String name, final String addr) {
       addServer(name, addr, DEFAULT_PORT);
   }
   
   /**
    * Creates a new connection to a server.
    * @param name	The name of the server to add
    * @param addr	The address for the server
    * @param port	The port for this server
    */
   public void addServer(final String name, final String addr, final int port) {
	   IrcBot newbot = new IrcBot();

	   newbot.setName(nick);
	   newbot.setLogin(ident);
	   newbot.setVerbose(debug);
	   newbot.setAutoNickChange(true);
	   newbot.useShutdownHook(false);
	   newbot.setVersion(VERSION);
	   newbot.setFinger(FINGER_MSG);
	   newbot.setAutoReconnect(true);
	   newbot.setAutoReconnectChannels(true);
	   newbot.startIdentServer();
	   
	   newbot.setMessageDelay(0);
	   
	   newbot.setListenerManager(new ThreadedListenerManager<IrcBot>());
	   newbot.getListenerManager().addListener(new CheckIdentified(newbot));
	   newbot.getListenerManager().addListener(new ConnectEvents());
	   
	   try {
		   newbot.connect(addr, port);
		} catch (IOException | IrcException e) {
			EventLog.fatal(e, "ConnectEvents", "onDisconnect");
		}
	   
	   bots.put(name, newbot);
	   
	   // check we are in all the channels we should be
	   Timer rejoin = new Timer(true);
	   rejoin.scheduleAtFixedRate(new AutoJoin(newbot), REJOIN_MS, REJOIN_MS);
   }
   
   /**
    * Tells the bot to join a channel on a server.
    * 
    * @param server The name of the server
    * @param channel the channel name
    * 
    * @return true if action was successful
    */
   public boolean addChannel(final String server, final String channel) {
	   boolean ret;
	   IrcBot bot = bots.get(server);
	   if (bot != null) {
		   bot.joinChannel(channel);
		   bot.addValidChannel(channel);
		   EventLog.debug("Joined " + channel + " on " + server,
		                  "SMBaseBot", "addChannel");
		   ret = true;
	   } else {
		   EventLog.log("There is no bot currently connected to " + server,
		                "SMBaseBot", "addChannel");
		   ret = false;
	   }
	   return ret;
   }
   
   /**
    * Tells the bot to add a listener for a certain server.
    * 
    * @param server The name of the server
    * @param listener The instance of a listener class
    * 
    * @return true if action was successful
    */
   public boolean addListener(final String server, final Event listener) {
	   boolean ret;
	   IrcBot bot = bots.get(server);
	   if (bot != null) {
		   bot.getListenerManager().addListener(listener);
		   EventLog.debug("Added new listener for " + server,
		                  "SMBaseBot", "addListener");
		   ret = true;
	   } else {
		   EventLog.log("There is no bot currently connected to " + server,
		                "SMBaseBot", "addListener");
		   ret = false;
	   }
	   return ret;
   }

   /**
    * Adds a listener object to the irc bot.
    * 
    * @param server     The server the listner is for
    * @param listener   The listener object 
    * @param channels   The valid channels
    */
   public void addListener(final String server,
                           final Event listener,
                           final String[] channels) {
       listener.addValidChan(channels);
       addListener(server, listener);
   }

   /**
    * Returns the name of the server a bot is connected to.
    * 
    * @param ircBot the bot to check.
    * 
    * @return the server name
    */
	public String getServer(final IrcBot ircBot) {
		String ret = null;
		for (Entry<String, IrcBot> bot: bots.entrySet()) {
			if (bot.getValue() == ircBot) {
				ret = bot.getKey();
				break;
			}
		}
		
		if (ret == null) {
            EventLog.log("A bot was passed to the function that is not a part "
                         + "of the system", "SMBaseBot", "getServer");
        }
		
		return ret;
	}
	
   /**
    * Returns the bot for a certain server.
    * 
    * @param server the name of the server
    * 
    * @return the bot object
    */
	public IrcBot getBot(final String server) {
		return bots.get(server);
	}
	
	/**
	 * Sends a message to all channels on all servers.
	 * 
	 * @param out The message to send.
	 */
	public static void sendMessageToAll(final String out) {
		for (IrcBot bot: bots.values())  {
			for (Channel chan: bot.getChannels()) {
				bot.sendIRCMessage(chan.getName(), out);
			}
		}
	}
	
	/**
	 * Identifies a bot.
	 * 
	 * @param bot the bot to identify
	 */
	public static void identify(final IrcBot bot) {
		bot.identify(password);
	}
}
