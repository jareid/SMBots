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
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.text.SimpleDateFormat;

public class Logstream implements Runnable {
    private final String _path;         // The root path we will use for logging
    private final BlockingQueue<StringBuilder> _queue; // Used to queue up messages for logging
    private final Thread _writeThread;  // Thread to run this logstream on

    private int _dayOfMonth = -1;       // The current day of the month
    private String _cachedPath = "";    // Our log path.  Changes for each day of the month

   protected Logstream(BlockingQueue<StringBuilder> queue, String rootPath) {
        _path  = rootPath;

        _queue = queue;  // LinkedBlockingQueue<StringBuilder>();

        _writeThread = new Thread(this);
        _writeThread.start();
   }

    public void write(String msg, String cls, String func)
    {
        queue(msg, cls, func);      
    }

    public void run()
    {
        // logging never stops unless we restart the entire server so just loop forever
        while(true) {
            try {
                StringBuilder builder = _queue.take();  
                flush(builder);
                Thread.sleep(10);
            } catch(InterruptedException ex) {
                flush(ex.toString());
                System.out.println("Exception: LogStream.run: " + ex.getMessage());
            }
        }
    }

    private void flush(StringBuilder builder) { 
        flush(builder.toString());
    }

    private void flush(String data) {

        BufferedWriter writer = null;

        try {

            System.out.println(data);

            writer = getOutputStream();

            writer.write(data);
            writer.newLine();
            writer.flush();             
        }
        catch(IOException ex) {
            // what to do if we can't even log to our log file????
            System.out.println("IOException: EventLog.flush: " + ex.getMessage());
        }
        finally {
            closeOutputStream(writer);
        }
    }

    private boolean dayOfMonthHasChanged(Calendar calendar) {
        return calendar.get(Calendar.DAY_OF_MONTH) != _dayOfMonth;
    }

    private String getPath() {

        Calendar calendar = Calendar.getInstance();

        if(dayOfMonthHasChanged(calendar)) {

            StringBuilder pathBuilder = new StringBuilder();
            SimpleDateFormat df = new SimpleDateFormat("H_mm.dd-MMM-yy");
            Date date = new Date();

            _dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            pathBuilder.append(_path);
            pathBuilder.append(df.format(date));
            pathBuilder.append(".log");

            _cachedPath = pathBuilder.toString();       
        }

        return _cachedPath;
    }

    private BufferedWriter getOutputStream() throws IOException {
        return new BufferedWriter(new FileWriter(getPath(), true));         
    }

    private void closeOutputStream(BufferedWriter writer) {
        try {
            if(writer != null) {
                writer.close();
            }
        }
        catch(Exception ex) {
            System.out.println("Exception: LogStream.closeOutputStream: " + ex.getMessage());
        }   
    }

    private StringBuilder queue(String msg, String cls, String func) {
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
            _queue.put(msgBuilder);
        }
        catch (InterruptedException e) {
            flush(new StringBuilder(e.toString()));
            flush(msgBuilder);
        }

        return msgBuilder;
    }
}
