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

import org.pircbotx.exception.IrcException;
import org.pircbotx.exception.NickAlreadyInUseException;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.smokinmils.bot.CheckIdentified;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.logging.EventLog;
/**
 * A singleton Database access system for the poker bot
 * 
 * @author Jamie Reid
 */
public class SMBaseBot {	
	/** Instance variable */
	private static SMBaseBot _instance = new SMBaseBot();

	/** Static 'instance' method */
	public static SMBaseBot getInstance() { return _instance; }
   
	/** The pircbotx instance */
	private static Map<String,IrcBot> bots;
   
	/** Boolean on whether the bot has been initialised */
 	private static boolean _initialised = false;
   
	/** Boolean on whether debug output is on */
	private static boolean _debug = false;
   
	/** The bot nickname */
	private static String _nick;
	
	/** The bot nickserv password */
	private static String _password;
   
	/** The bot ident string */
	private static String _ident;

	/** The current version of the bot */
	private static String Version = "SmokinMils Bot System 0.1";
	
	/** The message to return on a CTCP FINGER request */
	private static String FingerMessage = "";
   
   /**
    * Constructor
    */
   private SMBaseBot() {
	   bots = new HashMap<String,IrcBot>();
   }   
   
   /**
    * Sets up the bot with the correct servers and channels
    * 
    * @param nickname a list of all the servers the bot should connect to
    * @param password the nickserv password for the bot
    * @param login the ident name for this bot
    * @param debug if we should turn the debug on
    * 
    * @return true if the bot hasn't already been initialised
    */
   public boolean initialise(String nickname, String password, String login, boolean debug) {
	   boolean ret = false;
	   if (!_initialised) {
		   _debug = debug;
		   _nick = nickname;
		   _password = password;
		   _ident = login;
	       _initialised = true;
	       EventLog.create(_nick, _debug);
	       ret = true;
	   }
	   return ret;
   }
	   
   /**
    * Creates a new connection to a server.
    * @param name	The name of the server to add
    * @param addr	The address for the server
    */
   public void addServer(String name, String addr) { addServer(name, addr, 6667); }
   
   /**
    * Creates a new connection to a server.
    * @param name	The name of the server to add
    * @param addr	The address for the server
    * @param port	The port for this server
    */
   public void addServer(String name, String addr, int port) {
	   IrcBot newbot = new IrcBot();

	   newbot.setName(_nick);
	   newbot.setLogin(_ident);
	   newbot.setVerbose(_debug);
	   newbot.identify(_password);
	   newbot.setAutoNickChange(true);
	   newbot.useShutdownHook(false);
	   newbot.setVersion(Version);
	   newbot.setFinger(FingerMessage);
	   // TODO: check what the two lines below do
	   newbot.setAutoReconnect(true);
	   newbot.setAutoReconnectChannels(true);
	   newbot.startIdentServer();
	   
	   // TODO: move to settings?
	   newbot.setMessageDelay(5);
	   
	   newbot.setListenerManager( new ThreadedListenerManager<IrcBot>() );
	   newbot.getListenerManager().addListener( new CheckIdentified() );
	   
	   try {
		   newbot.connect(addr, port);
	   } catch (NickAlreadyInUseException e) {
		   // TODO 
	   } catch (IOException e) {
		   // TODO
	   } catch (IrcException e) {
		   // TODO
	   }
	   
	   bots.put(name, newbot);
   }
   
   /**
    * Tells the bot to join a channel on a server
    * 
    * @param server The name of the server
    * @param channel the channel name
    */
   public boolean addChannel(String server, String channel) {
	   boolean ret;
	   IrcBot bot = bots.get(server);
	   if (bot != null) {
		   bot.joinChannel(channel);
		   EventLog.debug("Joined " + channel + " on " + server, "SMBaseBot", "addChannel");
		   ret = true;
	   } else {
		   EventLog.log("There is no bot currently connected to " + server, "SMBaseBot", "addChannel");
		   ret = false;
	   }
	   return ret;
   }
   
   /**
    * Tells the bot to add a listener for a certain server
    * 
    * @param server The name of the server
    * @param channel The instance of a listener class
    */
   public boolean addListener(String server, Event listener) {
	   boolean ret;
	   IrcBot bot = bots.get(server);
	   if (bot != null) {
		   bot.getListenerManager().addListener( listener );
		   EventLog.debug("Added new listener for " + server, "SMBaseBot", "addListener");
		   ret = true;
	   } else {
		   EventLog.log("There is no bot currently connected to " + server, "SMBaseBot", "addListener");
		   ret = false;
	   }
	   return ret;
   }

   /**
    * Returns the name of the server a bot is connected to.
    * 
    * @param ircBot the bot to check.
    * 
    * @return the server name
    */
	public String getServer(IrcBot ircBot) {
		String ret = null;
		for (Entry<String, IrcBot> bot: bots.entrySet()) {
			if (bot.getValue() == ircBot) {
				ret = bot.getKey();
				break;
			}
		}
		
		if (ret == null)
			EventLog.log("A bot was passed to the function that is not a part of the system", "SMBaseBot", "getServer");
		
		return ret;
	}
	
   /**
    * Returns the bot for a certain server
    * 
    * @param server the name of the server
    * 
    * @return the bot object
    */
	public IrcBot getBot(String server) {
		return bots.get(server);
	}
}
