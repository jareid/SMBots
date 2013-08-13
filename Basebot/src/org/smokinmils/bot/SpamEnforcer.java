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
    private final HashMap<String, HashMap<User, Long>> spamMap;
    
    /** how long between position commands. */
    private static final long POSITION_DELAY = 30 * Utils.MS_IN_MIN;
    
    /** Message informing of them about the limits on !position. */
    private static final String POSITION_MESSAGE = "You can only use the !position command every "
            + "30 minutes";
    
    /** the map of user->time for profile restricting. */
    private final HashMap<User, Long> positionMap;
    
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
        spamMap = new HashMap<String, HashMap<User, Long>>();
        positionMap = new HashMap<User, Long>();
    }
    
    /**
     * Adds a channel to the spam enforcement.
     * @param chan the channel to add to ENFORCEMENT!
     */
    public void add(final String chan) {
        spamMap.put(chan, new HashMap<User, Long>());
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
        if (spamMap.containsKey(chan)) {
            HashMap<User, Long> thisChannel = spamMap.get(chan);
            if (thisChannel.containsKey(user)) {

               if (System.currentTimeMillis() - thisChannel.get(user) < DELAY) {
                // NAY
                   ret = false;
                   String out = MESSAGE.replaceAll("%chan", fastchan);
                   bot.sendIRCNotice(user, out);
               } else {
                // YAY
                   thisChannel.put(user, System.currentTimeMillis());
                   spamMap.put(chan, thisChannel);
               }
            } else {
                //first time so YAY
                thisChannel.put(user, System.currentTimeMillis());
                spamMap.put(chan, thisChannel);
            }
        }
        
        return ret;
    }
    
    /**
     * Checks if a user can issue the position.
     * @param event the irc event to derive channel user etc from
     * @return yay if they can issue position, or nay otherwise
     */
    public boolean checkPosition(final Message event) {
        User user = event.getUser();
        IrcBot bot = event.getBot();
        boolean ret = true;
        Long now = System.currentTimeMillis();
        if (positionMap.containsKey(user)) {
           
           if (now - positionMap.get(user) < POSITION_DELAY) {
            // NAY
               ret = false;
               bot.sendIRCNotice(user, POSITION_MESSAGE);
           } else {
            // YAY
               positionMap.put(user, now);
           }
        } else {
            //first time so... YAY
            positionMap.put(user, now);
        }
        
        
        return ret;
    }
}
