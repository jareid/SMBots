/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.tasks;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.smokinmils.pokerbot.Client;
import org.smokinmils.pokerbot.settings.Variables;

/**
 * A task that handles a the reconnection to IRC when the connection is dropped
 * 
 * @author Jamie Reid
 */
public class Reconnect extends TimerTask {	
	Client ircClient;
	
	/**
	 * Constructor
	 * 
	 * @param the IRC client
	 */
	public Reconnect(Client irc) {
		ircClient = irc;
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.util.TimerTask#run()
	 */
	public void run() {
		Timer rcnt_timer = ircClient.getReconnectTimer();
		try {
			if ( !ircClient.isConnected() )
			{
				ircClient.connect(Variables.Server, Variables.Port);
			}
		} catch (NickAlreadyInUseException e) {
			ircClient.disconnect();
			rcnt_timer = new Timer(true);
			rcnt_timer.schedule( new Reconnect( ircClient ), Variables.ReconnectMS );
		} catch (IOException | IrcException e) {
			rcnt_timer = new Timer(true);
			rcnt_timer.schedule( new Reconnect( ircClient ), Variables.ReconnectMS );
		}
	}
}
