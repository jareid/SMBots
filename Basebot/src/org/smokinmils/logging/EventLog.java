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
 * Class interface through which all logging occurs.
 * 
 * @author palacsint
 * @see <a href="http://codereview.stackexchange.com/questions/12336/robust-log
 * ging-solution-to-file-on-disk-from-multiple-threads-on-serverside-code">
 * codereview</a>
 */
public final class EventLog {
    /** The eventlog object. */
	private static Logstream eventLog;
	
    /** A boolean denoting if we log debug or not. */
	private static boolean debug;
    
    /**
     * Hiding the default constructor.
     */
    private EventLog() { }
    
	/**
	 * Create a new log system.
	 * 
	 * @param queue    The queue
	 * @param rootpath The file path
	 * 
	 * @return The logstream object
	 */
	private static Logstream create(final BlockingQueue<StringBuilder> queue,
	                               final String rootpath) {
		eventLog = new Logstream(queue, rootpath);
		return eventLog;
	}
	
    /**
     * Create a new log system.
     * 
     * @param rootpath The file path
     * @param dbg    If this log logs debug or not
     * 
     * @return The logstream object
     */
	public static Logstream create(final String rootpath, final boolean dbg) {
		debug = dbg;
	    return create(new LinkedBlockingQueue<StringBuilder>(), rootpath);
	}

	/**
	 * Log regular events.
	 * 
	 * @param ex   The exception
	 * @param cls  The originating class
	 * @param func The originating functions
	 */
	public static void log(final Exception ex, 
	                       final String cls,
	                       final String func) {
	    log("Exception: " + ex.toString(), cls, func);
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		log(sw.toString(), cls, func);
	}
	
	/**
     * Log regular events.
     * 
     * @param msg   The message
     * @param cls  The originating class
     * @param func The originating functions
     */
	public static void log(final String msg,
	                       final String cls,
	                       final String func) {
	    eventLog.write(msg, cls, func);
	}
	 
	/**
     * Log info events.
     * 
     * @param msg   The message
     * @param cls  The originating class
     * @param func The originating functions
     */
	public static void info(final String msg,
	                        final String cls,
	                        final String func) {
	    log("INFO: " + msg, cls, func);
	}
	 
	/**
     * Log debug events.
     * 
     * @param msg   The message
     * @param cls  The originating class
     * @param func The originating functions
     */
    public static void debug(final String msg,
	                         final String cls,
	                         final String func) {
		if (debug) {
			log("DEBUG " + msg, cls, func);
		}
	}
	
    /**
     * Log fatal events.
     * 
     * @param ex   The exception
     * @param cls  The originating class
     * @param func The originating functions
     */
    public static void fatal(final Exception ex,
	                         final String cls,
	                         final String func) {
	    log("Fatal Exception: " + ex.toString(), cls, func);
	}
}
