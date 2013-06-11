/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.ini4j.Ini;
import org.ini4j.Wini;
import org.pircbotx.Channel;
import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.logging.EventLog;
/**
 * Provides the functionality for managers to login/logout and users to check who is logged in
 * 
 * @author Jamie
 */
public class ManagerSystem extends Event {
	public static final String OnCommand = "!on";
	public static final String LoginCommand = "!login";
	public static final String LogoutCommand = "!logout";
	
	private static final String FileName = "managers.ini";	

	private static final String NoLoggedOn = "%b%c12[%c04Logged In%c12] There are %c04no%c12 currently logged in managers.";
	private static final String LoggedOn = "%b%c12[%c04Logged In%c12] Currently logged in manager: %c04%who";
	private static final String NotLoggedIn = "%b%c12[%c04Login%c12]%c04 You are not currently logged in...";
	private static final String LoggedIn = "%b%c12[%c04Login%c12]%c04 %who%c12, you have sucessfully been logged in!";
	private static final String LoggedOut = "%b%c12[%c04Login%c12]%c04 %who%c12, you have sucessfully been logged out!";
	private static final String InactiveLoggedOut = "%b%c12[%c04Inactive%c12]%c04 %who%c12, you have been logged out for inactivity in %c04%actchan%c12!";
	private static final String CantLogIn = "%b%c12[%c04Login%c12]%c04 %b%c04%who%c12 is currently logged in, please wait until they finish their shift";
	
	private static String LoggedInUser;
	private static Map<String, Double> ManagerTimes;
	private static int InactiveTime;
	private static String ActivityChan;
	private static String ManagerChan;
	private static IrcBot Bot;
	
	private static final int DefaultInactiveTime = 15;
	
	private static Timer NextMin;
	private static Timer Inactive;
	
	public ManagerSystem(String active_chan, String manager_chan, IrcBot bot) {
		LoggedInUser = null;
		ManagerTimes = new HashMap<String, Double>();
		InactiveTime = DefaultInactiveTime;
		ActivityChan = active_chan;
		ManagerChan = manager_chan;
		Bot = bot;

		try {
			File inifile = new File( FileName );
			if(inifile.exists()) {
			// read from the file
				Ini ini = new Ini( new FileReader( FileName ) );
		        LoggedInUser = ini.get("loggedin", "who");
		        Integer temp = ini.get("inactive", "maxtime", Integer.class);
		        InactiveTime = (temp == null ? DefaultInactiveTime : temp);
		        
		        Ini.Section section = ini.get("times");
		        if (section != null) {
			        for (String user: section.keySet()) {
			        	Double val = section.get(user, Double.class);
			        	if (val == null) val = 0.0;
			        	ManagerTimes.put(user, val);
			        }
		        }
			}
		} catch (IOException e) {
			EventLog.log(e, "ManagerSystem", "ManagerSystem");
		}
		
		NextMin = new Timer(true);
		NextMin.scheduleAtFixedRate(new IncreaseTime(), 60*1000, 60*1000);
		
		if (LoggedInUser != null) {			
			Inactive = new Timer(true);
			Inactive.schedule( new InactiveTask(), InactiveTime*60*1000);
		}
	}
	
	/**
	 * This method handles the chips command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */
	@Override
	public void message(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		Channel chan = event.getChannel();
		
		if ( bot.userIsIdentified( sender )) {
			if ( sender.equalsIgnoreCase( LoggedInUser ) &&
				chan.getName().equalsIgnoreCase(ActivityChan)) {
				Inactive.cancel();
				Inactive = new Timer(true);
				Inactive.schedule( new InactiveTask(), InactiveTime*60*1000);
			}
			
			if (isValidChannel( chan.getName() ) && Utils.startsWith(message, OnCommand )) {
				if (LoggedInUser == null) {
					bot.sendIRCMessage( event.getChannel(), NoLoggedOn );
				} else {
					bot.sendIRCMessage( event.getChannel(),
									LoggedOn.replaceAll( "%who", LoggedInUser ) );
				}
			} else if (ManagerChan.equalsIgnoreCase( chan.getName() ) &&
			            Utils.startsWith(message, LoginCommand )) {
				if (LoggedInUser != null) {
					bot.sendIRCNotice(sender, CantLogIn.replaceAll("%who", LoggedInUser));					
				} else {
					managerLoggedIn( sender );
					bot.sendIRCMessage(chan, LoggedIn.replaceAll("%who", sender));	
				}
			} else if (ManagerChan.equalsIgnoreCase( chan.getName() ) &&
			            Utils.startsWith(message, LogoutCommand )) {
				if (LoggedInUser == null || !LoggedInUser.equalsIgnoreCase(sender)) {
					bot.sendIRCNotice(sender, NotLoggedIn);
				} else {
					managerLoggedOut();
					bot.sendIRCMessage(chan, LoggedOut.replaceAll("%who", sender));	
				}
			}
		}
	}
	
	private static void managerLoggedIn(String who) {
		if (Inactive != null) Inactive.cancel();
		Inactive = new Timer(true);
		Inactive.schedule( new InactiveTask(), InactiveTime*60*1000);
		LoggedInUser = who;
	}
	
	private static void managerLoggedOut() {
		if (Inactive != null) Inactive.cancel();
		LoggedInUser = null;
	}
	
	public static void inactive() {
		if (Inactive != null) Inactive.cancel();
		if (LoggedInUser != null) {
			String out = InactiveLoggedOut.replaceAll("%who", LoggedInUser);
			out = out.replaceAll("%actchan", ActivityChan);
			Bot.sendIRCMessage(ManagerChan, out);	
			managerLoggedOut();
		}
	}
	
	public static void nextMinute() {
		checkData();
		String user = (LoggedInUser == null ? "NOBODY" : LoggedInUser);
		if (LoggedInUser != null) {
			Double current = ManagerTimes.get(user);
			if (current == null) current = 0.0;
			current += (1.0 / 60.0);
			ManagerTimes.put(user, current);
			saveData();
		}
	}
	
	private static void checkData() {
		File inifile = new File( FileName );
		if(!inifile.exists()) {
			ManagerTimes.clear();
			try {
				if(!inifile.createNewFile()) return;
			} catch (IOException e) {
				EventLog.log(e, "ManagerSystem", "checkData");
			}
		}
	}
	
	private static void saveData() {
	    try {
			File inifile = new File( FileName );
			Wini ini = new Wini(inifile);
			
	        ini.put("loggedin", "who", LoggedInUser);	   
	        ini.put("inactive", "maxtime", InactiveTime);	        
	        for (Entry<String, Double> entry: ManagerTimes.entrySet())  {
	            ini.put("times", entry.getKey(), entry.getValue());
	        }
	        ini.store();
		} catch (IOException e) {
			EventLog.log(e, "ManagerSystem", "saveData");
		}
	}
}


class IncreaseTime extends TimerTask {
	public void run() {
		ManagerSystem.nextMinute();
	}
}

class InactiveTask extends TimerTask {
	public void run() {
		ManagerSystem.inactive();
	}
}