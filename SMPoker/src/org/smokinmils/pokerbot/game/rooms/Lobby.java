/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.game.rooms;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.smokinmils.logging.EventLog;

import org.smokinmils.pokerbot.Client;
import org.smokinmils.pokerbot.Database;
import org.smokinmils.pokerbot.Utils;
import org.smokinmils.pokerbot.enums.CommandType;
import org.smokinmils.pokerbot.enums.RoomType;
import org.smokinmils.pokerbot.settings.Strings;
import org.smokinmils.pokerbot.settings.Variables;

public class Lobby extends Room {
	/* A timer to announce poker to the channel */
	Timer announceTimer;
	class AnnounceTask extends TimerTask {
		public void run() {
			ircClient.sendIRCMessage(ircChannel, Strings.PokerAnnounce);
		    announceTimer.schedule(new AnnounceTask(), Variables.AnnounceMins*60*1000);
		}
	}	
	
	public Lobby(String channel, Client irc) {
		super(channel, irc, RoomType.LOBBY);
		roomTopic = Strings.LobbyTopic;
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
			//case CHIPS:
				//disabled
				//onChips(sender, login, hostname, message);
				//break;
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
			case PROMOS:
				onJackpots(sender, login, hostname, message);
				break;
			//case GIVE:
				//disabled
				//onGive(sender, login, hostname, message);
				//break;
			case PROFILE:
				onProfile(sender, login, hostname, message);
				break;
			case PROFILES:
				onProfiles(sender, login, hostname, message);
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
			sendFormat(sender, CommandType.INFO.getCommandText(), CommandType.INFO.getFormat());
			for (String line: Strings.InfoMessage.split("\n")) {
				ircClient.sendIRCNotice( sender, line );
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
											CommandType.NEWTABLE, CommandType.WATCHTBL, 
											CommandType.JOIN, CommandType.PROMOS,
											/*CommandType.GIVE,*/ CommandType.PROFILE, CommandType.PROFILES};
				for (CommandType item: lobby_cmds) {
					sendFullCommand(sender, item);
					ircClient.sendIRCNotice(sender,"%b%c15-----");
				}
			} else {
				ircClient.sendIRCNotice(sender, Strings.InvalidInfoArgs.replaceAll("%invalid", msg[0]));
			}
		} else {
			invalidArguments( sender, CommandType.INFO.getFormat() );
		}
	}
		
	/**
	 * This method handles the chips command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 *
	@SuppressWarnings("unused")
	private void onChips(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		String user;
		
		if ((msg.length == 0 && msg[0].compareTo("") == 0) || msg.length == 1) {
			user = (msg[0].compareTo("") != 0 ? msg[0] : sender);
			Map<String,Integer> creds = Database.getInstance().checkAllCredits( user );
			
			String credstr;
			if (creds.size() != 0) {
				String active = Database.getInstance().getActiveProfile( user );
				if (user.compareToIgnoreCase(sender) == 0) {
					credstr = Strings.CheckCreditSelfMsg;
				} else  {
					credstr = Strings.CheckCreditMsg;
				}
				Integer active_creds = creds.get(active);
				if (active_creds == null) active_creds = 0;
				credstr = credstr.replaceAll("%creds", Integer.toString(active_creds));
				credstr = credstr.replaceAll("%active", active);
				
				if (creds.size() > 0) {
					credstr += " and ";
					for (Entry<String, Integer> cred: creds.entrySet()) {
						if (cred.getKey().compareTo(active) != 0) {
							String othercred = Strings.CreditsOtherProfiles.replaceAll("%name", cred.getKey());
							othercred = othercred.replaceAll("%amount",Integer.toString(cred.getValue()));
							credstr += othercred + " ";
						}
					}
				}
			} else {
				if (user.compareToIgnoreCase(sender) == 0) {
					credstr = Strings.NoCreditsSelf;
				} else  {
					credstr = Strings.NoCredits;
				}
			}
			credstr = credstr.replaceAll("%user", user);
			credstr = credstr.replaceAll("%sender", sender);
			
			ircClient.sendIRCMessage(ircChannel, credstr);		
		} else {
			invalidArguments( sender, CommandType.CHIPS.getFormat() );
		}
	}*/
	
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
			ircClient.sendIRCNotice(sender,
						Strings.AllTablesMsg.replaceAll("%count", Integer.toString(tables.size())));
			for (Entry<Integer, Integer> table: tables.entrySet()) {
				Table tbl = ircClient.getTable(table.getKey());				
				ircClient.sendIRCNotice(sender, tbl.formatTableInfo(Strings.TableInfoMsg));
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
				ircClient.sendIRCNotice(sender, Strings.NoTablesMsg.replaceAll("%bb", msg[0]));
			} else {
				String out = Strings.FoundTablesMsg.replaceAll("%bb", msg[0]);
				out = out.replaceAll("%count", Integer.toString(table_ids.size()) );
				out = out.replaceAll("%tables", table_ids.toString() );
				
				ircClient.sendIRCNotice(sender, out);
				
				for (Integer id: table_ids) {
					Table tbl = ircClient.getTable(id);			
					ircClient.sendIRCNotice(sender, tbl.formatTableInfo(Strings.TableInfoMsg));
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
		if (ircClient.isHost(sender, ircChannel)) {
			if (msg.length == 3 || msg.length == 4) {
				String profile;
				int profile_id = -1;
				if (msg.length == 4) {
					profile = msg[3];
				} else  {
					profile = Database.getInstance().getActiveProfile(sender);
				}
				profile_id = Database.getInstance().getProfileID(profile);
				
				Integer stake = Utils.tryParse( msg[0] );
				Integer buy_in = Utils.tryParse( msg[1] );
				Integer max_players = Utils.tryParse( msg[2] );
				if (stake != null && buy_in != null && max_players != null && !profile.isEmpty())  {
					// Verify stake is between correct buy-in levels
					int maxbuy = (stake*Variables.MaxBuyIn);
					int minbuy = (stake*Variables.MinBuyIn);
					
					if ( profile_id == -1 ) {
						List<String> profiles = Database.getInstance().getProfileTypes();
						ircClient.sendIRCNotice(sender, Strings.ValidProfiles.replaceAll("%profiles", profiles.toString()));
					} else if ( !validPlayers(max_players) ) {
						String out = Strings.InvalidTableSizeMsg.replaceAll("%size", Integer.toString(max_players));
						out = out.replaceAll("%allowed", Arrays.toString(Variables.AllowedTableSizes));
						ircClient.sendIRCNotice( sender, out );
					} else if ( !validStake(stake) ) {
						String out = Strings.InvalidTableBBMsg.replaceAll("%bb", Integer.toString(stake));
						out = out.replaceAll("%allowed", Arrays.toString(Variables.AllowedBigBlinds));
						ircClient.sendIRCNotice(sender, out  );
					} else if ( buy_in < minbuy || buy_in > maxbuy) {
						String out = Strings.IncorrectBuyInMsg.replaceAll("%buyin", Integer.toString(buy_in) );
						out = out.replaceAll( "%maxbuy", Integer.toString(maxbuy) );
						out = out.replaceAll( "%minbuy", Integer.toString(minbuy) );
						out = out.replaceAll( "%maxBB", Integer.toString(Variables.MaxBuyIn) );
						out = out.replaceAll( "%minBB", Integer.toString(Variables.MinBuyIn) );
						ircClient.sendIRCNotice( sender, out );
					} else if (!ircClient.userHasCredits( sender, buy_in, profile ) ) {
						String out = Strings.NoChipsMsg.replaceAll( "%chips", Integer.toString(buy_in));
						out = out.replaceAll( "%profile", profile );
						ircClient.sendIRCNotice(sender, out);
					} else {
						ircClient.newTable( stake, max_players, profile_id, true);		
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
				Table table = ircClient.getTable(id);
				if (table != null && table.canWatch( sender )) {
					ircClient.newObserver( sender, id );
				} else {
					ircClient.sendIRCNotice( sender, Strings.AlreadyWatchingMsg.replaceAll("%id", Integer.toString(id)) );
				}
			} else {
				ircClient.sendIRCMessage(ircChannel, Strings.NoTableIDMsg.replaceAll("%id", id.toString()));
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
				ircClient.sendIRCMessage(ircChannel,
										 Strings.NoTableIDMsg.replaceAll("%id", table_id.toString()));
			} else {
				Table table = ircClient.getTable(table_id);
				String profile = table.getProfile();
				int stake = Table.getTables().get( table_id );
				int maxbuy = (stake*Variables.MaxBuyIn);
				int minbuy = (stake*Variables.MinBuyIn);

				if ( buy_in < minbuy || buy_in > maxbuy) {
					String out = Strings.IncorrectBuyInMsg.replaceAll("%buyin", Integer.toString(buy_in) );
					out = out.replaceAll( "%maxbuy", Integer.toString(maxbuy) );
					out = out.replaceAll( "%minbuy", Integer.toString(minbuy) );
					out = out.replaceAll( "%maxBB", Integer.toString(Variables.MaxBuyIn) );
					out = out.replaceAll( "%minBB", Integer.toString(Variables.MinBuyIn) );
					ircClient.sendIRCNotice( sender, out );
				} else if ( ircClient.tableIsFull( table_id ) ) {
					ircClient.sendIRCNotice( sender, Strings.TableFullMsg.replaceAll("%id",
																						Integer.toString(table_id)) );								
				} else if (!ircClient.userHasCredits( sender, buy_in, profile ) ) {
					String out = Strings.NoChipsMsg.replaceAll( "%chips", Integer.toString(buy_in));
					out = out.replaceAll( "%profile", profile );
					ircClient.sendIRCNotice(sender, out);
				} else {
					if (table != null && table.canPlay( sender )) {
						ircClient.newPlayer( sender, table_id, buy_in );	
					} else {
						ircClient.sendIRCNotice( sender, 
								Strings.AlreadyPlayingMsg.replaceAll("%id", Integer.toString(table_id)) );
					}												
				}
			}
		} else {
			invalidArguments( sender, CommandType.JOIN.getFormat() );
		}
	}
	
	/**
	 * This method handles the promotions command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private void onJackpots(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			List<String> profiles = Database.getInstance().getProfileTypes();
			String jackpotstr = "";
			
			for (String profile: profiles) {
				Integer jackpot = null;
				try {
					BufferedReader readFile = new BufferedReader(new FileReader("jackpot." + profile));
					jackpot = Utils.tryParse(readFile.readLine()); 
					readFile.close();
				} catch (Exception e) {
				}
				 
				if (jackpot == null) jackpot = 0;
				
				jackpotstr += Strings.JackpotAmount.replaceAll("%profile",
						profile).replaceAll("%amount", Integer.toString(jackpot));
			}
			
			String out = Strings.JackpotInfo.replaceAll("%jackpots", jackpotstr);
			ircClient.sendIRCMessage(out);				
		} else {
			invalidArguments( sender, CommandType.PROMOS.getFormat() );
		}
	}
	
	/**
	 * This method handles the give command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	private void onGive(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");

		if (ircClient.isHost(sender, ircChannel)) {
			if (msg.length == 3) {
				String user = msg[0];
				String profile = msg[2];
				Integer amount = Utils.tryParse(msg[1]);
				
				if (amount != null) {
					List<String> profiles = Database.getInstance().getProfileTypes();
					
					if ( profiles.contains(profile) ) {
						boolean success = Database.getInstance().giveChips(user, amount, profile);
						if (success) {
							String out = Strings.GiveChips.replaceAll("%amount", Integer.toString(amount));
							out = out.replaceAll("%who", user);
							out = out.replaceAll("%sender", sender);
							out = out.replaceAll("%profile", profile);
							ircClient.sendIRCMessage(ircChannel, out);
							
							out = Strings.GiveChipsPM.replaceAll("%amount", Integer.toString(amount));
							out = out.replaceAll("%who", user);
							out = out.replaceAll("%sender", sender);
							out = out.replaceAll("%profile", profile);
							ircClient.sendIRCNotice(user, out);
						} else {
							EventLog.log(sender + " attempted to give someone chips and the database failed", "Lobby", "onGive");
						}
					} else {
						ircClient.sendIRCMessage(ircChannel, Strings.ValidProfiles.replaceAll("%profiles", profiles.toString()));
					}
				} else {
					invalidArguments( sender, CommandType.GIVE.getFormat() );
				}
			} else {
				invalidArguments( sender, CommandType.GIVE.getFormat() );
			}
		} else {
			EventLog.info(sender + " attempted to give someone chips", "Lobby", "onGive");
		}
	}*/
	
	/**
	 * This method handles the profile command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private void onProfile(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 1 && msg[0].compareTo("") != 0) {
			List<String> profiles = Database.getInstance().getProfileTypes();
			
			if (profiles.contains(msg[0])) {
				boolean success = Database.getInstance().updateActiveProfile(sender, msg[0]);
				if (success) {
					String out = Strings.ProfileChanged.replaceAll("%user", sender);
					out = out.replaceAll("%profile", msg[0]);
					ircClient.sendIRCMessage(ircChannel, out);
				} else {
					String out = Strings.ProfileChangeFail.replaceAll("%user", sender);
					out = out.replaceAll("%profile", msg[0]);
					ircClient.sendIRCMessage(ircChannel, out);
					EventLog.log(out, "Lobby", "onProfile");
				}				
			} else {
				ircClient.sendIRCMessage( ircChannel,
						Strings.ValidProfiles.replaceAll("%profiles", profiles.toString()) );
			}
		} else {
			invalidArguments( sender, CommandType.PROFILE.getFormat() );
		}
	}
	
	/**
	 * This method handles the profile command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private void onProfiles(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			List<String> profiles = Database.getInstance().getProfileTypes();
			ircClient.sendIRCMessage(ircChannel, Strings.ValidProfiles.replaceAll("%profiles", profiles.toString()));
		} else {
			invalidArguments( sender, CommandType.PROFILE.getFormat() );
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
