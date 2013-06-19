/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

import java.sql.SQLException;
import java.util.List;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ReferalUser;
import org.smokinmils.database.types.ReferrerType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for managing the rank groups.
 * 
 * @author Jamie
 */
public class RankGroups extends Event {

    /** The rename command. */
    private static final String REN_CMD       = "!rengroup";

    /** The command format. */
    private static final String REN_FRMT      = "%b%c12" + REN_CMD
                                                      + " <oldname> <newname>";

    /** The rename command length. */
    private static final int     REN_CMD_LEN   = 3;

    /** The remove referrer command. */
    private static final String RR_CMD        = "!remref";

    /** The command format. */
    private static final String RR_FRMT       = "%b%c12" + RR_CMD
                                                      + " <user> <referrer>";

    /** The command length. */
    private static final int     RR_CMD_LEN    = 3;
    
    /** The remove referrer command. */
    private static final String AR_CMD        = "!addref";

    /** The command format. */
    private static final String AR_FRMT       = "%b%c12" + AR_CMD
                                                      + " <user> <referrer>";

    /** The command length. */
    private static final int     AR_CMD_LEN    = 3;

    /** The new group command. */
    private static final String NEW_CMD       = "!newgroup";

    /** The command format. */
    private static final String NEW_FRMT      = "%b%c12" + NEW_CMD + " <name>";

    /** The rename command. */
    private static final String DEL_CMD       = "!delgroup";

    /** The command format. */
    private static final String DEL_FRMT      = "%b%c12" + DEL_CMD + " <group>";

    /** The rename command. */
    private static final String ADD_CMD       = "!rankadd";

    /** The command format. */
    private static final String ADD_FRMT      = "%b%c12" + ADD_CMD
                                                      + " <user> <group>";

    /** The command length. */
    private static final int     ADD_CMD_LEN   = 3;

    /** The rename command. */
    private static final String KIK_CMD       = "!rankkick";

    /** The command format. */
    private static final String KIK_FRMT      = "%b%c12" + KIK_CMD + " <user>";

    /** The rename command. */
    private static final String GRPS_CMD      = "!groups";

    /** The command format. */
    private static final String GRPS_FRMT     = "%b%c12" + GRPS_CMD + "";

    /** The rename command. */
    private static final String GL_CMD        = "!grouplist";

    /** The command format. */
    private static final String GL_FRMT       = "%b%c12" + GL_CMD + " <group>";

    /** Message for un ranked users. */
    private static final String NOT_RANKED    = "%b%c04%who%c12 is "
                                  + "currently not a member of any rank group.";

    /** Message for no user. */
    private static final String NO_USER       = "%b%c04%who%c12 does not "
                                                      + "exist as a user.";

    /** Message for no referrer. */
    private static final String NO_REFERRER   = "%b%c04%user%c12 has not "
                                             + "been referred by %c04%ref%c12.";

    /** Message for a referrer has been removed. */
    private static final String REF_REMOVED   = "%b%c04%user%c12 is no "
                                           + "longer referred by %c04%ref%c12.";

    /** Message when a user kicked from a rank group. */
    private static final String KICKED        = "%b%c04%who%c12 has been "
                                 + "kicked from the %c04%group%c12 rank group.";

    /** Message when a user added to a rank group. */
    private static final String ADDED         = "%b%c04%who%c12 has been "
                                    + "added to the %c04%group%c12 rank group.";

    /** Message when a user moved to a new rank group. */
    private static final String MOVED         = "%b%c04%who%c12 has been "
                            + "moved from %c04%oldgroup%c12 to %c04%group%c12.";

    /** Message when a rank group doesn't exist. */
    private static final String NO_GROUP      = "%b%c04%group%c12 does not "
                                              + "exist as a rank group.";

    /** Message when a rank group exists. */
    private static final String GROUP_EXISTS  = "%b%c04%group%c12 already "
                                               + "exists as a rank group.";

    /** Message when a rank group is created. */
    private static final String GROUP_CREATED = "%b%c04%group%c12 rank group "
                                                      + "has been created.";

    /** Message when a rank group is deleted. */
    private static final String GROUP_DELETED = "%b%c04%group%c12 rank group "
                                                      + "has been deleted.";

    /** Message when a rank group is renamed. */
    private static final String GROUP_RENAMED = "%b%c04%oldgroup%c12 rank "
                               + "group has been renamed to %c04%newgroup%c12.";

    /** Message listing all rank groups. */
    private static final String LIST_GROUPS   = "%b%c04%sender%c12: Valid "
                                           + "rank groups are: %c04%groups%c12";

    /** Message when a rank group's user list is requested. */
    private static final String GROUP_LIST    = "%b%c04%sender%c12:"
                         + "%c04%group%c12 rank group contains: %c04%users%c12";
    
    /** Message when informing you can't self refer. */
    private static final String NO_SELF = "%b%c04%sender%c12: " 
                                        + "You can not be your own referrer.";
    
    /** Message when the add refer is successful. */
    private static final String ADDSUCCESS = "%b%c04%sender%c12: Succesfully"
                            + " added %c04%ref%c12 as %c04%user's%c12 referer.";

    /** Message when the referral failed. */
    private static final String ADDFAILED  = "%b%c04%sender%c12: "
                                        + "%c04%who%c12 already has a referrer.";
    
    /**
     * This method handles the commands.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();

        if (bot.userIsIdentified(sender) && isValidChannel(chan.getName())) {
            try {
                if (Utils.startsWith(message, REN_CMD)) {
                    renameGroup(event);
                } else if (Utils.startsWith(message, NEW_CMD)) {
                    newGroup(event);
                } else if (Utils.startsWith(message, DEL_CMD)) {
                    deleteGroup(event);
                } else if (Utils.startsWith(message, ADD_CMD)) {
                    addRank(event);
                } else if (Utils.startsWith(message, KIK_CMD)) {
                    kickRank(event);
                } else if (Utils.startsWith(message, GRPS_CMD)) {
                    listGroups(event);
                } else if (Utils.startsWith(message, GL_CMD)) {
                    groupList(event);
                } else if (Utils.startsWith(message, RR_CMD)) {
                    removeReferrer(event);
                } else if (Utils.startsWith(message, AR_CMD)) {
                    addReferrer(event);
                }
            } catch (Exception e) {
                EventLog.log(e, "RankGroups", "message");
            }
        }
    }

    /**
     * Handles the command to kick rank users from a group.
     * 
     * @param event The message event
     * 
     * @throws SQLException when there is a database error
     */
    private void kickRank(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        User senderu = event.getUser();
        Channel channel = event.getChannel();

        if (msg.length == 2) {
            DB db = DB.getInstance();
            String who = msg[1];
            if (!db.checkUserExists(who)) {
                bot.sendIRCMessage(channel, NO_USER.replaceAll("%who", who));
            } else if (!db.isRank(who)) {
                bot.sendIRCMessage(channel, NOT_RANKED.replaceAll("%who", who));
            } else {
                String group = db.getRankGroup(who);
                db.kickRank(who);

                String out = KICKED.replaceAll("%who", who);
                out = out.replaceAll("%group", group);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(senderu, KIK_FRMT);
        }
    }

    /**
     * Handles the command to add rank users to a group.
     * 
     * @param event The message event
     * 
     * @throws SQLException when there is a database error
     */
    private void addRank(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        Channel channel = event.getChannel();

        if (msg.length == ADD_CMD_LEN) {
            DB db = DB.getInstance();
            String who = msg[1];
            String group = msg[2];
            if (!db.checkUserExists(who)) {
                bot.sendIRCMessage(channel, NO_USER.replaceAll("%who", who));
            } else if (!db.isRankGroup(group)) {
                bot.sendIRCMessage(channel,
                        NO_GROUP.replaceAll("%group", group));
            } else {
                String out = null;
                if (!db.isRank(who)) {
                    out = ADDED;
                    db.addRank(who, group);
                } else if (db.isRank(who)) {
                    String oldgroup = db.getRankGroup(who);
                    out = MOVED.replaceAll("%oldgroup", oldgroup);
                    db.updateRank(who, group);
                }

                out = out.replaceAll("%who", who);
                out = out.replaceAll("%group", group);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(event.getUser(), ADD_FRMT);
        }
    }

    /**
     * Handles the command to delete a group.
     * 
     * @param event The message event
     * 
     * @throws SQLException when there is a database error
     */
    private void deleteGroup(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        Channel channel = event.getChannel();

        if (msg.length == 2) {
            DB db = DB.getInstance();
            String group = msg[1];
            if (!db.isRankGroup(group)) {
                bot.sendIRCMessage(channel,
                        NO_GROUP.replaceAll("%group", group));
            } else {
                db.deleteRankGroup(group);
                bot.sendIRCMessage(channel,
                        GROUP_DELETED.replaceAll("%group", group));
            }
        } else {
            bot.invalidArguments(event.getUser(), DEL_FRMT);
        }
    }

    /**
     * Handles the command to create a new group.
     * 
     * @param event The message event
     * 
     * @throws SQLException when there is a database error
     */
    private void newGroup(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        Channel channel = event.getChannel();

        if (msg.length == 2) {
            DB db = DB.getInstance();
            String group = msg[1];
            if (db.isRankGroup(group)) {
                bot.sendIRCMessage(channel,
                        GROUP_EXISTS.replaceAll("%group", group));
            } else {
                db.newRankGroup(group);
                bot.sendIRCMessage(channel,
                        GROUP_CREATED.replaceAll("%group", group));
            }
        } else {
            bot.invalidArguments(event.getUser(), NEW_FRMT);
        }
    }

    /**
     * Handles the command to renamed a rank group.
     * 
     * @param event The message event
     * 
     * @throws SQLException when there is a database error
     */
    private void renameGroup(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        Channel channel = event.getChannel();

        if (msg.length == REN_CMD_LEN) {
            DB db = DB.getInstance();
            String oldgroup = msg[1];
            String newgroup = msg[2];
            if (!db.isRankGroup(oldgroup)) {
                bot.sendIRCMessage(channel,
                        NO_GROUP.replaceAll("%group", oldgroup));
            } else if (db.isRankGroup(newgroup)) {
                bot.sendIRCMessage(channel,
                        GROUP_EXISTS.replaceAll("%group", newgroup));
            } else {
                db.renameRankGroup(oldgroup, newgroup);

                String out = GROUP_RENAMED.replaceAll("%oldgroup", oldgroup);
                out = out.replaceAll("%newgroup", newgroup);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(event.getUser(), REN_FRMT);
        }
    }

    /**
     * Handles the command to list rank groups.
     * 
     * @param event The message event
     * 
     * @throws SQLException when there is a database error
     */
    private void listGroups(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        Channel channel = event.getChannel();

        if (msg.length == 1) {
            List<String> groups = DB.getInstance().listRankGroups();
            String out = LIST_GROUPS.replaceAll("%sender", sender);
            out = out.replaceAll("%groups", Utils.listToString(groups));
            bot.sendIRCMessage(channel, out);
        } else {
            bot.invalidArguments(event.getUser(), GRPS_FRMT);
        }
    }

    /**
     * Handles the command to list the users in rank groups.
     * 
     * @param event The message event
     * 
     * @throws SQLException when there is a database error
     */
    private void groupList(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        Channel channel = event.getChannel();

        if (msg.length == 2) {
            DB db = DB.getInstance();
            String group = msg[1];
            if (!db.isRankGroup(group)) {
                bot.sendIRCMessage(channel,
                        NO_GROUP.replaceAll("%group", group));
            } else {
                List<String> users = db.listRankGroupUsers(group);
                String out = GROUP_LIST.replaceAll("%sender", sender);
                out = out.replaceAll("%group", group);
                out = out.replaceAll("%users", Utils.listToString(users));
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(event.getUser(), GL_FRMT);
        }
    }

    /**
     * Handles the command to remove a referrer from a user.
     * 
     * @param event The message event
     * 
     * @throws SQLException when there is a database error
     */
    private void removeReferrer(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        Channel channel = event.getChannel();

        if (msg.length == RR_CMD_LEN) {
            DB db = DB.getInstance();
            String user = msg[1];
            String ref = msg[2];
            if (!db.checkUserExists(user)) {
                bot.sendIRCMessage(channel, NO_USER.replaceAll("%who", user));
            } else if (!db.checkUserExists(ref)) {
                bot.sendIRCMessage(channel, NO_USER.replaceAll("%who", ref));
            } else {
                List<ReferalUser> refs = db.getReferalUsers(user);
                ReferalUser found = null;
                for (ReferalUser aref : refs) {
                    if (aref.equals(ref)) {
                        found = aref;
                        break;
                    }
                }
                if (found == null) {
                    String out = NO_REFERRER.replaceAll("%user", user);
                    out = out.replaceAll("%ref", ref);
                    bot.sendIRCMessage(channel, out);
                } else {
                    db.delReferer(user, ref);
                    String out = REF_REMOVED.replaceAll("%user", user);
                    out = out.replaceAll("%ref", ref);
                    bot.sendIRCMessage(channel, out);
                }
            }
        } else {
            bot.invalidArguments(event.getUser(), RR_FRMT);
        }
    }
    
    /**
     * Handles the referral command.
     * 
     * @param event the message event
     * 
     * @throws SQLException on a database error
     */
    private void addReferrer(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel channel = event.getChannel();

        if (msg.length == AR_CMD_LEN) {
            DB db = DB.getInstance();
            ReferrerType reftype = db.getRefererType(sender);
            String user = msg[1];
            String ref = msg[2];
            if (reftype == ReferrerType.NONE) {
                if (ref.equalsIgnoreCase(user)) {
                    String out = NO_SELF.replaceAll("%sender", sender);
                    bot.sendIRCMessage(channel, out);
                } else if (!db.checkUserExists(ref)) {
                    String out = NO_USER.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", ref);
                    bot.sendIRCMessage(channel, out);
                } else if (!db.checkUserExists(user)) {
                    String out = NO_USER.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", user);
                    bot.sendIRCMessage(channel, out);
                } else {
                    db.addReferer(user, ref);
                    String out = ADDSUCCESS.replaceAll("%sender", sender);
                    out = out.replaceAll("%user", user);
                    out = out.replaceAll("%ref", ref);
                    bot.sendIRCMessage(channel, out);
                }
            } else {
                String out = ADDFAILED.replaceAll("%sender", sender);
                out = out.replaceAll("%user", user);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(senderu, AR_FRMT);
        }
    }
}
