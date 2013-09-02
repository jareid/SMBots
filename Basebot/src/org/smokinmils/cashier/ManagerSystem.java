/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
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
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for managers to login/logout and users to check
 * who is logged in.
 * 
 * @author Jamie
 */
public class ManagerSystem extends Event {
    /** The command used to check who is logged in. */
    public static final String         ON_CMD           = "!on";

    /** The command to login. */
    public static final String         LOGIN_CMD        = "!login";

    /** The command to logout. */
    public static final String         LOGOUT_CMD       = "!logout";

    /**The directory the times are stored. */
    private static final String         DIRNAME         = "settings";
    
    /** The file where the times are stored. */
    private static final String        FILENAME         = "managers.ini";

    /** Message when nobody is logged in. */
    private static final String        NOLOGGEDON          = "%b%c12[%c04Logged"
            + "In%c12] There are %c04no%c12 currently logged in managers.";

    /** Message when you check who is logged in. */
    private static final String        LOGGEDON            = "%b%c12[%c04Logged"
            + "In%c12] Currently logged in manager: %c04%who";

    /** Message when you try to log out but aren't logged in. */
    private static final String        NOTLOGGEDON         = "%b%c12[%c04Login"
            + "%c12]%c04 You are not currently logged in...";

    /** Message when you log in. */
    private static final String        LOGGEDIN            = "%b%c12[%c04Login"
            + "%c12]%c04 %who%c12, you have sucessfully been logged in!";

    /** Message when you log out. */
    private static final String        LOGGEDOUT           = "%b%c12[%c04Login"
            + "%c12]%c04 %who%c12, you have sucessfully been logged out!";

    /** Message when you log out for inactivity. */
    private static final String        INACTIVEDLOGGEDOUT  = "%b%c12[%c04Inacti"
            + "ve%c12]%c04 %who%c12, you have been logged out for inactivity in"
            + "%c04%actchan%c12!";

    /** Message when you can't log in. */
    private static final String        CANTLOGIN           = "%b%c12[%c04Login"
            + "%c12]%c04 %b%c04%who%c12 is currently logged in, please wait "
            + "until they finish their shift";

    /** The manager who is currently logged in. */
    private static String              loggedInUser;

    /** The map of times manager's have been logged in. */
    private static Map<String, Double> managerTimes;

    /** The amount of time user has been inactive. */
    private static int                 inactiveTime;

    /** The channel that is checked for inactivity. */
    private static String              activityChan;

    /** The channel the manager's can use commands. */
    private static String              managerChan;

    /** The channel the manager's can use commands. */
    private static String              hostChan;

    /** The IRC bot. */
    private static IrcBot              bot;

    /** Maximum minutes of inactivity. */
    private static final int           DEFAULTINACTIVETIME = 15;

    /** Timer used to perform checks every minute. */
    private static Timer               nextMin;

    /** Inactivity timer. */
    private static Timer               inactive;

    /**
     * Constructor.
     * 
     * @param activechan the channel that is monitored for activity.
     * @param managerchan the channel the commands can be used on.
     * @param hostchan the channel the commands can be used on.
     * @param irc The irc bot.
     */
    public ManagerSystem(final String activechan, final String managerchan,
                         final String hostchan, final IrcBot irc) {
        setLoggedInUser(null);
        managerTimes = new HashMap<String, Double>();
        inactiveTime = DEFAULTINACTIVETIME;
        activityChan = activechan;
        setManagerChan(managerchan);
        hostChan = hostchan;
        bot = irc;

        try {
            File dir = new File(DIRNAME);
            File inifile = new File(dir, FILENAME);
            if (inifile.exists()) {
                // read from the file
                Ini ini = new Ini(new FileReader(inifile));
                setLoggedInUser(ini.get("loggedin", "who"));
                Integer temp = ini.get("inactive", "maxtime", Integer.class);
                inactiveTime = DEFAULTINACTIVETIME;
                if (temp != null) {
                    inactiveTime = temp;
                }

                Ini.Section section = ini.get("times");
                if (section != null) {
                    for (String user : section.keySet()) {
                        Double val = section.get(user, Double.class);
                        if (val == null) {
                            val = 0.0;
                        }
                        managerTimes.put(user, val);
                    }
                }
            }
        } catch (IOException e) {
            EventLog.log(e, "ManagerSystem", "ManagerSystem");
        }

        nextMin = new Timer(true);
        nextMin.scheduleAtFixedRate(new IncreaseTime(), Utils.MS_IN_MIN, Utils.MS_IN_MIN);

        if (getLoggedInUser() != null) {
            inactive = new Timer(true);
            inactive.schedule(
                    new InactiveTask(), inactiveTime * Utils.MS_IN_MIN);
        }
    }

    /**
     * This method handles the manager commands.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot irc = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        if (irc.userIsIdentified(senderu)) {
            if (sender.equalsIgnoreCase(getLoggedInUser())
                    && chan.getName().equalsIgnoreCase(activityChan)) {
                inactive.cancel();
                inactive = new Timer(true);
                inactive.schedule(new InactiveTask(), inactiveTime * Utils.MS_IN_MIN);
            }

            if (isValidChannel(chan.getName()) && Utils.startsWith(message, ON_CMD)) {
                if (getLoggedInUser() == null) {
                    irc.sendIRCMessage(event.getChannel(), NOLOGGEDON);
                } else {
                    irc.sendIRCMessage(event.getChannel(),
                            LOGGEDON.replaceAll("%who", getLoggedInUser()));
                }
            } else if (Utils.startsWith(message, LOGIN_CMD)
                    && (getManagerChan().equalsIgnoreCase(chan.getName())
                        || (hostChan.equalsIgnoreCase(chan.getName())
                             && bot.userIsHalfOp(senderu, chan.getName())))) {
                if (getLoggedInUser() != null) {
                    irc.sendIRCNotice(senderu, CANTLOGIN.replaceAll("%who", getLoggedInUser()));
                } else {
                    managerLoggedIn(sender);
                    irc.sendIRCMessage(chan, LOGGEDIN.replaceAll("%who", sender));
                }
            } else if (Utils.startsWith(message, LOGOUT_CMD)
                    && (getManagerChan().equalsIgnoreCase(chan.getName())
                        || (hostChan.equalsIgnoreCase(chan.getName())
                             && bot.userIsHalfOp(senderu, chan.getName())))) {
                if (getLoggedInUser() == null || !getLoggedInUser().equalsIgnoreCase(sender)) {
                    irc.sendIRCNotice(senderu, NOTLOGGEDON);
                } else {
                    managerLoggedOut();
                    irc.sendIRCMessage(chan, LOGGEDOUT.replaceAll("%who", sender));
                }
            }
        }
    }

    /**
     * Manager has logged in.
     * 
     * @param who the user logging in.
     */
    private static void managerLoggedIn(final String who) {
        if (inactive != null) {
            inactive.cancel();
        }
        inactive = new Timer(true);
        inactive.schedule(new InactiveTask(), inactiveTime * Utils.MS_IN_MIN);
        setLoggedInUser(who);
    }

    /**
     * Manager has logged out.
     */
    private static void managerLoggedOut() {
        if (inactive != null) {
            inactive.cancel();
        }
        setLoggedInUser(null);
    }

    /**
     * Checks for inactivity every minute.
     */
    public static void inactive() {
        if (inactive != null) {
            inactive.cancel();
        }
        if (getLoggedInUser() != null) {
            String out = INACTIVEDLOGGEDOUT.replaceAll("%who", getLoggedInUser());
            out = out.replaceAll("%actchan", activityChan);
            Channel chan = bot.getUserChannelDao().getChannel(getManagerChan());
            bot.sendIRCMessage(chan, out);
            managerLoggedOut();
        }
    }

    /**
     * Logs the current manager times to the file.
     */
    public static void nextMinute() {
        checkData();
        String user = getLoggedInUser();
        if (user == null) {
            user = "NOBODY";
        }

        if (getLoggedInUser() != null) {
            Double current = managerTimes.get(user);
            if (current == null) {
                current = 0.0;
            }
            current += (1.0 / Utils.MIN_IN_HOUR);
            managerTimes.put(user, current);
            saveData();
        }
    }

    /**
     * Check's the manager file exists.
     */
    private static void checkData() {
        File dir = new File(DIRNAME);
        File inifile = new File(dir, FILENAME);
        if (!inifile.exists()) {
            managerTimes.clear();
            try {
                if (!inifile.createNewFile()) {
                    return;
                }
            } catch (IOException e) {
                EventLog.log(e, "ManagerSystem", "checkData");
            }
        }
    }

    /**
     * Saves the data to a file.
     */
    private static void saveData() {
        try {
            File dir = new File(DIRNAME);
            File inifile = new File(dir, FILENAME);
            Wini ini = new Wini(inifile);

            String liu = getLoggedInUser();
            if (liu == null || liu.equals("")) {
                liu = "NOBODY";
            }
            ini.put("loggedin", "who", liu);
            ini.put("inactive", "maxtime", inactiveTime);
            for (Entry<String, Double> entry : managerTimes.entrySet()) {
                ini.put("times", entry.getKey(), entry.getValue());
            }
            ini.store();
        } catch (IOException e) {
            EventLog.log(e, "ManagerSystem", "saveData");
        }
    }

    /**
     * @return the loggedInUser
     */
    public static String getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * @param user the loggedInUser to set
     */
    private static void setLoggedInUser(final String user) {
        ManagerSystem.loggedInUser = user;
    }

    /**
     * @return the managerChan
     */
    public static String getManagerChan() {
        return managerChan;
    }

    /**
     * @param mchan the managerChan to set
     */
    private static void setManagerChan(final String mchan) {
        managerChan = mchan;
    }
}

/**
 * Task to increase logged times every minute.
 * 
 * @author Jamie
 */
class IncreaseTime extends TimerTask {
    @Override
    public void run() {
        ManagerSystem.nextMinute();
    }
}

/**
 * Task to check inactivity every minute.
 * 
 * @author Jamie
 */
class InactiveTask extends TimerTask {
    @Override
    public void run() {
        ManagerSystem.inactive();
    }
}
