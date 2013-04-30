/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.game.rooms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.pircbotx.Channel;
import org.smokinmils.Database;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;
import org.smokinmils.pokerbot.Utils;

import org.smokinmils.pokerbot.Client;
import org.smokinmils.pokerbot.enums.CommandType;
import org.smokinmils.pokerbot.enums.RoomType;
import org.smokinmils.pokerbot.settings.Strings;
import org.smokinmils.pokerbot.settings.Variables;

public class Lobby extends Room {
	/* A timer to announce poker to the channel */
	Timer announceTimer;
	class AnnounceTask extends TimerTask {
		public void run() {
			IrcClient.getBot().sendIRCMessage(IrcChannel, Strings.PokerAnnounce);
		    announceTimer.schedule(new AnnounceTask(), Variables.AnnounceMins*60*1000);
		}
	}	
	
	public Lobby(Channel channel, Client irc) {
		super(channel, irc, RoomType.LOBBY);
		RoomTopic = Strings.LobbyTopic;
		announceTimer = new Timer();
	    announceTimer.schedule(new AnnounceTask(), 250);
	}
	

    /**
     * This method is called whenever a message is sent to this channel.
     *
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
	protected void onMessage(String sender, String login, String hostname, String message) {
		int fSpace = message.indexOf(" ");
		if(fSpace == -1) fSpace = message.length();
		String firstWord = message.substring(0, fSpace).toLowerCase();
		
		if ((fSpace + 1) <= message.length()) fSpace++;    		
		message = message.substring( fSpace, message.length() );
		
		CommandType cmd = CommandType.fromString( firstWord );
		if (cmd != null) {
			switch (cmd) {
			case INFO:
				onInfo(sender, login, hostname, message);
				break;
			case NEWTABLE:
				onNewTable(sender, login, hostname, message);
				break;
			case WATCHTBL:
				onWatchTable(sender, login, hostname, message);
				break;
			case TABLES:
				onTables(sender, login, hostname, message);
				break;
			case JOIN:
				onJoinTable(sender, login, hostname, message);
				break;
			default:
				// no action
				break;				
			}
		}
	}
    
    /**
     * This method is called whenever we receive a notice to this channel.
     * 
     * @param sourceNick The nick of the user that sent the notice.
     * @param sourceLogin The login of the user that sent the notice.
     * @param sourceHostname The hostname of the user that sent the notice.
     * @param notice The notice message.
     */
	protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String notice) {}
    
    /**
     * This method is called whenever someone joins a channel which we are on.
     *
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
	protected void onJoin(String sender, String login, String hostname) { }    
    
    /**
     * This method is called whenever someone parts this channel which we are on.
     * This is also the handler for whenever someone quits from the channel
     *
     * @param sender The nick of the user who parted the channel.
     * @param login The login of the user who parted from the channel.
     * @param hostname The hostname of the user who parted from the channel.
     */
	protected void onPart(String sender, String login, String hostname) {}
    
    /**
     * This method is called whenever someone changes nick on this channel.
     *
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The hostname of the user.
     * @param newNick The new nick.
     */
	protected void onNickChange(String oldNick, String login, String hostname, String newNick) {}
	
	/**
	 * This method handles the info command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private void onInfo(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			for (String line: Strings.InfoMessage.split("\n")) {
				IrcClient.sendIRCNotice( sender, line );
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
				CommandType[] lobby_cmds = {CommandType.INFO, /*CommandType.CHIPS,*/ CommandType.TABLES,
											CommandType.NEWTABLE, CommandType.WATCHTBL, CommandType.JOIN};
				for (CommandType item: lobby_cmds) {
					sendFullCommand(sender, item);
					IrcClient.sendIRCNotice(sender,"%b%c15-----");
				}
			} else {
				IrcClient.sendIRCNotice(sender, Strings.InvalidInfoArgs.replaceAll("%invalid", msg[0]));
			}
		} else {
			invalidArguments( sender, CommandType.INFO.getFormat() );
		}
	}
	
	/**
	 * This method handles the new table command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private void onTables(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		Map<Integer, Integer> tables = new HashMap<Integer, Integer>(Table.getTables());
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			IrcClient.sendIRCNotice(sender,
						Strings.AllTablesMsg.replaceAll("%count", Integer.toString(tables.size())));
			for (Entry<Integer, Integer> table: tables.entrySet()) {
				Table tbl = IrcClient.getTable(table.getKey());				
				IrcClient.sendIRCNotice(sender, tbl.formatTableInfo(Strings.TableInfoMsg));
			}
		} else if (msg.length == 1){
			Vector<Integer> table_ids = new Vector<Integer>();
			Integer stake = Utils.tryParse( msg[0] );
			
			Integer tblstke = null;
			for (Entry<Integer, Integer> table: tables.entrySet()) {
				tblstke = (Integer)table.getValue();
				if (tblstke.compareTo(stake) == 0) {
					table_ids.add(table.getKey());
				}
			}
			
			if ( table_ids.size() == 0 ) {
				IrcClient.sendIRCNotice(sender, Strings.NoTablesMsg.replaceAll("%bb", msg[0]));
			} else {
				String out = Strings.FoundTablesMsg.replaceAll("%bb", msg[0]);
				out = out.replaceAll("%count", Integer.toString(table_ids.size()) );
				out = out.replaceAll("%tables", table_ids.toString() );
				
				IrcClient.sendIRCNotice(sender, out);
				
				for (Integer id: table_ids) {
					Table tbl = IrcClient.getTable(id);			
					IrcClient.sendIRCNotice(sender, tbl.formatTableInfo(Strings.TableInfoMsg));
				}					
			}
		} else {
			invalidArguments( sender, CommandType.TABLES.getFormat() );
		}
	}
	
	/**
	 * This method handles the info command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private void onNewTable(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (IrcClient.getBot().userIsHost(IrcClient.getBot().getUser(sender), IrcChannel.getName())) {
			if (msg.length == 3 || msg.length == 4) {
				ProfileType profile = null;
				if (msg.length == 4) {
					profile = ProfileType.fromString(msg[3]);
				} else  {
					try {
						profile = Database.getInstance().getActiveProfile(sender);
					} catch (Exception e) {
						EventLog.log(e, "Lobby", "onNewTable");
					}
				}
				
				Integer stake = Utils.tryParse( msg[0] );
				Integer buy_in = Utils.tryParse( msg[1] );
				Integer max_players = Utils.tryParse( msg[2] );
				if (stake != null && buy_in != null && max_players != null)  {
					// Verify stake is between correct buy-in levels
					int maxbuy = (stake*Variables.MaxBuyIn);
					int minbuy = (stake*Variables.MinBuyIn);
					
					if ( profile == null ) {
						IrcClient.getBot().sendIRCNotice(sender, IrcBot.ValidProfiles);	
					} else if ( !validPlayers(max_players) ) {
						String out = Strings.InvalidTableSizeMsg.replaceAll("%size", Integer.toString(max_players));
						out = out.replaceAll("%allowed", Arrays.toString(Variables.AllowedTableSizes));
						IrcClient.getBot().sendIRCNotice( sender, out );
					} else if ( !validStake(stake) ) {
						String out = Strings.InvalidTableBBMsg.replaceAll("%bb", Integer.toString(stake));
						out = out.replaceAll("%allowed", Arrays.toString(Variables.AllowedBigBlinds));
						IrcClient.getBot().sendIRCNotice( sender, out );
					} else if ( buy_in < minbuy || buy_in > maxbuy) {
						String out = Strings.IncorrectBuyInMsg.replaceAll("%buyin", Integer.toString(buy_in) );
						out = out.replaceAll( "%maxbuy", Integer.toString(maxbuy) );
						out = out.replaceAll( "%minbuy", Integer.toString(minbuy) );
						out = out.replaceAll( "%maxBB", Integer.toString(Variables.MaxBuyIn) );
						out = out.replaceAll( "%minBB", Integer.toString(Variables.MinBuyIn) );
						IrcClient.getBot().sendIRCNotice( sender, out );
					} else if (!IrcClient.userHasCredits( sender, buy_in, profile ) ) {
						String out = Strings.NoChipsMsg.replaceAll( "%chips", Integer.toString(buy_in));
						out = out.replaceAll( "%profile", profile.toString() );
						IrcClient.getBot().sendIRCNotice(sender, out);
					} else {
						IrcClient.newTable( stake, max_players, profile, true);		
					}
				} else {
					invalidArguments( sender, CommandType.NEWTABLE.getFormat() );
				}
			} else {
				invalidArguments( sender, CommandType.NEWTABLE.getFormat() );
			}
		}
	}
	
	/**
	 * This method handles the watch command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private void onWatchTable(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 1 && msg[0].compareTo("") != 0) {
			Integer id = Utils.tryParse( msg[0] );
			if ( Table.getTables().containsKey(id) ) {
				Table table = IrcClient.getTable(id);
				if (table != null && table.canWatch( sender )) {
					IrcClient.newObserver( sender, id );
				} else {
					IrcClient.sendIRCNotice( sender, Strings.AlreadyWatchingMsg.replaceAll("%id", Integer.toString(id)) );
				}
			} else {
				IrcClient.getBot().sendIRCMessage(IrcChannel, Strings.NoTableIDMsg.replaceAll("%id", id.toString()));
			}
		} else {
			invalidArguments( sender, CommandType.WATCHTBL.getFormat() );
		}
	}
	
	/**
	 * This method handles the join command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private void onJoinTable(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		Integer table_id = null;
		Integer buy_in = null;
		if ((msg.length == 1 && msg[0].compareTo("") != 0) || msg.length == 2) {
			if (msg.length == 2) {
				table_id = Utils.tryParse( msg[0] );
				buy_in = Utils.tryParse( msg[1] );
			} else {
				table_id = Utils.tryParse( msg[0] );
				if (table_id != null) {
					Integer stake = Table.getTables().get( table_id );
					if (stake != null)
						buy_in = stake * Variables.MinBuyIn;
					else
						buy_in = null;
				}
			}
			
			if (table_id == null || buy_in == null) {
				invalidArguments( sender, CommandType.JOIN.getFormat() );
			} else if (!Table.getTables().containsKey(table_id) ) {
				IrcClient.getBot().sendIRCMessage(IrcChannel,
										 Strings.NoTableIDMsg.replaceAll("%id", table_id.toString()));
			} else {
				Table table = IrcClient.getTable(table_id);
				ProfileType profile = table.getProfile();
				int stake = Table.getTables().get( table_id );
				int maxbuy = (stake*Variables.MaxBuyIn);
				int minbuy = (stake*Variables.MinBuyIn);

				if ( buy_in < minbuy || buy_in > maxbuy) {
					String out = Strings.IncorrectBuyInMsg.replaceAll("%buyin", Integer.toString(buy_in) );
					out = out.replaceAll( "%maxbuy", Integer.toString(maxbuy) );
					out = out.replaceAll( "%minbuy", Integer.toString(minbuy) );
					out = out.replaceAll( "%maxBB", Integer.toString(Variables.MaxBuyIn) );
					out = out.replaceAll( "%minBB", Integer.toString(Variables.MinBuyIn) );
					IrcClient.sendIRCNotice( sender, out );
				} else if ( IrcClient.tableIsFull( table_id ) ) {
					IrcClient.sendIRCNotice( sender, Strings.TableFullMsg.replaceAll("%id",
																						Integer.toString(table_id)) );								
				} else if (!IrcClient.userHasCredits( sender, buy_in, profile ) ) {
					String out = Strings.NoChipsMsg.replaceAll( "%chips", Integer.toString(buy_in));
					out = out.replaceAll( "%profile", profile.toString() );
					IrcClient.sendIRCNotice(sender, out);
				} else {
					if (table != null && table.canPlay( sender )) {
						IrcClient.newPlayer( IrcClient.getBot().getUser(sender), table_id, buy_in );	
					} else {
						IrcClient.sendIRCNotice( sender, 
								Strings.AlreadyPlayingMsg.replaceAll("%id", Integer.toString(table_id)) );
					}												
				}
			}
		} else {
			invalidArguments( sender, CommandType.JOIN.getFormat() );
		}
	}
	
	/**
	 * This method is used to check the supplied stake is an allowed big blind
	 * 
	 * @param stake The big blind to check
	 * 
	 * @return true if the supplied value is allowed
	 */
	private boolean validStake(int stake) { return Arrays.asList(Variables.AllowedBigBlinds).contains(stake); }

	/**
	 * This method is used to check the supplied stake is an allowed table size
	 * 
	 * @param stake The number of players to check
	 * 
	 * @return true if the supplied value is allowed
	 */
	private boolean validPlayers(int players) { return Arrays.asList(Variables.AllowedTableSizes).contains(players); }
}
