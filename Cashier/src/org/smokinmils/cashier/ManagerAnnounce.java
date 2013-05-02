/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.logging.EventLog;
/**
 * Provides announcements about the betting on an irc server
 * 
 * @author Jamie
 */
public class ManagerAnnounce extends TimerTask {	
	private IrcBot Bot;
	private String Channel;
	private Timer AnnounceTimer;
	private List<String> Messages;
	private List<Integer> Intervals;
	private static final String FileName = "messages.ini";
	private static final int DefaultInterval = 1;
	
	/**
	 * Constructor
	 * 
	 * @param bot
	 */
	public ManagerAnnounce(IrcBot bot, String chan) {
		Bot = bot;
		Channel = chan;
		Intervals = new ArrayList<Integer>();
		Messages = new ArrayList<String>();
		readData();

		AnnounceTimer = new Timer();
		AnnounceTimer.schedule( this , DefaultInterval*60*1000);
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		AnnounceTimer.cancel();		
	
		String out = null;
		Integer interval = DefaultInterval;
		if (Messages.size() >= 1 && Intervals.size() >= 1) {
			out = Messages.remove(0);
			interval = Intervals.remove(0); 
		
			Bot.sendIRCMessage(Channel, out);
		}
		
		if (Messages.size() == 0) {
			Intervals.clear();
			readData();
		}
		
		AnnounceTimer = new Timer();
		AnnounceTimer.schedule( this , interval*60*1000);
	}
	
	private void readData() {
		try {
			Ini ini = new Ini( new FileReader( FileName ) );

	    	for (String name: ini.keySet()) {
	    		Section section = ini.get(name);
	    		String msg = section.get("message");
	    		Integer interval = section.get("interval", Integer.class);
	    		if (msg == null) {
	    			EventLog.log(name + " has no message", "ManagerAnnounce", "readData");
	    		} else if (interval == null) {
	    			EventLog.log(name + " has no interval", "ManagerAnnounce", "readData");
	    		} else {
	    			Messages.add( msg );
	    			Intervals.add( interval );
	    		}
	    	}
		} catch (IOException e) {
			EventLog.log(e, "ManagerAnnounce", "readData");
		}
	}
}
