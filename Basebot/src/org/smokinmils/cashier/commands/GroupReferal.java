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
import java.util.ArrayList;
import java.util.List;

import org.pircbotx.Channel;
import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.DBException;
import org.smokinmils.database.types.ReferrerType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for rank referals
 * 
 * @author Jamie
 */
public class GroupReferal extends Event {
	private static final String Command = "!grefer";
    private static final String Format = "%b%c12" + Command + " <user> <referrers>";
    
    private static final String NOT_RANKED = "%b%c04%sender%c12: %c04%who%c12 is currently not a member of any rank group.";
    private static final String NO_USER = "%c04%sender%c12: %c04%who%c12 does not exist as a user.";
    private static final String NO_SELF = "%c04%sender%c12: You can not be your own referrer.";
    private static final String SUCCESS = "%c04%sender%c12: Succesfully added %c04%referrers%c12 as %c04%who%c12's referer(s).";
    private static final String FAILED = "%c04%sender%c12: %c04%who%c12 is a public referral and can't be given to ranks.";
    
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
                EventLog.log(e, "GroupReferral", "message");
            }
		}
	}
	
	private void refer(Message event) throws DBException, SQLException {
        IrcBot bot = event.getBot();
	    String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();
        
	    if (msg.length >= 3) {
	        DB db = DB.getInstance();
            String user = msg[1];
            ReferrerType reftype = db.getRefererType(user);
            if (reftype == ReferrerType.NONE || reftype == ReferrerType.GROUP) {
                List<String> refs = new ArrayList<String>();
                boolean is_ok = true;
                for (int i = 2; i < msg.length; i++) {
                    String ref = msg[i];
                    if ( ref.equalsIgnoreCase( user ) ) {
                        String out = NO_SELF.replaceAll("%sender", sender);
                        bot.sendIRCMessage(channel, out);
                        
                        is_ok = false;
                    } else if ( !db.checkUserExists( ref ) ) {
                        String out = NO_USER.replaceAll("%sender", sender);
                        out = out.replaceAll("%who", ref);
                        bot.sendIRCMessage(channel, out);
                        
                        is_ok = false;
                    } else if ( !db.isRank( ref ) ) {
                        String out = NOT_RANKED.replaceAll("%sender", sender);
                        out = out.replaceAll("%who", ref);
                        bot.sendIRCMessage(channel, out);
                        
                        is_ok = false;
                    }
                    
                    // break from the loop if we had a problem.
                    if (!is_ok) break;
                }
                
                if (is_ok) {
                    for (String referrer: refs) {
                        db.addReferer(user, referrer);
                    }
                    String out = SUCCESS.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", user);
                    out = out.replaceAll("%referrers", Utils.ListToString(refs));
                    bot.sendIRCMessage(channel, out);
                }
            } else {
                String out = FAILED.replaceAll("%sender", sender);
                out = out.replaceAll("%who", user);
                bot.sendIRCMessage(channel, out);
            }
	    } else {
            bot.invalidArguments( sender, Format );
	    }
	}
}