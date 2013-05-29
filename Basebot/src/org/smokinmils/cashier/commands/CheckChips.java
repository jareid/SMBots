/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier.commands;

import java.util.Map;
import java.util.Map.Entry;

import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to check a user's chips
 * 
 * @author Jamie
 */
public class CheckChips extends Event {
	public static final String Command = "!check";
	public static final String Description = "%b%c12Query the bot about how many chips you or someone else has";
	public static final String Format = "%b%c12" + Command + " ?user?";
	
	/**
	 * These strings are used to specify the messages sent when a user checks their credit in the system
	 * 
	 * %sender - the person who sent the command
	 * %user - the person who the credit check is for
	 * %creds - the amount of credits
	 * %active - the active profile name
	 */
	public static final String CheckCreditMsg =  "%b%c04%sender%c12: %user %c12currently has %c04%creds%c12 chips on the active profile(%c04%active%c12)";
	public static final String CheckCreditSelfMsg =  "%b%c04%sender%c12: You %c12currently have %c04%creds%c12 chips on the active profile(%c04%active%c12)";
	public static final String CreditsOtherProfiles = "%c04%name%c12(%c04%amount%c12)";
	public static final String NoCredits = "%b%c04%sender: %user %c12currently has %c04no%c12 available chips.";
	public static final String NoCreditsSelf = "%b%c04%sender%c12: %c04You %c12currently has %c04no%c12 available chips.";
	
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
		
		if ( isValidChannel( event.getChannel().getName() ) &&
				bot.userIsIdentified( sender ) &&
				message.toLowerCase().startsWith( Command ) ) {			
			String[] msg = message.split(" ");
			String user = "";
			
			if (msg.length == 1 || msg.length == 2) {
				user = (msg.length > 1 ? msg[1] : sender);
				Map<ProfileType, Integer> creds = null;
				try {
					creds = DB.getInstance().checkAllCredits( user );
				} catch (Exception e) {
					EventLog.log(e, "CheckChips", "message");
				}
				
				String credstr;
				if (creds.size() != 0) {
					ProfileType active = null;
					try {
						active = DB.getInstance().getActiveProfile( user );
					} catch (Exception e) {
						EventLog.log(e, "CheckChips", "message");
					}
					
					if (user.compareToIgnoreCase(sender) == 0) {
						credstr = CheckCreditSelfMsg;
					} else  {
						credstr = CheckCreditMsg;
					}
					Integer active_creds = creds.get(active);
					if (active_creds == null) active_creds = 0;
					credstr = credstr.replaceAll("%creds", Integer.toString(active_creds));
					credstr = credstr.replaceAll("%active", active.toString());
					
					if (creds.size() > 1) {
						credstr += " and ";
						for (Entry<ProfileType, Integer> cred: creds.entrySet()) {
							if (cred.getKey().compareTo(active) != 0) {
								String othercred = CreditsOtherProfiles.replaceAll("%name", cred.getKey().toString());
								othercred = othercred.replaceAll("%amount",Integer.toString(cred.getValue()));
								credstr += othercred + " ";
							}
						}
					}
				} else {
					if (user.compareToIgnoreCase(sender) == 0) {
						credstr = NoCreditsSelf;
					} else  {
						credstr = NoCredits;
					}
				}
				credstr = credstr.replaceAll("%user", user);
				credstr = credstr.replaceAll("%sender", sender);
				
				bot.sendIRCMessage(event.getChannel().getName(), credstr);		
			} else {
				bot.invalidArguments( sender, Format );
			}
		}
	}
}
