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
import java.util.List;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ReferalUser;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality for rank referals.
 * 
 * @author Jamie
 */
public class GroupReferal extends Event {
    /** The refer command. */
    private static final String REF_CMD           = "!grefer";
    
    /** The refer command format. */
    private static final String REF_FORMAT        = "%b%c12" + REF_CMD
                                                  + " <user> <referrers>";
    
    /** The refer command length. */
    public static final int     REF_CMD_LEN       = 3;

    /** The check command. */
    private static final String CHK_CMD           = "!rcheck";
    
    /** The check command format. */
    private static final String CHK_FORMAT        = "%b%c12" + CHK_CMD
                                                          + " <user>";
    
    /** The refer command length. */
    public static final int     CHK_CMD_LEN       = 2;

    /** Message when a user is not a rank. */
    private static final String NOT_RANKED        = "%b%c04%sender%c12: "
                  + "%c04%who%c12 is currently not a member of any rank group.";
    
    /** Message when a user does not exist. */
    private static final String NO_USER           = "%b%c04%sender%c12: "
                                     + "%c04%who%c12 does not exist as a user.";
    
    /** Message when a user tries to refer themselves. */
    private static final String NO_SELF           = "%b%c04%sender%c12: "
                                          + "You can not be your own referrer.";
    
    /** Message when the command was successful. */
    private static final String SUCCESS           = "%b%c04%sender%c12: "
                                    + "Succesfully added %c04%referrers%c12 as "
                                    + "%c04%who%c12's referer(s).";
    
    // TODO: re-add this
    // private static final String FAILED =
    // "%b%c04%sender%c12: %c04%who%c12 is a public referral and can't be
    // given to ranks.";

    /** Start of check command line message. */
    private static final String REFER_CHECK_LINE  = "%b%c04";
    
    /** Check command output. */
    private static final String REFER_CHECK_FLINE = "%b%c04%sender%c12: "
                                          + "%c04%user%c12 is refered by: %c04";
    
    /** Message when user has no referrers. */
    private static final String REFER_CHECK_NONE  = "%b%c04%sender%c12: "
                                    + "%c04%user%c12 has %c04no%c12 referrers!";
    
    /** Max line length for the output of check. */
    private static final int MAX_LINE = 80;
    
    /** Size of the first line of check. */
    private static final int FIRST_LINE = 20;


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
                if (Utils.startsWith(message, REF_CMD)) {
                    refer(event);
                } else if (Utils.startsWith(message, CHK_CMD)) {
                    check(event);
                }
            } catch (Exception e) {
                EventLog.log(e, "GroupReferral", "message");
            }
        }
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
        String sender = event.getUser().getNick();
        Channel channel = event.getChannel();

        if (msg.length >= REF_CMD_LEN) {
            DB db = DB.getInstance();
            String user = msg[1];
            /*
             * ReferrerType reftype = db.getRefererType(user);
             * 
             * TODO: check with J if this should be removed? if (reftype ==
             * ReferrerType.NONE || reftype == ReferrerType.GROUP) {
             */
            List<String> refs = new ArrayList<String>();
            boolean isok = true;
            for (int i = 2; i < msg.length; i++) {
                String ref = msg[i];
                if (ref.equalsIgnoreCase(user)) {
                    String out = NO_SELF.replaceAll("%sender", sender);
                    bot.sendIRCMessage(channel, out);

                    isok = false;
                } else if (!db.checkUserExists(user)) {
                    String out = NO_USER.replaceAll("%sender", sender);
                    out = out.replaceAll("%who", user);
                    bot.sendIRCMessage(channel, out);

                    isok = false;
                } else if (!db.checkUserExists(ref)) {
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
            /*
             * TODO: check with J if this should be removed? } else { String out
             * = FAILED.replaceAll("%sender", sender); out =
             * out.replaceAll("%who", user); bot.sendIRCMessage(channel, out); }
             */
        } else {
            bot.invalidArguments(event.getUser(), REF_FORMAT);
        }
    }

    /**
     * Handles the check command.
     * 
     * @param event the message event
     * 
     * @throws SQLException on a database error
     */
    private void check(final Message event)
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
                        while (words.length > i
                                &&
                               line.length() + words[i].length() < linelim) {
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
}
