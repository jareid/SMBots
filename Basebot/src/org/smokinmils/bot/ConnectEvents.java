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
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.ReconnectEvent;
import org.smokinmils.BaseBot;
import org.smokinmils.logging.EventLog;

/**
 * Handles connection events.
 * 
 * @author Jamie
 */
public class ConnectEvents extends ListenerAdapter<IrcBot> {
    /** Amount of milliseconds to wait after an event before attempting the
     ** task. */
    private static final int WAITMS = 50;
    
    /**
     * Attempts to identify on a server connection.
     * 
     * @param event the ConnectEvent
     * 
     * @see org.pircbotx.hooks.type.ConnectEvent
     */
	public final void onConnect(final ConnectEvent<IrcBot> event) {
		BaseBot.identify(event.getBot());
	}
	
    /**
     * Attempts to reconnect on disconnection.
     * 
     * @param event the ReconnectEvent
     * 
     * @see org.pircbotx.hooks.type.ReconnectEvent
     */
	public final void onDisconnect(final ReconnectEvent<IrcBot> event) {
		EventLog.log("Bot disconnected, attempting reconnection...",
		             "ConnectEvents", "onDisconnect");
		try {
			Thread.sleep(WAITMS);
			event.getBot().reconnect();
		} catch (IOException | IrcException | InterruptedException e) {
			EventLog.fatal(e, "ConnectEvents", "onDisconnect");
		}
	}
	
    /**
     * Attempts to reconnect on disconnection.
     * 
     * @param event the ReconnectEvent
     * 
     * @see org.pircbotx.hooks.type.ReconnectEvent
     */
	public final void onReconnect(final ReconnectEvent<IrcBot> event) {
		if (!event.isSuccess()) {
			EventLog.log("Failed to reconnect, attempting reconnection...",
			             "ConnectEvents", "onReconnect");
			try {
				Thread.sleep(WAITMS);
				event.getBot().reconnect();
			} catch (IOException | IrcException | InterruptedException e) {
				EventLog.fatal(e, "ConnectEvents", "onReconnect");
			}
		}
	}
}
