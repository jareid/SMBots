/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Class interface through which all logging occurs
 * @author palacsint
 * @see http://codereview.stackexchange.com/questions/12336/robust-logging-solution-to-file-on-disk-from-multiple-threads-on-serverside-code
 *
 */
public class EventLog {
	private static Logstream _eventLog;
	private static boolean _debug;

	public static Logstream create(BlockingQueue<StringBuilder> queue, String rootPath) {
		_eventLog = new Logstream(queue, rootPath);
		return _eventLog;
	}

	public static Logstream create(String rootPath, boolean debug) {
		_debug = debug;
	    return create(new LinkedBlockingQueue<StringBuilder>(), rootPath);
	}

	public static void log(Exception ex, String cls, String func) {
	    log("Exception: " + ex.toString(), cls, func);
	}
	
	public static void log(String msg, String cls, String func) {
	    _eventLog.write(msg, cls, func);
	}
	 
	public static void info(String msg, String cls, String func) {
	    log("INFO: " + msg, cls, func);
	}
	 
	public static void debug(String msg, String cls, String func) {
		if (_debug == true) {
			log("DEBUG " + msg, cls, func);
		}
	}
	
	public static void fatal(Exception ex, String cls, String func) {
	    log("Fatal Exception: " + ex.toString(), cls, func);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		log(sw.toString(), cls, func);
	}
}
