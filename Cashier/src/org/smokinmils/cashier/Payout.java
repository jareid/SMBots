/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier;

import org.pircbotx.Channel;
import org.smokinmils.Database;
import org.smokinmils.Utils;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to give a user some chips
 * 
 * @author Jamie
 */
public class Payout extends Event {
	public static final String Command = "!payout";
	public static final String Description = "%b%c12Payout a number of chips from a players profile";
	public static final String Format = "%b%c12" + Command + " <user> <amount> <profile>";
	
	private static final String PayoutChips = "%b%c04%sender:%c12 Paid out %c04%amount%c12 chips from the %c04%profile%c12 account of %c04%who%c12";
	private static final String PayoutChipsPM = "%b%c12You have had %c04%amount%c12 chips paid out from your account by %c04%sender%c12";
	public static final String NoChipsMsg = "%b%c12Sorry, %c04%user%c12 does not have %c04%chips%c12 chips available for the %c04%profile%c12 profile.";
	
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
		
		if ( isValidChannel( chan.getName() ) &&
				bot.userIsIdentified( sender ) &&
				message.startsWith( Command ) ) {			
			String[] msg = message.split(" ");

			if ( chan.isOp(event.getUser()) ) {
				if (msg.length == 4) {
					String user = msg[1];
					Integer amount = Utils.tryParse(msg[2]);
					ProfileType profile = ProfileType.fromString(msg[3]);
						
					if (amount != null && amount > 0) {
						int chips = 0;
						try {
							chips = Database.getInstance().checkCredits( user, profile );
						} catch (Exception e) {
							EventLog.log(e, "Payout", "message");
						}

						if ( profile == null ) {
							bot.sendIRCMessage(chan.getName(), IrcBot.ValidProfiles);
						} else if ( chips < amount ) {
							String out = NoChipsMsg.replaceAll( "%chips", Integer.toString(amount));
							out = out.replaceAll( "%profile", profile.toString() );
							out = out.replaceAll( "%user", user );
							bot.sendIRCMessage(chan.getName(), out);
						} else {
							boolean success = false;
							try {
								success = Database.getInstance().payoutChips(user, amount, profile);
							} catch (Exception e) {
								EventLog.log(e, "Payout", "message");
							}
							
							if (success) {
								String out = PayoutChips.replaceAll("%amount", Integer.toString(amount));
								out = out.replaceAll("%who", user);
								out = out.replaceAll("%sender", sender);
								out = out.replaceAll("%profile", profile.toString());
								bot.sendIRCMessage(chan.getName(), out);
								
								out = PayoutChipsPM.replaceAll("%amount", Integer.toString(amount));
								out = out.replaceAll("%who", user);
								out = out.replaceAll("%sender", sender);
								out = out.replaceAll("%profile", profile.toString());
								bot.sendIRCNotice(user, out);
							} else {
								EventLog.log(sender + " attempted to pay out chips and the database failed", "Payout", "message");
							}
						}
					} else {
						bot.invalidArguments( sender, Format );
					}
				} else {
					bot.invalidArguments( sender, Format );
				}
			} else {
				EventLog.info(sender + " attempted to pay out someone chips", "Payout", "message");
			}
		}
	}
}
