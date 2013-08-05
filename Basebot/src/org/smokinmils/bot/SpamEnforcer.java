package org.smokinmils.bot;

import java.util.HashMap;

import org.pircbotx.User;
import org.smokinmils.bot.events.Message;

/**
 * Class that maintains last time commands have been used to stop people form spamming in a
 * specified channel.
 * @author cjc
 *
 */
public final class SpamEnforcer {

    /** How long between bets can we do? */
    private static final long DELAY = 5 * Utils.MS_IN_SEC;

    /** Message to tell the user to go to the other channel. */
    private static final String MESSAGE = "You can only use a command once every 5 seconds here. "
    		                    + "For faster play join %chan";
    
    /** the list of Channel -> (User -> Time last used a command). */
    private HashMap<String, HashMap<User, Long>> theList;
    
    /** Instance variable. */
    private static SpamEnforcer instance;
    static {
        try {
            instance = new SpamEnforcer();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Static 'instance' method.
     * 
     * @return the singleton Instance
     */
    public static SpamEnforcer getInstance() {
        return instance;
    }
    

    /**
     * Constructor.
     */
    private SpamEnforcer() {
       //HAX TODO XMLerize this?
        theList = new HashMap<String, HashMap<User, Long>>();
    }
    
    /**
     * Adds a channel to the spam enforcement.
     * @param chan the channel to add to ENFORCEMENT!
     */
    public void add(final String chan) {
        theList.put(chan, new HashMap<User, Long>());
    }
    /**
     * Checks if a user can issue a command or not.
     * @param event the ircevent to derive chan user etc from
     * @param fastchan the string representation of the channel used for faster play
     * @return yay if they can issue a command, or nay otherwise
     */
    public boolean check(final Message event,
                               final String fastchan) {
        String chan = event.getChannel().getName();
        User user = event.getUser();
        IrcBot bot = event.getBot();
        
        boolean ret = true;
        // check if channel exists
        if (theList.containsKey(chan)) {
            HashMap<User, Long> thisChannel = theList.get(chan);
            if (thisChannel.containsKey(user)) {
               if (System.currentTimeMillis() - thisChannel.get(user) >= DELAY) {
                  //YAY
                  thisChannel.put(user, System.currentTimeMillis());
               } else {
                   // NAY
                   ret = false;
                   String out = MESSAGE.replaceAll("%chan", fastchan);
                   bot.sendIRCNotice(user, out);
               }
            } else {
                //first time so YAY
                thisChannel.put(user, System.currentTimeMillis());
            }
        }
        return ret;
    }
}
