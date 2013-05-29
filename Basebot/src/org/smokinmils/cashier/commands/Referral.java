/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier.commands;

import java.sql.SQLException;

import org.pircbotx.Channel;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.DBException;
import org.smokinmils.database.types.ReferrerType;
import org.smokinmils.logging.EventLog;
/**
 * Provides the functionality for public referals
 * 
 * @author Jamie
 */
public class Referral extends Event {
    private static final String Command = "!refer";
    private static final String Format = "%b%c12" + Command + " <user>";
	
    private static final String NO_USER = "%c04%sender%c12: %c04%who%c12 does not exist as a user.";
    private static final String NO_SELF = "%c04%sender%c12: You can not be your own referrer.";
    private static final String SUCCESS = "%c04%sender%c12: Succesfully added %c04%who%c12 as your referer.";
    private static final String FAILED = "%c04%sender%c12: You already have a referrer.";
    
	/**
	 * This method handles the chips command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */
	@Override
	public void message(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		Channel chan = event.getChannel();
		
		if ( message.toLowerCase().startsWith( Command ) &&
		     bot.userIsIdentified( sender ) &&
		     isValidChannel( chan.getName() )) {
		    try {
		        refer(event);
		    } catch (Exception e) {
		        EventLog.log(e, "Referral", "message");
		    }
		}
	}
	
	private void refer(Message event) throws DBException, SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();
        
        if (msg.length == 2) {
            DB db = DB.getInstance();
            ReferrerType reftype = db.getRefererType(sender);
            if (reftype == ReferrerType.NONE) {
                String referrer = msg[1];
                if ( referrer.equalsIgnoreCase( sender ) ) {
                    String out = NO_SELF.replaceAll("%sender", sender);
                    bot.sendIRCMessage(channel, out);
                } else if ( !db.checkUserExists( referrer ) ) {
                    String out = NO_USER.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", referrer);
                    bot.sendIRCMessage(channel, out);
                } else {
                    db.addReferer(sender, referrer);
                    String out = SUCCESS.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", sender);
                    bot.sendIRCMessage(channel, out);
                }
            } else {
                String out = FAILED.replaceAll("%sender", sender);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments( sender, Format );
        }
	}
}