/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.pircbotx.User;
import org.pircbotx.hooks.WaitForQueue;
import org.pircbotx.hooks.events.NoticeEvent;
import org.smokinmils.bot.events.Action;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Kick;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.NickChange;
import org.smokinmils.bot.events.Part;
import org.smokinmils.bot.events.PrivateMessage;
import org.smokinmils.bot.events.Quit;
import org.smokinmils.bot.events.UserList;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.UserCheck;
import org.smokinmils.games.timedrollcomp.TimedRollComp;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to automatically verify users.
 * Also, gives a player play chips if they don't exist
 * 
 * @author Jamie
 */
public class CheckIdentified extends Event {    
    /** A list of users already sent a no identification message. */
    private static List<String> sentNoIdent = new ArrayList<String>();
    
    /**
     * This string is output when the user does not meet the above status
     * with NickServ.
     */
    public static final String NOT_IDENTIFIED_MSG = 
                        "%b%c12You must be identified with %c04NickServ%c12"
                                + " to use the bot commands";
    
    /**
     * This string is output when the user is not identified by NICKSERV.
     */
    public static final String NOT_IDENTIFIED = "%b%c12Sorry, %c04%user%c12 is"
                                              + " not current identified.";
    
    /**
     * This string is output when the user has just been created.
     */
    public static final String WELCOME_MSG = "%b%c01Hello %c04%user%c01, "
            + "welcome to our channel! For information on our games and "
            + "systems please type %c04!info%c01 and use further commands "
            + "for specific topics listed. If you need any help just ask in "
            + "the channel. We hope that you enjoy your stay!";
    
    /** Contains a separate thread to process identification checks. */
    private final CheckUserQueue checkThread;
    
    /** Tells us whether to enable automatic checks or not. */
    private final boolean isEnabled;
    
    /**
     * Constructor.
     * 
     * @param bot The irc bot for this server's ident checks
     * @param enabled If auto checks are on or not.
     * 
     * @see Event
     */
    public CheckIdentified(final IrcBot bot, final boolean enabled) {
        super();
        checkThread = new CheckUserQueue(bot);
        isEnabled = enabled;
    }
    
    @Override
    public final void action(final Action event) {
        IrcBot bot = event.getBot();
        User user = event.getUser();
        if (!bot.userIsIdentified(user)) {
            checkThread.addUser(user);
        }
    }
    
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        User user = event.getUser();
        if (!bot.userIsIdentified(user)
                && event.getMessage().startsWith("!")
                && !event.getMessage().startsWith(TimedRollComp.CMD)) {
            checkThread.addUser(user);
            // If we already told this user, don't tell them again
            if (!sentNoIdent.contains(user.getNick())) {
                bot.sendIRCNotice(user, NOT_IDENTIFIED_MSG);
                sentNoIdent.add(user.getNick());
            }
        }
    }
    
    @Override
    public final void privateMessage(final PrivateMessage event) {
        IrcBot bot = event.getBot();
        User user = event.getUser();
        if (!bot.userIsIdentified(user)) {
            checkThread.addUser(user);
        }
    }
    
    @Override
    public final void join(final Join event) {
        User user = event.getUser();
        String nick = user.getNick();
        // (Re-)Check the user's status with NickServ
        if (!nick.equalsIgnoreCase(event.getBot().getNick())
                && !nick.equalsIgnoreCase("X")) {
            event.getBot().removeIdentifiedUser(user.getNick());
            checkThread.addUser(user);
        }
    }
    
    @Override
    public final void nickChange(final NickChange event) {
        event.getBot().removeIdentifiedUser(event.getOldNick());
        checkThread.addUser(event.getUser());
    }
    
    @Override
    public final void part(final Part event) {
        event.getBot().removeIdentifiedUser(event.getUser().getNick());
    }
    
    @Override
    public final void kick(final Kick event) {
        event.getBot().removeIdentifiedUser(event.getRecipient().getNick());
    }   
    
    @Override
    public final void quit(final Quit event) {
        event.getBot().removeIdentifiedUser(event.getUser().getNick());
    }
    
    @Override
    public final void userList(final UserList event) {
        IrcBot bot = event.getBot();
        // TODO: fix concurrency issue here
        for (User usr: event.getUsers()) {
            String nick = usr.getNick();
            if (!nick.equalsIgnoreCase(bot.getNick()) 
                    && !nick.equalsIgnoreCase("X")
                    && !bot.userIsIdentified(usr)) {
                bot.removeIdentifiedUser(usr.getNick());
                checkThread.addUser(usr);
            }
        }
    }
    
    /**
     * Sends a request to NickServ to check a user's status with the server
     * and waits for the response.
     * 
     * @param user the username
     * 
     * @return true if the user meets the required status
     */
    public final boolean manualStatusRequest(final User user) {
        Boolean ident = checkThread.checkIdentified(user);
        boolean ret = false;
        if (ident) {
            ret = true;
        }
        return ret;
    }
    
    /**
     * A separate class that is used to process identification checks
     * 
     * This is a thread safe class that processes events using a queue system.
     * 
     * @author Jamie
     */
    final class CheckUserQueue extends Thread {
        /** A queue of events users to be checked. */
        private final Deque<User> users;
        
        /** The IRC bot used to check. */
        private final IrcBot bot;
        
        /** A map that stores when this user last requested an ident check. */
        //private final Map<User, Long> userMap;
        // TODO: reimplement for performance.
        
        /** The number of milliseconds to wait for network services responses.*/
        private static final int WAIT_MS = 3500;
        
        /** A message for when the network services don't respond. */
        private static final String NO_SERVICES = "%b%c12The IRC Network "
                + "Services appear to be down, please try again shortly.";

        /** A message for when a user already has an account. */
        private static final String TOO_MANY_ACCS = "%b%c12You have too many "
                 + "accounts, speak to an admin if there is a problem";

        /**
         * Constructor.
         * 
         * @param irc the bot used for these checks
         */
        private CheckUserQueue(final IrcBot irc) {
            users = new ArrayDeque<User>();
            //userMap = new HashMap<User, Long>();
            bot = irc;
            this.start();
        }
        
        /**
         * Processes requests in order they arrived.
         * 
         * (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            boolean interuptted = false;
            while (!(Thread.interrupted() || interuptted)) { 
                if (!users.isEmpty()) {
                    User user = users.removeFirst();
                    sendStatusRequest(user);
                }
                
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    interuptted = true;
                }
            }
            return;
        }
        
        /**
         * Sends a request to NickServ to check a user's status with the server
         * and waits for the response.
         * 
         * @param user the username
         * 
         * @return true if the user meets the required status
         */
        private Boolean checkIdentified(final User user) {
            Boolean ret = null;
            ExecutorService executor = Executors.newFixedThreadPool(1);
            FutureTask<Boolean> choicetask =
                    new FutureTask<Boolean>(new CheckUser(bot, user));
            executor.execute(choicetask);
            try {
                ret = choicetask.get(WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // Do nothing, we expect this.
                ret = null;
            } catch (InterruptedException | ExecutionException e) {
                EventLog.log(e, "CheckIdentified", "checkIdentified");
            }   
            executor.shutdown();
            
            return ret;
        }
        
        /**
         * Sends a request to NickServ to check a user's status with the server
         * and waits for the response.
         * 
         * @param user     The user
         * 
         * @return true if successful, false if not identified, null if failed.
         */
        private Boolean sendStatusRequest(final User user) {
            Boolean identd = checkIdentified(user);
            // Only add users with the correct levels
            if (identd == null) {
                bot.sendIRCNotice(user, NO_SERVICES);
            } else if (identd) {
                EventLog.info(user.getNick() + " identified", "CheckIdentified",
                              "sendStatusRequest");
                sentNoIdent.remove(user);
                try {
                    UserCheck res = DB.getInstance().checkUserExists(user.getNick(),
                                                                     user.getHostmask());
                    if (res == UserCheck.FAILED) {
                        bot.sendIRCNotice(user, TOO_MANY_ACCS);
                    } else {
                        bot.addIdentifiedUser(user);
                        if (res == UserCheck.CREATED) {
                            String out = WELCOME_MSG.replaceAll("%user",
                                                                user.getNick());
                            bot.sendIRCNotice(user, out);
                        }
                    }
                } catch (Exception e) {
                    EventLog.log(e, "CheckIdentified", "sendStatusRequest");
                }
            }
            return identd;
        }
        
        /**
         * Add a user to the queue for identification checks.
         * 
         * @param user The user to add
         * 
         * @see org.pircbotx.User
         */
        public void addUser(final User user) {
            if (isEnabled) {
                //Long time = UserMap.get(user);
                //Long now = System.currentTimeMillis();
                // Only check users if they are not awaiting a check
                // Or they haven't been checked in the last three seconds
                // && !(time != null && (time-now) < 3000)
                List<String> restrict = new ArrayList<String>();
                restrict.add("HOUSE");
                restrict.add("POINTS");
                if (!restrict.contains(user.getNick().toUpperCase()) && !users.contains(user)) {
                    users.addLast(user);
                    //userMap.put(user, now);
                }
            }
        }
    }
}


/**
 * This class is used to time limit IDENT checks with the Services.
 * 
 * @author Jamie
 */
class CheckUser implements Callable<Boolean> {
    /** A string to store the response from a nickserv status command. */
    private static final String NICKSERV_STATUS = "STATUS";
    
    /** A string to store username of nickserv. */
    private static final String NICKSERV = "NickServ";
    
    /** The required nickserv status value. */
    private static final int STATUS_REQD = 3;
    
    /** The length of the response. */
    private static final int STATUS_RESP_LEN = 3;
    
    /** The IRC Bot that this check is being performed on. */
    private final IrcBot bot;
    
    /** The user to check. */
    private final User user;
    
    /**
     * Constructor.
     * 
     * @param irc   The irc bot
     * @param usr  The user to check
     */
    public CheckUser(final IrcBot irc, final User usr) {
        bot = irc;
        user = usr;
    }


    /**
     *  The call that checks a user's identification status.
     *  
     *  @return true if they are identified, false if they aren't and null if
     *           the check failed.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Boolean call() {
        EventLog.debug("Checking the status of " + user.getNick(),
                "CheckUser::CheckIdentified", "call");
        
        WaitForQueue queue = new WaitForQueue(bot);
        bot.sendRaw().rawLine("PRIVMSG NickServ " + NICKSERV_STATUS + " " + user.getNick());
        
        boolean received = false;
        Boolean ret = false;
        //Infinite loop since we might receive notices from non NickServ
        while (!received) {
            //Use the waitFor() method to wait for a MessageEvent.
            //This will block (wait) until a message event comes in, ignoring
            //everything else     
            NoticeEvent<IrcBot> currentEvent = null;
            try {
                currentEvent = queue.waitFor(NoticeEvent.class);
            } catch (InterruptedException ex) {
                EventLog.log(ex, "CheckUser::CheckIdentified", "call");
            }
            
            //Check if this message is the response
            String[] msg = currentEvent.getMessage().split(" ");
            if (Utils.startsWith(currentEvent.getMessage(), NICKSERV_STATUS)
                && currentEvent.getUser().getNick().equalsIgnoreCase(NICKSERV)
                 && msg.length == STATUS_RESP_LEN
                 && msg[1].equalsIgnoreCase(user.getNick())) {
                Integer code = Utils.tryParse(msg[2]);
                
                // Only add users with the correct levels
                if (code >= STATUS_REQD) {
                    EventLog.info(user.getNick() + " identified",
                                          "CheckUser::CheckIdentified", "call");
                    ret = true;
                }
                queue.close();
                received = true;
            }
        }
        return ret;
    }
}
