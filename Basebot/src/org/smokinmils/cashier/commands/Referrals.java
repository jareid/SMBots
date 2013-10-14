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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.ReferalUser;
import org.smokinmils.database.types.ReferrerType;
import org.smokinmils.database.types.UserCheck;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for managing the rank groups.
 * 
 * @author Jamie
 */
public class Referrals extends Event {    
    /** The command. */
    public static final String  REF_CMD = "!refer";

    /** The command format. */
    private static final String REFMT  = "%b%c12" + REF_CMD + " <user>";

    /** The command length. */
    public static final int     REF_CMD_LEN = 2;
    
    /** The group refer command. */
    private static final String GREF_CMD           = "!grefer";
    
    /** The group refer command format. */
    private static final String GREF_FORMAT        = "%b%c12" + GREF_CMD + " <user> <referrers>";
    
    /** The group refer command length. */
    public static final int     GREF_CMD_LEN       = 3;

    /** The check command. */
    private static final String CHK_CMD           = "!rcheck";
    
    /** The check command format. */
    private static final String CHK_FORMAT        = "%b%c12" + CHK_CMD + " <user>";
    
    /** The refer command length. */
    public static final int     CHK_CMD_LEN       = 2;
    
    /** The rename command. */
    private static final String REN_CMD       = "!rengroup";

    /** The command format. */
    private static final String REN_FRMT      = "%b%c12" + REN_CMD + " <oldname> <newname>";

    /** The rename command length. */
    private static final int     REN_CMD_LEN   = 3;

    /** The remove referrer command. */
    private static final String RR_CMD        = "!remref";

    /** The command format. */
    private static final String RR_FRMT       = "%b%c12" + RR_CMD + " <user> <referrer>";

    /** The command length. */
    private static final int     RR_CMD_LEN    = 3;
    
    /** The remove referrer command. */
    private static final String AR_CMD        = "!addref";

    /** The command format. */
    private static final String AR_FRMT       = "%b%c12" + AR_CMD + " <user> <referrer>";

    /** The command length. */
    private static final int     AR_CMD_LEN    = 3;

    /** The new group command. */
    private static final String NEW_CMD       = "!newgroup";

    /** The command format. */
    private static final String NEW_FRMT      = "%b%c12" + NEW_CMD + " <name> <owner>";

    /** The rename command. */
    private static final String DEL_CMD       = "!delgroup";

    /** The command format. */
    private static final String DEL_FRMT      = "%b%c12" + DEL_CMD + " <group>";

    /** The rename command. */
    private static final String ADD_CMD       = "!rankadd";

    /** The command format. */
    private static final String ADD_FRMT      = "%b%c12" + ADD_CMD + " <user> <group>";

    /** The command length. */
    private static final int    ADD_CMD_LEN   = 3;

    /** The rename command. */
    private static final String KIK_CMD       = "!rankkick";

    /** The command format. */
    private static final String KIK_FRMT      = "%b%c12" + KIK_CMD + " <user>";

    /** The groups command. */
    private static final String GRPS_CMD      = "!groups";

    /** The groups command format. */
    private static final String GRPS_FRMT     = "%b%c12" + GRPS_CMD + "";

    /** The group list command. */
    private static final String GL_CMD        = "!grouplist";

    /** The group list command format. */
    private static final String GL_FRMT       = "%b%c12" + GL_CMD + " <group>";

    /** The command to check points. */
    public static final String MYPOINTS_CMD   = "!mypoints";
    
    /** The command to check points. */
    public static final String GRPPOINTS_CMD   = "!grppoints";
    
    /** The group list command format. */
    private static final String GRPPOINTS_FRMT   = "%b%c12" + GRPPOINTS_CMD + " <group>";
    
    /** The command to give points. */
    public static final String  POINTS_CMD    = "!points";
    
    /** The group list command format. */
    private static final String POINTS_FRMT   = "%b%c12" + POINTS_CMD + " <user> <amount>";
    
    /** The command to give points. */
    public static final String  MINPOINTS_CMD    = "!minpoints";
    
    /** The group list command format. */
    private static final String MINPOINTS_FRMT   = "%b%c12" + MINPOINTS_CMD + " <points>";
    
    /** Max line length for the output of check. */
    private static final int MAX_LINE         = 80;
    
    /** Size of the first line of check. */
    private static final int FIRST_LINE       = 20;

    /** Message when a user is not a rank. */
    private static final String NOT_RANKED    = "%b%c04%sender%c12: "
                  + "%c04%who%c12 is currently not a member of any rank group.";
    
    /** Message when the command was successful. */
    private static final String SUCCESS       = "%b%c04%sender%c12: Succesfully added "
                                              + "%c04%referrers%c12 as %c04%who%c12's referer(s).";
   

    /** Start of check command line message. */
    private static final String REFER_CHECK_LINE  = "%b%c04";
    
    /** Check command output. */
    private static final String REFER_CHECK_FLINE = "%b%c04%sender%c12: "
                                          + "%c04%user%c12 is refered by: %c04";
    
    /** Message when user has no referrers. */
    private static final String REFER_CHECK_NONE  = "%b%c04%sender%c12: "
                                    + "%c04%user%c12 has %c04no%c12 referrers!";

    /** Message when the user doesn't exist. */
    private static final String NO_USER = "%b%c04%sender%c12: " 
                                        + "%c04%who%c12 does not exist as a user.";

    /** Message when informing you can't self refer. */
    private static final String NO_SELF = "%b%c04%sender%c12: You can not be your own referrer.";
    
    /** Message when the referral is successful. */
    private static final String REFSUCCESS = "%b%c04%sender%c12: "
                                        + "Succesfully added %c04%who%c12 as your referer.";

    /** Message when the referral failed. */
    private static final String REFFAILED  = "%b%c04%sender%c12: You already have a referrer.";

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
    private static final String NO_GROUP      = "%b%c04%group%c12 does not exist as a rank group.";

    /** Message when a rank group exists. */
    private static final String GROUP_EXISTS  = "%b%c04%group%c12 already exists as a rank group.";

    /** Message when a rank group is created. */
    private static final String GROUP_CREATED = "%b%c04%group%c12 rank group has been created.";

    /** Message when a rank group is deleted. */
    private static final String GROUP_DELETED = "%b%c04%group%c12 rank group has been deleted.";

    /** Message when a rank group is renamed. */
    private static final String GROUP_RENAMED = "%b%c04%oldgroup%c12 rank group has "
                                              + "been renamed to %c04%newgroup%c12.";

    /** Message listing all rank groups. */
    private static final String LIST_GROUPS   = "%b%c04%sender%c12: Valid "
                                              + "rank groups are: %c04%groups%c12";

    /** Message when a rank group's user list is requested. */
    private static final String GROUP_LIST    = "%b%c04%sender%c12: %c04%group%c12 "
                                              + "rank group contains: %c04%users%c12";
    
    /** Message when the add refer is successful. */
    private static final String ADDSUCCESS    = "%b%c04%sender%c12: Succesfully"
                                              + " added %c04%ref%c12 as %c04%user's%c12 referer.";

    /** Message when the referral failed. */
    private static final String ADDFAILED     = "%b%c04%sender%c12: "
                                              + "%c04%who%c12 already has a referrer.";
    
    /** Message when the points are checked. */
    private static final String POINTS        = "%b%c04%sender%c12: "
                                              + "%c04%who%c12 has %c04%points%c12 points this week."
                                              + " The minimum is %c04%minpoints%c12 per user."
                                              + " The total points this week is %c04%totpoints.";
    
    /** Message when the points are checked. */
    private static final String POINTS_STATS  = "%b%c04%sender%c12: "
                                              + "%c04%profile%c12 has %c04%chips%c12 chips so far"
                                              + ". That is %c04%chippoint%c12 per point and "
                                              + "%c04%user%c12 will get %c04%userchips%c12.";
    
    /** Message when the points are given. */
    private static final String GIVEPOINTS    = "%b%c04%sender%c12: "
                                              + "%c04%who%c12 has been given %c04%points%c12 points"
                                              + " and now has %c04%newpoints%c12 points";
    
    /** Message when the minimum points is set. */
    private static final String MINPOINTS    = "%b%c04%sender%c12: %c12The minimum points "
                                              + "has been set to %c04%points%c12 points";
    
    /** The group failed announce string. */
    private static final String GROUP_FAILING = "%b%c04%sender%c12: %c04%group%c12 has "
                                              + "%c04%points%c12 of %c04%min%c12 points  this week";

    /** The valid channels for rank only commands. */
    private final List<String> rankValidChans;

    /** The valid channels for manager only commands. */
    private final List<String> managerValidChans;
    
    /**
     * Constructor.
     * 
     * @param mgrchans Valid manager channels.
     * @param rnkchans Valid rank channels.
     */
    public Referrals(final String[] mgrchans, final String[] rnkchans) {
        managerValidChans = new ArrayList<String>();
        rankValidChans = new ArrayList<String>();
        
        for (String chan: mgrchans) {
            managerValidChans.add(chan.toLowerCase());
            rankValidChans.add(chan.toLowerCase());
            // add mgr channels as valid rank channels so that rank commands work in mgr chans too
        }
        
        for (String chan: rnkchans) {
            rankValidChans.add(chan.toLowerCase());
        }
    }
    
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

        try {
            if (bot.userIsIdentified(sender)) {
                String cname = chan.getName();
                /*
                 * if (isValidChannel(cname) && Utils.startsWith(message, REF_CMD)) {
                    refer(event);
                } else 
                 */
                if (isRankValidChannel(cname)) {
                    /*
                    if (Utils.startsWith(message, GREF_CMD)) {
                        groupRefer(event);
                    } else if (Utils.startsWith(message, CHK_CMD)) {
                        referCheck(event);
                    } else */
                    if (Utils.startsWith(message, MYPOINTS_CMD)) {
                        checkPoints(event);
                    }
                }
                
                if (isMgrValidChannel(cname)) {
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
                    } else if (Utils.startsWith(message, POINTS_CMD)) {
                        givePoints(event);
                    } else if (Utils.startsWith(message, MINPOINTS_CMD)) {
                        setMinPoints(event);
                    } else if (Utils.startsWith(message, GRPPOINTS_CMD)) {
                        checkGroupPoints(event);
                    }
                }
            }
        } catch (Exception e) {
            EventLog.log(e, "RankGroups", "message");
        }
    }

    /** 
     * Checks if the channel is a valid rank channel.
     * 
     * @param cname the channel to check.
     * 
     * @return true if the channel is valid.
     */
    private boolean isMgrValidChannel(final String cname) {
        return managerValidChans.contains(cname.toLowerCase());
    }
    
    /** 
     * Checks if the channel is a valid manager channel.
     * 
     * @param cname the channel to check.
     * 
     * @return true if the channel is valid.
     */
    private boolean isRankValidChannel(final String cname) {
        return rankValidChans.contains(cname.toLowerCase());
    }

    /**
     * Handles the referral command.
     * 
     * @param event the message event
     * 
     * @throws SQLException on a database error
     */
    private void refer(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel channel = event.getChannel();

        if (msg.length == 2) {
            DB db = DB.getInstance();
            ReferrerType reftype = db.getRefererType(sender);
            if (reftype == ReferrerType.NONE) {
                String referrer = msg[1];
                User ref = bot.getUserChannelDao().getUser(referrer);
                if (referrer.equalsIgnoreCase(sender)) {
                    String out = NO_SELF.replaceAll("%sender", sender);
                    bot.sendIRCMessage(channel, out);
                } else if (!db.checkUserExists(referrer)
                        || db.checkUserExists(referrer, ref.getHostmask()) == UserCheck.FAILED) {
                    String out = NO_USER.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", referrer);
                    bot.sendIRCMessage(channel, out);
                } else {
                    db.addReferer(sender, referrer);
                    String out = REFSUCCESS.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", referrer);
                    bot.sendIRCMessage(channel, out);
                }
            } else {
                String out = REFFAILED.replaceAll("%sender", sender);
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(senderu, REFMT);
        }
    }

    /**
     * Handles the group referral command.
     * 
     * @param event the message event
     * 
     * @throws SQLException on a database error
     */
    private void groupRefer(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        String sender = event.getUser().getNick();
        Channel channel = event.getChannel();

        if (msg.length >= GREF_CMD_LEN) {
            DB db = DB.getInstance();
            String user = msg[1];
           
            List<String> refs = new ArrayList<String>();
            boolean isok = true;
            for (int i = 2; i < msg.length; i++) {
                String ref = msg[i];
                User refer = bot.getUserChannelDao().getUser(ref);
                if (ref.equalsIgnoreCase(user)) {
                    String out = NO_SELF.replaceAll("%sender", sender);
                    bot.sendIRCMessage(channel, out);

                    isok = false;
                } else if (!db.checkUserExists(user)) {
                    String out = NO_USER.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", user);
                    bot.sendIRCMessage(channel, out);

                    isok = false;
                } else if (!db.checkUserExists(ref) 
                        || db.checkUserExists(ref, refer.getHostmask()) == UserCheck.FAILED) {
                    String out = NO_USER.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", ref);
                    bot.sendIRCMessage(channel, out);

                    isok = false;
                } else if (!db.isRank(ref)) {
                    String out = NOT_RANKED.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", ref);
                    bot.sendIRCMessage(channel, out);

                    isok = false;
                }

                // break from the loop if we had a problem.
                if (!isok) {
                    break;
                } else {
                    refs.add(ref);
                }
            }

            if (isok) {
                for (String referrer : refs) {
                    db.addReferer(user, referrer);
                }
                String out = SUCCESS.replaceAll("%sender", sender);
                out = out.replaceAll("%who", user);
                out = out.replaceAll("%referrers", Utils.listToString(refs));
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(event.getUser(), GREF_FORMAT);
        }
    }

    /**
     * Handles the check command.
     * 
     * @param event the message event
     * 
     * @throws SQLException on a database error
     */
    private void referCheck(final Message event)
        throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        User senderu = event.getUser();
        String sender = senderu.getNick();

        if (msg.length >= 2) {
            DB db = DB.getInstance();
            String user = msg[1];
            if (!db.checkUserExists(user)) {
                String out = NO_USER.replaceAll("%sender", sender);
                out = out.replaceAll("%who", user);
                bot.sendIRCMessage(event.getChannel(), out);
            } else {
                List<ReferalUser> refs = db.getReferalUsers(user);
                if (refs.size() == 0) {
                    String line = REFER_CHECK_NONE.replaceAll("%user", user);
                    line = line.replaceAll("%sender", sender);
                    bot.sendIRCNotice(senderu, line);
                } else {
                    String[] words = Utils.listToString(refs)
                                                        .split("(?=[\\s\\.])");
                    int i = 0;
                    boolean isfirst = true;
                    while (words.length > i) {
                        String line = REFER_CHECK_LINE;
                        int linelim = MAX_LINE;
                        if (isfirst) {
                            line = REFER_CHECK_FLINE.replaceAll("%user", user);
                            line = line.replaceAll("%sender", sender);
                            isfirst = false;
                            linelim = MAX_LINE - FIRST_LINE;
                        }
                        while (words.length > i && line.length() + words[i].length() < linelim) {
                            line += words[i];
                            i++;
                        }
                        bot.sendIRCNotice(senderu, line);
                    }
                }
            }
        } else {
            bot.invalidArguments(event.getUser(), CHK_FORMAT);
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
                String out = NO_USER.replaceAll("%who", who);
                out = out.replaceAll("%sender", event.getUser().getNick());
                bot.sendIRCMessage(channel, out);
            } else if (!db.isRank(who)) {
                String out =  NOT_RANKED.replaceAll("%who", who);
                out = out.replaceAll("%sender", event.getUser().getNick());
                bot.sendIRCMessage(channel, out);
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
                String out = NO_USER.replaceAll("%who", who);
                out = out.replaceAll("%sender", event.getUser().getNick());
                bot.sendIRCMessage(channel, out);
            } else if (!db.isRankGroup(group)) {
                bot.sendIRCMessage(channel, NO_GROUP.replaceAll("%group", group));
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
                bot.sendIRCMessage(channel, NO_GROUP.replaceAll("%group", group));
            } else {
                db.deleteRankGroup(group);
                bot.sendIRCMessage(channel, GROUP_DELETED.replaceAll("%group", group));
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
        
        int cmdlen = 1 + 1 + 1;
        if (msg.length == cmdlen) {
            DB db = DB.getInstance();
            String group = msg[1];
            String owner = msg[2];
            if (!db.checkUserExists(owner)) {
                String out = NO_USER.replaceAll("%sender", event.getUser().getNick());
                out = out.replaceAll("%who", owner);
                bot.sendIRCMessage(channel, out);
            } else if (db.isRankGroup(group)) {
                bot.sendIRCMessage(channel, GROUP_EXISTS.replaceAll("%group", group));
            } else {
                db.newRankGroup(owner, group);
                bot.sendIRCMessage(channel, GROUP_CREATED.replaceAll("%group", group));
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
                bot.sendIRCMessage(channel, NO_GROUP.replaceAll("%group", oldgroup));
            } else if (db.isRankGroup(newgroup)) {
                bot.sendIRCMessage(channel, GROUP_EXISTS.replaceAll("%group", newgroup));
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
    private void addReferrer(final Message event) throws SQLException {
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
                    String out = NO_USER.replaceAll("%who", ref);
                    out = out.replaceAll("%sender", event.getUser().getNick());
                    bot.sendIRCMessage(channel, out);
                } else if (!db.checkUserExists(user)) {
                    String out = NO_USER.replaceAll("%who", sender);
                    out = out.replaceAll("%sender", event.getUser().getNick());
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
    
    /**
     * Checks the number of points a rank has.
     * 
     * @param event The message event.
     * @throws SQLException When the database fails.
     */
    private void checkPoints(final Message event) throws SQLException {
        IrcBot bot = event.getBot();
        Channel channel = event.getChannel();
        String[] msg = event.getMessage().split(" ");
        User user = event.getUser();
        
        DB db = DB.getInstance();

        String sender = user.getNick();
        String who = user.getNick();
        
        if (msg.length > 1) {
            who = msg[1];
        }
        
        if (db.isRank(who)) {
            int rankpoints = db.checkPoints(who);
            int totalpoints = db.getPointTotal();
            int minpoints = db.getMinPoints();
            Map<ProfileType, Double> chips = db.checkAllCredits(DB.POINTS_USER);
            
            String out = POINTS.replaceAll("%points", Integer.toString(rankpoints));
            out = out.replaceAll("%who", who);
            out = out.replaceAll("%sender", sender);
            out = out.replaceAll("%minpoints", Integer.toString(minpoints));
            out = out.replaceAll("%totpoints", Integer.toString(totalpoints));
            bot.sendIRCMessage(channel, out);
            
            for (Entry<ProfileType, Double> entry: chips.entrySet()) {
                double coin = entry.getValue();
                double chippoint = coin / totalpoints;
                if (chippoint == Double.NaN) {
                    chippoint = 0.0;
                }
                
                double userchips = chippoint * rankpoints;
                
                out = POINTS_STATS;
                out = out.replaceAll("%profile", entry.getKey().toString());
                out = out.replaceAll("%chippoint", Utils.chipsToString(chippoint));
                out = out.replaceAll("%userchips", Utils.chipsToString(userchips));
                out = out.replaceAll("%chips", Utils.chipsToString(coin));
                out = out.replaceAll("%user", who);
                out = out.replaceAll("%sender", sender);
                bot.sendIRCNotice(user, out);
            }
        } else {
            String out =  NOT_RANKED.replaceAll("%who", who);
            out = out.replaceAll("%sender", sender);
            bot.sendIRCMessage(channel, out);
        }
    }

    /**
     * Gives a rank a number of points.
     * 
     * @param event The message event.
     * 
     * @throws SQLException When the database fails.
     */
    private void givePoints(final Message event) throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        Channel channel = event.getChannel();

        int cmdlen = 1 + 1 + 1;
        if (msg.length == cmdlen) {
            DB db = DB.getInstance();
            String who = msg[1];
            Integer points = Utils.tryParse(msg[2]);
            
            if (points == null) {
                bot.invalidArguments(event.getUser(), POINTS_FRMT);
            } else if (!db.isRank(who)) {
                String out =  NOT_RANKED.replaceAll("%who", who);
                out = out.replaceAll("%sender", event.getUser().getNick());
                
                bot.sendIRCMessage(channel, out);
            } else {
                db.givePoints(who, points);
                int newpoints = db.checkPoints(who);
                String out = GIVEPOINTS.replaceAll("%who", who);
                out = out.replaceAll("%sender", event.getUser().getNick());
                out = out.replaceAll("%points", Integer.toString(points));
                out = out.replaceAll("%newpoints", Integer.toString(newpoints));
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(event.getUser(), POINTS_FRMT);
        }
    }

    /**
     * Gives a rank a number of points.
     * 
     * @param event The message event.
     * 
     * @throws SQLException When the database fails.
     */
    private void setMinPoints(final Message event) throws SQLException {
        IrcBot bot = event.getBot();
        String[] msg = event.getMessage().split(" ");
        Channel channel = event.getChannel();

        int cmdlen = 1 + 1;
        if (msg.length == cmdlen) {
            DB db = DB.getInstance();
            Integer points = Utils.tryParse(msg[1]);
            
            if (points == null) {
                bot.invalidArguments(event.getUser(), MINPOINTS_FRMT);
            } else {
                db.setMinPoints(points);
                String out = MINPOINTS.replaceAll("%points", Integer.toString(points));
                out = out.replaceAll("%sender", event.getUser().getNick());
                
                bot.sendIRCMessage(channel, out);
            }
        } else {
            bot.invalidArguments(event.getUser(), MINPOINTS_FRMT);
        }
    }
    
    /**
     * Checks the number of points a group has.
     * 
     * @param event The message event.
     * @throws SQLException When the database fails.
     */
    private void checkGroupPoints(final Message event) throws SQLException {
        IrcBot bot = event.getBot();
        Channel channel = event.getChannel();
        String[] msg = event.getMessage().split(" ");
        
        if (msg.length == 2) {            
            bot.bePatient(event.getUser());
            
            DB db = DB.getInstance();
            String group = msg[1].toLowerCase();
            if (!db.isRankGroup(group)) {
                bot.sendIRCMessage(channel, NO_GROUP.replaceAll("%group", group));
            } else {
                int minpoints = db.getMinPoints();
                Map<String, Integer> grouppoints = new HashMap<String, Integer>();
                Map<String, Integer> groupusers = new HashMap<String, Integer>();            
                Map<String, Integer> allpoints = db.getPoints();
                for (Entry<String, Integer> ent: allpoints.entrySet()) {
                    if (ent.getValue() != null) {
                        // note how many points the group have.
                        String grp = db.getRankGroup(ent.getKey()).toLowerCase();
                        if (!grouppoints.containsKey(grp)) {
                            grouppoints.put(grp, ent.getValue());
                            groupusers.put(grp, 1);
                        } else {
                            grouppoints.put(grp, grouppoints.get(grp) + ent.getValue());
                            groupusers.put(grp, groupusers.get(grp) + 1);
                        }
                    }
                }
                
                Integer users = groupusers.get(group);
                Integer userpts = grouppoints.get(group);
                if (users != null && userpts != null) {
                    int min = users * minpoints;
                    String out = GROUP_FAILING.replaceAll("%group", group);
                    out = out.replaceAll("%min", Integer.toString(min));
                    out = out.replaceAll("%points", Integer.toString(userpts));
                    out = out.replaceAll("%sender", event.getUser().getNick());
                    bot.sendIRCMessage(channel, out);

                } else {
                    bot.sendIRCMessage(channel, NO_GROUP.replaceAll("%group", group));
                }
            }
        } else {
            bot.invalidArguments(event.getUser(), GRPPOINTS_FRMT);
        }
    }
}
