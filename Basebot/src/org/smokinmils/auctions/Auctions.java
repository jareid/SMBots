/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.auctions;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;

/**
 * Provides the functionality to payout a user some chips.
 * 
 * @author Jamie
 */
public class Auctions extends Event {
    /** The payout command. */
    public static final String  BIDCMD       = "!bid";

    /** The payout command format. */
    public static final String  PAYFMT        = "%b%c12" + BIDCMD + " <item_id>";
    
    /** The give command. */
    public static final String  ADDCMD     = "!additem";

    /** The give command format. */
    public static final String  ADDFMT      = "%b%c12" + ADDCMD + " <item> <length> <start_price>";

    /** The give command length. */
    public static final int     ADD_CMD_LEN     = 4;
    
    /** Additional time on bids. */
    private static final int ADDITIONAL_TIME = 30;

    /**
     * This method handles the auction commands.
     * 
     * @param event the Message event
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        
        if (isValidChannel(chan.getName()) && bot.userIsIdentified(sender)) {
            if (bot.userIsOp(sender, chan.getName()) && Utils.startsWith(message, ADDCMD)) {
                addItem(event);
            } else if (Utils.startsWith(message, BIDCMD)) {
                placeBid(event);
            }
        }
    }

    /**
     * This method handles the bid command.
     * 
     * @param event the Message event
     */
    private void placeBid(final Message event) {
        // TODO Auto-generated method stub
        
    }

    /**
     * This method handles the add item command.
     * 
     * @param event the Message event
     */
    private void addItem(final Message event) {
        // TODO Auto-generated method stub
        
    }
}
