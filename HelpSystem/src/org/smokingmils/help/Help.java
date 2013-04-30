/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokingmils.help;

import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;

/**
 * Provides the functionality to check a user's chips
 * 
 * @author Jamie
 */
public class Help extends Event {
	public static final String Command = "!info";
	public static final String Description = "%b%c12Lists the available profiles";
	public static final String Format = "%b%c12" + Command + "";
	
	public static final String ProfileChanged = "%b%c04%user %c12is now using the %c04%profile%c12 game profile";
	public static final String ProfileChangeFail = "%b%c04%user %c12tried to change to the %c04%profile%c12 game profile and it failed. Please try again!";
	
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
		String chan = event.getChannel().getName();
		
		if ( isValidChannel( event.getChannel().getName() ) &&
				bot.userIsIdentified( sender ) &&
				message.startsWith( Command ) ) {			
			/*String[] msg = message.split(" ");
			if (msg.length == 0 || msg[0].compareTo("") == 0) {
				for (String line: Strings.InfoMessage.split("\n")) {
					bot.sendIRCNotice( sender, line );
				}
			} else if (msg.length == 1){
				CommandType infocmd = CommandType.fromString( Strings.CommandChar + msg[0] );
				if (infocmd != null) {
					sendFullCommand(sender, infocmd);
				} else if ( msg[0].compareToIgnoreCase("table") == 0 ) {
					CommandType[] table_cmds = {CommandType.CHECK, CommandType.RAISE, CommandType.FOLD,
							 					CommandType.TBLCHIPS, CommandType.REBUY, CommandType.LEAVE,
							 					CommandType.SITDOWN, CommandType.SITOUT};
					for (CommandType item: table_cmds) {
						sendFullCommand(sender, item);
					}
				} else if ( msg[0].compareToIgnoreCase("lobby") == 0 ) {
					CommandType[] lobby_cmds = {CommandType.INFO, CommandType.TABLES,
												CommandType.NEWTABLE, CommandType.WATCHTBL, CommandType.JOIN};
					for (CommandType item: lobby_cmds) {
						sendFullCommand(sender, item);
						bot.sendIRCNotice(sender,"%b%c15-----");
					}
				} else {
					bot.sendIRCNotice(sender, Strings.InvalidInfoArgs.replaceAll("%invalid", msg[0]));
				}
			} else {
				bot.sendIRCNotice(who, InvalidArgs);
				bot.sendIRCNotice(who, format);		
			}*/
		}
	}
    
    /**
     * Sends a command's format message
     * 
     * @param who		The user to send to
     * @param cmd		The command
     * @param format	The command format
     */
    protected void sendFormat(IrcBot bot, String who, String cmd, String format) {	
	}
    
    /**
     * Sends a command's format followed by it's description
     * 
     * @param who		The user to send to
     * @param cmd		The command
     */
    private void sendFullCommand(IrcBot bot, String who, Command cmd) {
		bot.sendIRCNotice(who, "%b%c04 " + cmd.getCommandText()
								+ "%c12 - Format:" + cmd.getFormat());
		bot.sendIRCNotice(who, cmd.getDescription());
	}
}
