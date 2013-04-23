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
public class TransferChips extends Event {
	public static final String Command = "!transfer";
	public static final String Description = "%b%c12Transfer an amount of chips from a profile to another user";
	public static final String Format = "%b%c12" + Command + " <user> <amount> <profile>";
	
	private static final String NoUser = "%b%c04%sender:%c12 %c04%%who%c12 does not exist in the database";
	private static final String TransferChips = "%b%c04%sender%c12 has transfered %c04%amount%c12 chips to the %c04%profile%c12 account of %c04%who%c12";
	private static final String TransferChipsUser = "%b%c12You have had %c04%amount%c12 chips transfered into your %c04%profile%c12 account by %c04%sender%c12";
	private static final String TransferChipsSender = "%b%c12You have transferred %c04%amount%c12 chips from your %c04%profile%c12 account to %c04%who%c12";
	
	/**
	 * This method handles the chips command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */
	public void onMessage(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		Channel chan = event.getChannel();
		
		if ( isValidChannel( chan.getName() ) &&
				bot.userIsIdentified( sender ) &&
				message.startsWith( Command ) ) {			
			String[] msg = message.split(" ");

			if (msg.length == 3) {
				String user = msg[0];
				Integer amount = Utils.tryParse(msg[1]);
				ProfileType profile = ProfileType.fromString(msg[2]);
				
				if (!user.isEmpty() && amount != null) {
					// Check valid profile
					if (profile == null) {
						bot.sendIRCMessage(chan.getName(), IrcBot.ValidProfiles);
					} else {
						try {
							int chips = Database.getInstance().checkCredits( sender, profile );
							if ( chips >= amount ) {
								bot.NoChips(sender, amount, profile);
							} else if ( !Database.getInstance().checkUserExists( user ) ) {
								String out = NoUser.replaceAll("%user", user);
								out = out.replaceAll("%sender", sender);
								bot.sendIRCMessage(chan.getName(), out);
							} else {
								Database.getInstance().transferChips( sender, user, amount, profile );
								
								// Send message to channel
								String out = TransferChips.replaceAll("%who", user);
								out = out.replaceAll("%sender", sender);
								out = out.replaceAll("%amount", Integer.toString(amount) );
								out = out.replaceAll("%profile", profile.toString());
								bot.sendIRCMessage(chan.getName(), out);
								
								// Send notice to sender
								out = TransferChipsUser.replaceAll("%who", user);
								out = out.replaceAll("%sender", sender);
								out = out.replaceAll("%amount", Integer.toString(amount) );
								out = out.replaceAll("%profile", profile.toString());
								bot.sendIRCNotice(user, out);
								
								// Send notice to user
								out = TransferChipsSender.replaceAll("%who", user);
								out = out.replaceAll("%sender", sender);
								out = out.replaceAll("%amount", Integer.toString(amount) );
								out = out.replaceAll("%profile", profile.toString());
								bot.sendIRCNotice(sender, out);
							}
						} catch (Exception e) {
							EventLog.log(e, "TransferChips", "onMessage");
						}
					}
				} else {
					bot.invalidArguments( sender, Format );
				}
			} else {
				bot.invalidArguments( sender, Format );
			}
		}
	}
}
