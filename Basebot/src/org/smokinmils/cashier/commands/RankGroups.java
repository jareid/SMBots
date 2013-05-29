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
import org.smokinmils.logging.EventLog;
/**
 * Provides the functionality for managing the rank groups
 * 
 * @author Jamie
 */
public class RankGroups extends Event {
	private static final String RenCommand = "!rengroup";
	private static final String RenFormat = "%b%c12" + RenCommand + " <oldname> <newname>";
	
	private static final String NewCommand = "!newgroup";
    private static final String NewFormat = "%b%c12" + NewCommand + " <name>";
    
	private static final String DelCommand = "!delgroup";
    private static final String DelFormat = "%b%c12" + DelCommand + " <group>";
    
	private static final String AddCommand = "!rankadd";
    private static final String AddFormat = "%b%c12" + AddCommand + " <user> <group>";
    
	private static final String KikCommand = "!rankkick";
    private static final String KikFormat = "%b%c12" + KikCommand + " <user>";
    
    private static final String NOT_RANKED = "%b%c04%who%c12 is currently not a member of any rank group.";
    private static final String NO_USER = "%b%c04%who%c12 does not exist as a user.";
    private static final String KICKED = "%b%c04%who%c12 has beened kicked from the %c04%group%c12 rank group.";
    private static final String ADDED = "%b%c04%who%c12 has beened added to the %c04%group%c12 rank group.";
    private static final String MOVED = "%b%c04%who%c12 has beened moved from %c04%oldgroup%c12 to %c04%group%c12.";
    private static final String NO_GROUP = "%b%c04%group%c12 does not exist as a rank group.";
    private static final String GROUP_EXISTS = "%b%c04%group%c12 already exists as a rank group.";
    private static final String GROUP_CREATED = "%b%c04%group%c12 rank group has been created.";
    private static final String GROUP_DELETED = "%b%c04%group%c12 rank group has been deleted.";
    private static final String GROUP_RENAMED = "%b%c04%oldgroup%c12 rank group has been renamed to %c04%newgroup%c12.";
    
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
		
		if ( bot.userIsIdentified( sender ) && isValidChannel( chan.getName() )) {
		    try {
    			if (message.toLowerCase().startsWith( RenCommand )) {
    			    renameGroup(event);
    			} else if (message.toLowerCase().startsWith( NewCommand )) {
    			    newGroup(event);
                } else if (message.toLowerCase().startsWith( DelCommand )) {
                    deleteGroup(event);
                } else if (message.toLowerCase().startsWith( AddCommand )) {
                    addRank(event);
                } else if (message.toLowerCase().startsWith( KikCommand )) {
                    kickRank(event);
                }
		    } catch (Exception e) {
                EventLog.log(e, "RankGroups", "message");
		    }
		}
	}

    private void kickRank(Message event) throws DBException, SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();
        
        if (msg.length == 2) {
            DB db = DB.getInstance();
            String who = msg[1];
            if ( !db.checkUserExists(who) ) {
               bot.sendIRCMessage(channel, NO_USER.replaceAll("%who",who));
            } else if ( !db.isRank( who ) ) {
                bot.sendIRCMessage(channel, NOT_RANKED.replaceAll("%who",who));
            } else {
                String group = db.getRankGroup(who);
                db.kickRank(sender);
                
                String out = KICKED.replaceAll("%who", who);
                out = out.replaceAll("%group", group);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments( sender, KikFormat );
        }
    }

    private void addRank(Message event) throws DBException, SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();
        
        if (msg.length == 3) {
            DB db = DB.getInstance();
            String who = msg[1];
            String group = msg[2];
            if ( !db.checkUserExists(who) ) {
                bot.sendIRCMessage(channel, NO_USER.replaceAll("%who",who));
             } else if ( !db.isRank( who ) ) {
                 bot.sendIRCMessage(channel, NOT_RANKED.replaceAll("%who",who));
            } else {
                String out = null;
                if ( db.isRank( who ) ) {
                    String oldgroup = db.getRankGroup(who);
                    out = MOVED.replaceAll("%oldgroup", oldgroup);                    
                    db.updateRank(who, group);
                } else {
                    out = ADDED;                    
                    db.addRank(who, group);
                }
                
                out = out.replaceAll("%who", who);
                out = out.replaceAll("%group", group);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments( sender, AddFormat );
        }
    }

    private void deleteGroup(Message event) throws DBException, SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();
        
        if (msg.length == 2) {
            DB db = DB.getInstance();
            String group = msg[1];
            if ( !db.isRankGroup( group ) ) {
                bot.sendIRCMessage(channel, NO_GROUP.replaceAll("%group", group));
            } else {
                db.deleteRankGroup(group);
                bot.sendIRCMessage(channel, GROUP_DELETED.replaceAll("%group", group));
            }
        } else {
            bot.invalidArguments( sender, DelFormat );
        }
    }

    private void newGroup(Message event) throws DBException, SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();
        
        if (msg.length == 2) {
            DB db = DB.getInstance();
            String group = msg[1];
            if ( db.isRankGroup( group ) ) {
                bot.sendIRCMessage(channel, GROUP_EXISTS.replaceAll("%group", group));
            } else {
                db.newRankGroup(group);
                bot.sendIRCMessage(channel, GROUP_CREATED.replaceAll("%group", group));
            }
        } else {
            bot.invalidArguments( sender, NewFormat );
        }
    }

    private void renameGroup(Message event) throws DBException, SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        String channel = event.getChannel().getName();
        
        if (msg.length == 2) {
            DB db = DB.getInstance();
            String oldgroup = msg[1];
            String newgroup = msg[1];
            if ( !db.isRankGroup( oldgroup ) ) {
                bot.sendIRCMessage(channel, NO_GROUP.replaceAll("%group", oldgroup));
            } else if ( db.isRankGroup( newgroup ) ) {
                bot.sendIRCMessage(channel, GROUP_EXISTS.replaceAll("%group", newgroup));
            } else {
                db.renameRankGroup(oldgroup, newgroup);
                
                String out = GROUP_RENAMED.replaceAll("%oldgroup", oldgroup);
                out = out.replaceAll("%newgroup", newgroup);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments( sender, RenFormat );
        }
    }
}