package org.smokinmils.games;

import java.util.ArrayList;
import java.util.HashMap;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.BaseBot;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.commands.UserCommands;
import org.smokinmils.games.casino.DiceDuel;
import org.smokinmils.games.casino.OverUnder;
import org.smokinmils.games.casino.Roulette;
import org.smokinmils.games.casino.blackjack.BJGame;
import org.smokinmils.games.rockpaperscissors.RPSGame;
import org.smokinmils.games.rpg.Duel;

/**
 * Allows managers to invite the bot into a channel with a certain game.
 * @author cjc
 *
 */
public class Invite extends Event {

    /** the command to invite. */
    private static final String INVITE_CMD = "!invite";
    
    /** The command to uninvite. */
    private static final String UNINVITE_CMD = "!uninvite";
    
    /** The length of the uninvite command. */
    private static final int UNINVITE_LEN = 2;
    
    /** The format of the uninvite command. */
    private static final String UNINVITE_FMT = UNINVITE_CMD + " <channel>";
    
    /** string representation for roulette . */
    private static final String ROULETTE = "roulette";
    
    /** string representation for dice duel. */
    private static final String DD = "dd";
    
    /** string representation for dice duel. */
    private static final String DUEL = "duel";
    
    /** string representation for overunder. */
    private static final String OU = "ou";
    
    /** string representation for rockpaperscissors. */
    private static final String RPS = "rps";
    
    /** string representation for blackjack. */
    private static final String BJ = "bj";

    /** length of the roulette command. */
    private static final int ROULETTE_CMD_LENGTH = 4;
    
    /** length of the normal command (ie not roulette). */
    private static final int CMD_LENGTH = 3;

    /** String to let the user know the game has been placed in the channel. */
    private static final String INVITE_DONE = "%b%c04%who%c12: %game has been started in %chan";
    
    /** String to let the user know that the game is already present in the channel. */
    private static final String INVITE_NOPE = "%b%c04%who%c12: %game is already in %chan";
    
    /** String to let the user know that the bot has left #chan. */
    private static final String UNINVITE_DONE = "%b%c04%who%c12: All games removed from %chan";
    
    /** Instructions for roulette. */
    private static final String INVALID_COMMAND_ROULETTE = "%b%c04%who%c12: To invite a roulette " 
    + "game you must use !invite #chan roulette x, where x is the delay for the roulette rounds";

    /** Instructions for other games. */
    private static final String INVALID_COMMAND = "%b%c04%who%c12: To invite a " 
                    + "game you must use !invite #chan <game>";
    
    /** Somewhere to store all the listeners we add, so we can remove them so the bot will
     * actually leave a channel.
     * Map of Channel -> Arraylist of events
     */
    private final HashMap<String, ArrayList<Event>> tempListeners;
    
    /** Keep track of the games that have been invited for dupe checking. 
     * TODO Neater with single map?
    */
    private final HashMap<String, ArrayList<String>> tempGames;
    
    
    /**
     * Constructor.
     */
    public Invite() {
        tempListeners = new HashMap<String, ArrayList<Event>>();
        tempGames = new HashMap<String, ArrayList<String>>();
    }
    
    /* (non-Javadoc)
     * @see org.smokinmils.bot.Event#message(org.smokinmils.bot.events.Message)
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        
        if (isValidChannel(chan.getName())
                && bot.userIsIdentified(sender)
                && bot.userIsOp(sender, chan.getName())) {
            if (Utils.startsWith(message, INVITE_CMD)) {
                doInvite(event); 
            } else if (Utils.startsWith(message, UNINVITE_CMD)) {
                doUninvite(event);
            }
        }
    }
    
    /**
     * Handles the invite command.
     * @param event the event
     */
    private void doInvite(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        String[] msg = message.split(" ");
        
        String newchannel = msg[1].toLowerCase();
        BaseBot bb = BaseBot.getInstance();
        String game = msg[2].toLowerCase();
        
        // assuming adding
        boolean adding = true;
        
        // check if said game is already in the channel
        if (tempGames.containsKey(newchannel)) {
            ArrayList<String> channels = tempGames.get(newchannel);
            if (channels.contains(game)) {
                adding = false;
            }
        }

        if (adding) { 
               if (msg.length < CMD_LENGTH) {
                   String out = INVALID_COMMAND.replaceAll("%who", sender.getNick());
                   bot.sendIRCMessage(chan, out);
               } else {
                   // valid length 
                   Event listener = null;
                   if (game.equals(ROULETTE)) { // roulette is different due to delay arg
                       if (msg.length < ROULETTE_CMD_LENGTH) {
                           String out = INVALID_COMMAND_ROULETTE.replaceAll("%who", 
                                   sender.getNick());
                           bot.sendIRCMessage(chan, out); 
                       } else {
                           int rdelay = Utils.tryParse(msg[1 + 1 + 1]);
                           
                           if (rdelay > 0) {
                               listener = new Roulette(rdelay, newchannel, bot);
                               listener.addValidChan(newchannel);
                               bb.addListener(bot.getServer(), listener);
                           } else {
                               String out = INVALID_COMMAND_ROULETTE.replaceAll("%who", 
                                       sender.getNick());
                               bot.sendIRCMessage(chan, out);   
                           }
                       }
                   } else if (game.equals(DD)) {
                       listener = new DiceDuel(bot, newchannel);
                       listener.addValidChan(newchannel);
                       bb.addListener(bot.getServer(), listener);
                   } else if (game.equals(DUEL)) {
                       listener = new Duel(bot, newchannel);
                       listener.addValidChan(newchannel);
                       bb.addListener(bot.getServer(), listener);
                   } else if (game.equals(OU)) {
                       listener = new OverUnder();
                       listener.addValidChan(newchannel);
                       bb.addListener(bot.getServer(), listener);
                   } else if (game.equals(RPS)) {
                       listener = new RPSGame();
                       listener.addValidChan(newchannel);
                       ((RPSGame) listener).addAnnounce(newchannel, bot);
                       bb.addListener(bot.getServer(), listener);
                   } else if (game.equals(BJ)) {
                       listener = new BJGame(bot);
                       listener.addValidChan(newchannel);
                       bb.addListener(bot.getServer(), listener);
                   } else {
                       String out = INVALID_COMMAND.replaceAll("%who", sender.getNick());
                       bot.sendIRCMessage(chan, out); 
                   }
                   
                   // add user commands if we are adding and we are not already in that channel
                   if (listener != null && !bot.getValidChannels().contains(newchannel)) {
                       bb.addChannel(bot.getServer(), newchannel); 
                       //add in harmless commands for basic stuffs.
                       UserCommands uc = new UserCommands();
                       uc.addValidChan(newchannel);
                       bb.addListener(bot.getServer(), uc);
                       if (tempListeners.containsKey(newchannel)) {
                           ArrayList<Event> listeners = tempListeners.get(newchannel);
                           listeners.add(uc);
                       } else {
                           ArrayList<Event> listeners = new ArrayList<Event>();
                           listeners.add(uc);
                           tempListeners.put(newchannel, listeners);
                       }
                   }
                   
                   // keep track of the listeners for uninviting
                   if (listener != null) {
                      if (tempListeners.containsKey(newchannel)) {
                          ArrayList<Event> listeners = tempListeners.get(newchannel);
                          listeners.add(listener);
                      } else {
                          ArrayList<Event> listeners = new ArrayList<Event>();
                          listeners.add(listener);
                          tempListeners.put(newchannel, listeners);
                      }
                      // add to gameListeners to easily check for dupes
                      if (tempGames.containsKey(newchannel)) {
                          ArrayList<String> channels = tempGames.get(newchannel);
                          channels.add(game);
                      } else {
                          ArrayList<String> channels = new ArrayList<String>();
                          channels.add(game);
                          tempGames.put(newchannel, channels);
                      }
                      
                      // all done in the hood
                      String out = INVITE_DONE.replaceAll("%who", sender.getNick());
                      out = out.replaceAll("%game", game);
                      out = out.replaceAll("%chan", newchannel);
                      bot.sendIRCMessage(chan, out);
                   }
               }
            //}
        } else { // game already in chan
            String out = INVITE_NOPE.replaceAll("%who", sender.getNick());
            out = out.replaceAll("%game", game);
            out = out.replaceAll("%chan", newchannel);
            bot.sendIRCMessage(chan, out);
        }
    }
    
    /**
     * Handles the uninvite command.
     * @param event the event from what which we get everything
     */
    private void doUninvite(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        String[] msg = message.split(" ");
        String channel = msg[1];
        
        if (msg.length < UNINVITE_LEN) {
            bot.invalidArguments(sender, UNINVITE_FMT);
        } else if (tempListeners.containsKey(channel)) {
            ArrayList<Event> listeners = tempListeners.get(channel);
            for (Event e : listeners) {
                bot.getListenerManager().removeListener(e);
            }
            Channel chan = bot.getUserChannelDao().getChannel(channel);
            chan.send().part();
            
            // clean up
            bot.delValidChannel(channel);
            tempListeners.remove(channel);
            tempGames.remove(channel);
            
            String out = UNINVITE_DONE.replaceAll("%who", sender.getNick());
            out = out.replaceAll("%chan", channel);
            bot.sendIRCMessage(event.getChannel(), out);
            
        } 
    }
}
