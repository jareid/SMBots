/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

import java.io.IOException;

import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ReconnectEvent;
import org.smokinmils.logging.EventLog;

/**
 * Handles connection events
 * 
 * @author Jamie
 */
public class ConnectEvents extends ListenerAdapter<IrcBot> {
	public void onDisconnect(ReconnectEvent<IrcBot> event) {
		EventLog.log("Bot disconnected, attempting reconnection...", "ConnectEvents", "onDisconnect");
		try {
			Thread.sleep(25);
			event.getBot().reconnect();
		} catch (IOException | IrcException | InterruptedException e) {
			EventLog.fatal(e, "ConnectEvents", "onDisconnect");
		}
	}
	
	public void onReconnect(ReconnectEvent<IrcBot> event) {
		if (event.isSuccess() == false) {
			EventLog.log("Failed to reconnect, attempting reconnection...", "ConnectEvents", "onReconnect");
			try {
				Thread.sleep(50);
				event.getBot().reconnect();
			} catch (IOException | IrcException | InterruptedException e) {
				EventLog.fatal(e, "ConnectEvents", "onReconnect");
			}
		}
	}
}
