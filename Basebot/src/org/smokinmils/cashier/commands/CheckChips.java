/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

import java.util.Map;
import java.util.Map.Entry;

import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to check a user's chips.
 * 
 * @author Jamie
 */
public class CheckChips extends Event {
    /** The command. */
	public static final String COMMAND = "!check";

    /** The command description. */
	public static final String DESC = "%b%c12Query the bot about how many chips"
	                                + " you or someone else has";

    /** The command description. */
	public static final String FORMAT = "%b%c12" + COMMAND + " ?user?";
	
	
	/** Used to specify the Check message on another user. */
	public static final String CHECKCREDITSMSG =  "%b%c04%sender%c12: %user "
	        + "%c12currently has %c04%creds%c12 chips on the active profile("
	        + "%c04%active%c12)";
	
	/** Used to specify the other profiles in the check message. */
	public static final String OTHERPROFILES = "%c04%name%c12 "
	                                                + "(%c04%amount%c12)";
	
	/** Used to specify the user has no credits. */
	public static final String NOCREDITS = "%b%c04%sender: %user %c12currently "
	                                     + "has %c04no%c12 available chips.";

    /**
     * This method handles the chips command.
     * 
     * @param event the Message event
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();

        if (isValidChannel(event.getChannel().getName())
                && bot.userIsIdentified(senderu)
                && Utils.startsWith(message, COMMAND)) {
            String[] msg = message.split(" ");
            String user = "";

            if (msg.length == 1 || msg.length == 2) {
                if (msg.length > 1) {
                    user = msg[1];
                } else {
                    user = sender;
                }
                Map<ProfileType, Double> creds = null;
                try {
                    creds = DB.getInstance().checkAllCredits(user);
                } catch (Exception e) {
                    EventLog.log(e, "CheckChips", "message");
                }

                String credstr;
                if (creds.size() != 0) {
                    ProfileType active = null;
                    try {
                        active = DB.getInstance().getActiveProfile(user);
                    } catch (Exception e) {
                        EventLog.log(e, "CheckChips", "message");
                    }

                    credstr = CHECKCREDITSMSG;

                    Double activecreds = creds.get(active);
                    if (activecreds == null) {
                        activecreds = 0.0;
                    }
                    credstr = credstr.replaceAll("%creds",
                            Utils.chipsToString(activecreds));
                    credstr = credstr.replaceAll("%active", active.toString());

                    if (creds.size() > 1) {
                        credstr += " and ";
                        for (Entry<ProfileType, Double> cred 
                                : creds.entrySet()) {
                            if (cred.getKey().compareTo(active) != 0) {
                                String othercred = OTHERPROFILES.replaceAll(
                                        "%name", cred.getKey().toString());
                                othercred = othercred.replaceAll("%amount",
                                        Utils.chipsToString(cred.getValue()));
                                credstr += othercred + " ";
                            }
                        }
                    }
                } else {
                    credstr = NOCREDITS;
                }
                credstr = credstr.replaceAll("%user", user);

                if (user.equalsIgnoreCase(sender)) {
                    sender = "You";
                }
                credstr = credstr.replaceAll("%sender", sender);

                bot.sendIRCMessage(event.getChannel(), credstr);
            } else {
                bot.invalidArguments(senderu, FORMAT);
            }
        }
    }
}
