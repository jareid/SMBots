/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.BlockingQueue;

/**
 * Class interface through which all logging occurs.
 * 
 * @author palacsint
 * @see <a href="http://codereview.stackexchange.com/questions/12336/robust-log
 * ging-solution-to-file-on-disk-from-multiple-threads-on-serverside-code">
 * codereview</a>
 */
public class Logstream implements Runnable {
    /**  The root path we will use for logging. */
    private final String path;
    
    /** Used to queue up messages for logging. */
    private final BlockingQueue<StringBuilder> queue;
    
    /** Thread to run this logstream on. */
    private final Thread writeThread;

    /** The current day of the month. */
    private int dayOfMonth = -1;
    
    /** Our log path.  Changes for each day of the month. */
    private String cachedPath = "";
    
    /** Amount of MS between queue reads. */
    private static final int QUEUE_MS = 10;

    /**
     * Constructor.
     * 
     * @param queuesys The queue object
     * @param rootpath The root path of the log
     */
   protected Logstream(final BlockingQueue<StringBuilder> queuesys,
                       final String rootpath) {
        path  = rootpath;

        queue = queuesys;  // LinkedBlockingQueue<StringBuilder>();

        writeThread = new Thread(this);
        writeThread.start();
   }

   
   /**
    * Used to write a message to the file.
    * 
    * @param msg    The message
    * @param cls    The class
    * @param func   The function
    */
   public final void write(final String msg,
                           final String cls,
                           final String func) {
       queue(msg, cls, func);      
   }

   /**
    * 
    */
   @Override
public final void run() {
       // logging never stops unless server is restarted so loop forever
       while (true) {
           try {
               StringBuilder builder = queue.take();
               flush(builder);
               Thread.sleep(QUEUE_MS);
            } catch (InterruptedException ex) {
                flush(ex.toString());
                System.out.println("Exception: LogStream.run: "
                                    + ex.getMessage());
            }
        }
    }

   /**
    * Flush the String Builder contents.
    * 
    * @param builder the StringBuilder object
    */
    private void flush(final StringBuilder builder) { 
        flush(builder.toString());
    }

    /**
     * Flush the String contents into the log.
     * 
     * @param data the String object
     */
    private void flush(final String data) {
        BufferedWriter writer = null;

        try {
            System.out.println(data);

            writer = getOutputStream();

            writer.write(data);
            writer.newLine();
            writer.flush();             
        } catch (IOException ex) {
            // what to do if we can't even log to our log file????
            System.out.println("IOException: EventLog.flush: " + ex.getMessage());
        } finally {
            closeOutputStream(writer);
        }
    }

    /**
     * Checks if we are on a new day.
     * 
     * @param calendar The calendar object
     * 
     * @return true if the day changed
     */
    private boolean dayOfMonthHasChanged(final Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_MONTH) != dayOfMonth;
    }

    /**
     * Get's the log path.
     * 
     * @return the log path
     */
    private String getPath() {
        Calendar calendar = Calendar.getInstance();

        if (dayOfMonthHasChanged(calendar)) {
            StringBuilder pathBuilder = new StringBuilder();
            SimpleDateFormat df = new SimpleDateFormat("H_mm.dd-MMM-yy");
            Date date = new Date();

            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            pathBuilder.append(path);
            pathBuilder.append(df.format(date));
            pathBuilder.append(".log");

            cachedPath = pathBuilder.toString();       
        }

        return cachedPath;
    }

    /** 
     * Provides a BufferedWriter for writing to the log.
     * 
     * @return a newly created BufferedWriter
     * 
     * @throws IOException if the creation of a BufferedWriter fails
     */
    private BufferedWriter getOutputStream() throws IOException {
        String dirname = "logs";
        File dir = new File(dirname);
        File afile = new File(dir, getPath());
        return new BufferedWriter(new FileWriter(afile, true));         
    }

    /**
     * Closes the output stream after we finish writing.
     * 
     * @param writer The BufferedWriter object
     */
    private void closeOutputStream(final BufferedWriter writer) {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception ex) {
            System.out.println("Exception: LogStream.closeOutputStream: "
                              + ex.getMessage());
        }   
    }

    /**
     * Adds a new log message to the queue to be processed.
     * 
     * @param msg   The message
     * @param cls   The originating class
     * @param func  The originating function
     * 
     * @return A StringBuilder object
     */
    private StringBuilder queue(final String msg,
                                final String cls,
                                final String func) {
        Date date = new Date(System.currentTimeMillis());

        // $time  . ": " . $func . ": " . $msg ."\n"
        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append(new SimpleDateFormat("H:mm:ss").format(date));
    	msgBuilder.append(": ");
        if (!cls.isEmpty()) {
        	msgBuilder.append(cls);
        	msgBuilder.append("->");
        }
        if (!func.isEmpty()) {
            msgBuilder.append(func);
            msgBuilder.append("()");
            msgBuilder.append(" :: ");
        }
        msgBuilder.append(msg);

        try {
            queue.put(msgBuilder);
        } catch (InterruptedException e) {
            flush(new StringBuilder(e.toString()));
            flush(msgBuilder);
        }

        return msgBuilder;
    }
}
