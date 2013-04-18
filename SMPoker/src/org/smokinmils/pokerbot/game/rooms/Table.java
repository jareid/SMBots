/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.game.rooms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TreeMap;
import java.util.Vector;

import org.jibble.pircbot.User;
import org.smokinmils.pokerbot.Client;
import org.smokinmils.pokerbot.Database;
import org.smokinmils.pokerbot.Utils;
import org.smokinmils.pokerbot.enums.ActionType;
import org.smokinmils.pokerbot.enums.CommandType;
import org.smokinmils.pokerbot.enums.RoomType;
import org.smokinmils.pokerbot.game.Card;
import org.smokinmils.pokerbot.game.Deck;
import org.smokinmils.pokerbot.game.Hand;
import org.smokinmils.pokerbot.game.HandValue;
import org.smokinmils.pokerbot.game.SidePot;
import org.smokinmils.pokerbot.logging.EventLog;
import org.smokinmils.pokerbot.settings.Strings;
import org.smokinmils.pokerbot.settings.Variables;
import org.smokinmils.pokerbot.tasks.TableTask;

/**
 * The IRC and poker functionality for a poker table
 * 
 * @author Jamie Reid
 */
public class Table extends Room {	
	/** Stores the most recent table's ID */
	private static Integer CurrentID = 0;
	
	/** A map of all tables and their big blind */
	private static Map<Integer,Integer> tables = new HashMap<Integer,Integer>();

	/** The ID of this table */
	private int tableID;
	
	/** The profile ID of this table */
	private int profileID;
	private String profileName;
    
	/** The size of the big blind. */
	private int bigBlind;
	
	/** The size of the small blind. */
	private int smallBlind;
	
	/** The players at the table. */
	private List<Player> players;
	
    /** The active players in the current hand. */
    private final List<Player> activePlayers;
    
    /** The players at the table but sitting out. */
    private final List<Player> satOutPlayers;
	
	/** The observers watching the table. */
	private Vector<String> observers;
	
	/** The maximum buy in permitted */
	private int minPlayers;
	
	/** The maximum buy in permitted */
	private int maxPlayers;
	
	/** The maximum buy in permitted */
	private int minBuyIn;
	
	/** The minimum buy in permitted */
	private int maxBuyIn;
	
	/** The maximum buy in permitted */
	private int handID;
	
    /** The maximum number of bets or raises in a single hand per player. */
    private static final int MAX_RAISES = 4;
    
    /** The deck of cards. */
    private Deck deck;
    
    /** The community cards on the board. */
    private List<Card> board;

    /** The current dealer position. */
    private int dealerPosition;
    
    /** The position of the acting player. */
    private int actorPosition;
    
    /** The acting player. */
    private Player actor;

    /** The minimum bet in the current hand. */
    private int minBet;
    
    /** The current bet in the current hand. */
    private int bet;
    
    /** The pot in the current hand. */
    private int pot;
    
    /** Marks the table as having an active hand */
    private boolean handActive;
	
    /** Timer to handle waiting for enough players */
	private Timer waitForPlayersTimer;
	private int waitedCount;
	
	/** Timer to start a game */
	private Timer startGameTimer;
	
	/** Timer to wait for an action */
	private Timer actionTimer;
	
	/** Action received lock */
	private boolean actionReceived;
	
	/** Boolean used for when a hand was cancelled due to a disconnect */
	private boolean disconnected;
	
	/** Side pots */
	private List<SidePot> sidePots;
	
	/** Player bets */
	private Map<Player,Integer> playerBets;
	
	/** Rounds */
	private static final int PREFLOP = 0;
	private static final int FLOP 	 = 1;
	private static final int TURN	 = 2;
	private static final int RIVER	 = 3;
	
	/** The round of the current hand */
	private int currentRound;
	
	/** Number of players left to act */
	private volatile int playersToAct;
	
	/** Number of players left to act */
	private boolean createdManually;
	
    /**
     * Constructor.
     * 
     * @param channel  The channel this bot is running on
     * @param irc	   The IRC client
     * @param bb	   The size of the big blind.
     * @param max	   Maximum allowed players on the table
     * @param profile  The profile ID this table is for
     * @param manual   Whether the table was created auto or manually
     */
	public Table(String channel, Client irc, int id, int bb, int max, int profile, boolean manual) {
    	super(channel, irc, RoomType.TABLE);	
    	boolean failed = false;
		players = new ArrayList<Player>();
		activePlayers = new ArrayList<Player>();
		satOutPlayers = new ArrayList<Player>();
        board = new ArrayList<Card>();

		sidePots = new ArrayList<SidePot>();
        
        try {
	        deck = null;
	        deck = new Deck();
		} catch (Exception e) {
			failed = true;
			EventLog.fatal(e, "Table", "Constructor");
		}
        
		startGameTimer = null;
		actionTimer = null;
		waitForPlayersTimer = null;
		waitedCount = 0;
		handActive = false;
		disconnected = false;
		
		createdManually = manual;
		
		CurrentID++;
		tableID = id;
		
		try{
			profileID = profile;
			if ( profileID == -1 )
				throw new IllegalArgumentException("ProfileID can not be -1");
		} catch (Exception e) {
			failed = true;
			EventLog.fatal(e, "Table", "Constructor");
		}
		
		try{
			profileName = Database.getInstance().getProfileName( profile );
			if ( profileName.isEmpty() )
				throw new IllegalArgumentException("Profile name can not be null or empty");
		} catch (Exception e) {
			failed = true;
			EventLog.fatal(e, "Table", "Constructor");
		}
		
		observers = new Vector<String>();
		maxPlayers = max;
		minPlayers = Variables.MinPlayers;
		playersToAct = 0;
		currentRound = 0;
		actionReceived = false;
		
		// Ensure big blind is even
		if (bb <= 1) bb = 2;
		if ( (bb % 2) == 1 ) bb += 1;		
		tables.put(tableID, bb);		
		bigBlind = bb;
		smallBlind = bb / 2;
		
		// Calculate min and max buyins
		minBuyIn = Variables.MinBuyIn * bigBlind;
		maxBuyIn = Variables.MaxBuyIn * bigBlind;
		
		dealerPosition = -1;
		handID = -1;
		
		setTopic();
		scheduleWaitForPlayers();
		if (failed) {
			ircClient.sendIRCMessage("Something caused the bot to crash... please notify the staff.");
			System.exit(1);
		}
	}
	
	/** 
	 * Returns the table ID
	 * 
	 * @return The table ID
	 */
	public int getTableID() { return tableID; }
	
	/** 
	 * Returns the profile ID
	 * 
	 * @return The profile ID
	 */
	public int getProfileID() { return profileID; }
	
	/** 
	 * Returns the profile name
	 * 
	 * @return The profile name
	 */
	public String getProfile() { return profileName; }
	
	/** 
	 * Returns the small blind
	 * 
	 * @return The small blind
	 */
	public int getSmallBlind() { return smallBlind; }
	
	/** 
	 * Returns the big blind
	 * 
	 * @return The big blind
	 */
	public int getBigBlind() { return bigBlind; }
	
	/** 
	 * Returns the players at the table
	 * 
	 * @return The players at the table
	 */
	public Player[] getPlayers() { return (Player[]) players.toArray(); }
	
	/** 
	 * Returns the minimum buy in
	 * 
	 * @return The minimum buy in
	 */
	public int getMinBuyIn() { return minBuyIn; }
	
	/** 
	 * Returns the maximum buy in
	 * 
	 * @return The maximum buy in
	 */
	public int getMaxBuyIn() { return maxBuyIn; }
	
	/** 
	 * Returns the number of players at the table
	 * 
	 * @return The number of players
	 */
	public int getNoOfPlayers() { return players.size() + satOutPlayers.size(); }
	
	/** 
	 * Returns the minimum number of players for play to commence
	 * 
	 * @return The minimum number of players
	 */
	public int getMinPlayers() { return minPlayers; }
	
	/** 
	 * Returns the maximum number of players for play to commence
	 * 
	 * @return The maximum number of players
	 */
	public int getMaxPlayers() { return maxPlayers; }
	
	/** 
	 * Returns the number of players who are sat down/active
	 * 
	 * @return The number of players
	 */
	public int getPlayersSatDown() { return players.size(); }
	
	/** 
	 * Returns the next table ID
	 * 
	 * @return The next available ID
	 */
	public static final Integer getNextID() { 
		Integer next_id = CurrentID+1;
		for (Integer i = 1; i < next_id; i++) {
			if (!tables.containsKey(i)) {
				next_id = i;
				break;
			}
		}
		return next_id;
	}
	
	/** 
	 * Returns the Map of all tables and their big blind
	 * 
	 * @return The map of all table IDs and big blinds
	 */
	public static Map<Integer, Integer> getTables( ) { return tables; }
	
	/**
	 * This method checks if the table is full
	 * 
	 * @return true if the table is full. false otherwise
	 */
	public boolean isFull() { return ((players.size() + satOutPlayers.size()) == maxPlayers); }
	
	/**
	 * This method returns how many seats are left
	 * 
	 * @return the number of remaining seats
	 */
	public int seatsLeft() { return (maxPlayers - players.size()); }
	
	/**
	 * This method is used to check if a user is already watching the table
	 * 
	 * @return true if the user is not already watching
	 */
	public boolean canWatch(String user) { return observers.contains( user ); }
	
	/**
	 * This method is used to check if a user is already playing at the table
	 * 
	 * @return true if the user is not already playing
	 */
	public boolean canPlay(String user) {
		Player found = findPlayer(user);
		return (found == null ? true : false);
	}

	
 
    
    /**
     * Outputs a table message replacing all variables
     * 
     * @param The unformatted string
     * 
     * @return The formatted string
     */
    public String formatTableInfo(String in) {
		int seats = maxPlayers - (players.size() + satOutPlayers.size());
		in = in.replaceAll( "%id", Integer.toString(tableID) );
		in = in.replaceAll( "%sb", Integer.toString(smallBlind) );
		in = in.replaceAll( "%bb", Integer.toString(bigBlind) );
		in = in.replaceAll( "%min", Integer.toString(minBuyIn) );
		in = in.replaceAll( "%max", Integer.toString(maxBuyIn) );
		in = in.replaceAll( "%Pcur", Integer.toString(players.size() + satOutPlayers.size()) );
		in = in.replaceAll( "%Pmin", Integer.toString(minPlayers) );
		in = in.replaceAll( "%Pmax", Integer.toString(maxPlayers) );
		in = in.replaceAll( "%seats", Integer.toString(seats) );
		in = in.replaceAll( "%watching", Integer.toString(observers.size())  );
		in = in.replaceAll( "%hID", Integer.toString(handID) );
		in = in.replaceAll( "%profile", profileName );
    	return in;
    }
   
    
    /**
     * Adds a player to the table
     * 
     * @param player  The player.
     */
	public synchronized void playerJoins(String sender, Integer buy_in) {
		Player player = new Player(sender, buy_in);
		boolean found = false;
		User[] users = ircClient.getUsers(ircChannel);
		for (int i = 0; i < users.length; i++) {
			String nick = users[i].getNick();
			if (nick.compareTo(sender) == 0) {
				found = true;
			}
		}
			
    	if (found) {
    		players.add(player);
    	} else {
    		// Add Timer
    		satOutPlayers.add(player);
    		player.scheduleSitOut(this);
    	}
		
		// Remove chips from db
		Database.getInstance().buyIn(sender, buy_in, profileID);
		Database.getInstance().addPokerTableCount(sender, tableID, profileID, buy_in);
		
		// Invite the player to join channel
		ircClient.sendInvite( sender, ircChannel );
		
		// Try to voice the user incase they were watching
		ircClient.voice( ircChannel, sender );
		observers.remove( sender );
		
		// Announce
		String out = Strings.PlayerJoins.replaceAll("%id", Integer.toString(tableID) );
		out = out.replaceAll( "%player", player.getName() );
		out = out.replaceAll( "%chips",Integer.toString(player.getChips()) );		
		ircClient.sendIRCMessage( ircChannel, out);
		setTopic();
	}
	
    /**
     * A player has left the table
     * 
     * @param player the player who left
     * @param sat_out true if the player was sat out prior to leaving
     */
    public synchronized void playerLeaves(Player player, boolean sat_out) {
    	String name = player.getName();
    	int chips = player.getChips() + player.getRebuy();
    	
    	// Ensure user has left the table
    	if (createdManually) {
    		ircClient.kick( ircChannel, name );
    	} else {
    		ircClient.deVoice( ircChannel, name );
    	}
    	
    	// Cash out
		Database.getInstance().cashOut( name, chips, profileID );
		Database.getInstance().addPokerTableCount(player.getName(), tableID, profileID, 0);
    	
    	// Remove from our player list
    	if (sat_out) {
	    	satOutPlayers.remove(player);
	    	// Cancel timer
			if (player.getSittingOutTimer() != null) player.getSittingOutTimer().cancel();
    	} else {
    		players.remove(player);
    	}
		
		// Announce
		String out = Strings.PlayerLeaves.replaceAll("%id", Integer.toString(tableID) );
		out = out.replaceAll( "%player", name );
		out = out.replaceAll( "%chips",Integer.toString(chips) );		
		ircClient.sendIRCMessage( ircChannel, out);
		ircClient.sendIRCNotice( player.getName(),
						Strings.PlayerLeavesPM.replaceAll("%id", Integer.toString(tableID)) );
		setTopic();
    }
    
    private synchronized void playerSitsDown(Player player) {
    	if (player.getSittingOutTimer() != null) player.getSittingOutTimer().cancel();
    	
		// Switch the player lists
    	satOutPlayers.remove(player);
    	players.add(player);
    	
		player.setSatOut(false);
    	
    	// Announce
		String out = Strings.PlayerSitsDown.replaceAll("%id", Integer.toString(tableID));
		out = out.replaceAll("%player", player.getName());
		ircClient.sendIRCMessage( ircChannel, out);
    }
    
    /**
     * Called when a player is auto sat out or chooses to sit out.
     * 
     * @param player
     */
    private synchronized void playerSitsOut(Player player) {
    	// Cancel timer
		player.scheduleSitOut(this);
		
		// Switch the player lists
    	players.remove(player);
    	satOutPlayers.add(player); 
    	
		// handle current hands
		if (player == actor && handActive) 
	    	onAction(ActionType.FOLD, "", true);
		else if (activePlayers.contains(player)) {
			playersToAct--;
	    	activePlayers.remove(player);
			if (activePlayers.size() == 1) {
				playerWins(activePlayers.get(0));
			}
		}   	

		player.setSatOut(true);
    	
    	// Announce
		String out = Strings.PlayerSitsOut.replaceAll("%id", Integer.toString(tableID));
		out = out.replaceAll("%player", player.getName());
		ircClient.sendIRCMessage( ircChannel, out);
    }
    
    /**
     * This method is called whenever a message is sent to this channel.
     *
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
	protected synchronized void onMessage(String sender, String login, String hostname, String message) {
		int fSpace = message.indexOf(" ");
		if(fSpace == -1) fSpace = message.length();
		String firstWord = message.substring(0, fSpace).toLowerCase();
		
		if ((fSpace + 1) <= message.length()) fSpace++;
		String restofmsg = message.substring( fSpace, message.length() );
		String[] msg = restofmsg.split(" ");
		
		CommandType cmd = CommandType.fromString( firstWord );
		if (cmd != null) {
			boolean isActor = (actor != null ? (actor.getName().compareToIgnoreCase( sender ) == 0) : false);
			switch (cmd) {
			case CHECK:
				if ((msg.length == 0 || msg[0].compareTo("") == 0) && isActor) {
					if (bet == 0 || actor.getBet() >= bet ) onAction( ActionType.CHECK, restofmsg, false );
					else onAction( ActionType.CALL, restofmsg, false);
				} else if (!isActor) {
					invalidAction( sender );
				} else {
					invalidArguments( sender, cmd.getFormat() );
				}
				break;
			case RAISE:
				if (msg.length == 1 && msg[0].compareTo("") != 0 && isActor) {
					if (bet == 0) onAction( ActionType.BET, restofmsg, false );
					else onAction( ActionType.RAISE, restofmsg, false );	
				} else if (!isActor) {
					invalidAction( sender );
				} else {
					invalidArguments( sender, cmd.getFormat() );
				}
				break;
			case FOLD:
				if ((msg.length == 0 || msg[0].compareTo("") == 0) && isActor) {
					onAction( ActionType.FOLD, restofmsg, false );			
				} else if (!isActor) {
					invalidAction( sender );
				} else {
					invalidArguments( sender, cmd.getFormat() );
				}
				break;
			case TBLCHIPS:
				onChips(sender, login, hostname, restofmsg);
			case REBUY:
				onRebuy(sender, login, hostname, restofmsg);
				break;
			case SITDOWN:
				onSitDown(sender, login, hostname, restofmsg);
				break;
			case LEAVE:
				onLeave(sender, login, hostname, restofmsg);
				break;
			case SITOUT:
				onSitOut(sender, login, hostname, restofmsg);
				break;
			default:
				// Nothing to be done
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
	protected synchronized void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String notice) {}
    
    /**
     * This method is called whenever someone joins a channel which we are on.
     *
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
	protected synchronized void onJoin(String sender, String login, String hostname) {
    	if ( sender.compareToIgnoreCase( ircClient.getNick() ) != 0) {
    		Player found = null;
    		for (Player plyr: satOutPlayers) {
    			if (plyr.getName().compareToIgnoreCase( sender ) == 0) {
    				found = plyr;
    				break;
    			}
    		}
    		
    		if ( found != null) {
    			ircClient.voice( ircChannel, sender );
    			playerSitsDown( found );
    		} else {
    			observers.add( sender.toLowerCase() );
    		}
    	} else {
    		ircClient.setMode( ircChannel, "+m");
    	}
		setTopic();
    }    
    
    /**
     * This method is called whenever someone parts this channel which we are on.
     * This is also the handler for whenever someone quits from the channel
     *
     * @param sender The nick of the user who parted the channel.
     * @param login The login of the user who parted from the channel.
     * @param hostname The hostname of the user who parted from the channel.
     */
	protected synchronized void onPart(String sender, String login, String hostname) {
    	if ( sender.compareToIgnoreCase( ircClient.getNick() ) != 0) {
    		Player found = null;
    		for (Player plyr: players) {
    			if (plyr.getName().compareToIgnoreCase( sender ) == 0) {
    				found = plyr;
    				break;
    			}
    		}
    		
    		if (found != null) {   	
    			playerSitsOut(found);
    			
    			// not enough players, wait.
    			if (players.size() < minPlayers)  {
    				if (waitForPlayersTimer == null) scheduleWaitForPlayers();
    			}
    		} else if (observers.contains(sender.toLowerCase())) {
    			observers.remove(sender.toLowerCase());
    		}
    	}
	}
    
    /**
     * This method is called whenever someone changes nick on this channel.
     *
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The hostname of the user.
     * @param newNick The new nick.
     */
	protected synchronized void onNickChange(String oldNick, String login, String hostname, String newNick) {}
	
    /**
     * Called when a user (possibly us) gets granted operator status for a channel.
     *
     * @param channel 		 The channel in which the mode change took place.
     * @param sourceNick 	 The nick of the user that performed the mode change.
     * @param sourceLogin 	 The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode change.
     * @param recipient 	 The nick of the user that got 'opped'.
     */
    protected synchronized void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    	if ( sourceNick.compareToIgnoreCase( ircClient.getNick() ) == 0) {
    		setTopic();
    		ircClient.setMode( ircChannel, "+m");
    	}
    }
    
   /**
    * Called when a user needs to send a message back to the room
    *
    * @param timerName The type of timer that requires attention.
    */
   protected synchronized void onTimer(String timerName) {	   
	   switch (timerName) {
	   case TableTask.ActionTaskName:
		   if (!actionReceived) {
			   String out = Strings.NoActionWarning.replaceAll("%hID", Integer.toString(handID) );
		    	out = out.replaceAll("%actor", actor.getName() );
		    	out = out.replaceAll("%secs", Integer.toString(Variables.ActionWarningTimeSecs) );
		    	ircClient.sendIRCMessage( ircChannel, out );
			   
			   if (actionTimer != null) actionTimer.cancel();
			   actionTimer = new Timer();
			   actionTimer.schedule( new TableTask( this, TableTask.ActionWarningTaskName), Variables.ActionWarningTimeSecs*1000);
		   }
		   break;
	   case TableTask.ActionWarningTaskName:
		   if (actionTimer != null) actionTimer.cancel();
		   if (!actionReceived) {
			   noActionReceived();
		   }
		   break;
	   case TableTask.StartGameTaskName:
		   if (startGameTimer != null) startGameTimer.cancel();
			// Do we need players at the table?
			if ( players.size() < minPlayers ) {
				// Not enough players, wait again
				scheduleWaitForPlayers();
			} else if (!handActive) {
				// No players needed, start playing
				nextHand();
			}
			
		   break;
	   case TableTask.WaitForPlayersTaskName:
		   if (waitForPlayersTimer != null) waitForPlayersTimer.cancel();
			// Do we need players at the table?
			if (getNoOfPlayers() == 0) {
				if (createdManually && waitedCount > Strings.MaxWaitCount) closeTable();
				else {
					waitedCount++;					
					scheduleWaitForPlayers();
				}
			} else if ( players.size() < minPlayers ) {
				waitedCount = 0;
				// Continue waiting
				int need = minPlayers - players.size();
				String out = Strings.WaitingForPlayersMsg.replaceAll("%need", Integer.toString(need) );
				out = out.replaceAll("%min", Integer.toString(minPlayers) );
				out = out.replaceAll("%max", Integer.toString(maxPlayers) );
				out = out.replaceAll("%seated", Integer.toString(players.size()) );
				ircClient.sendIRCMessage(ircChannel, out);
				
				scheduleWaitForPlayers();
			} else {
				String out = Strings.GameStartMsg.replaceAll("%bb", Integer.toString(getBigBlind()) );
				out = out.replaceAll("%sb", Integer.toString(getSmallBlind()) );
				out = out.replaceAll("%secs", Integer.toString(Strings.GameStartSecs) );
				out = out.replaceAll("%seatedP", Integer.toString(getPlayersSatDown()) );
				ircClient.sendIRCMessage(ircChannel, out);

				waitedCount = 0;
				
				// Schedule the game to start
				startGameTimer = new Timer();
				startGameTimer.schedule(new TableTask( this, TableTask.StartGameTaskName ),
						Strings.GameStartSecs*1000);
			}
		   break;
	   }
   }
   
   /**
	* This method handles the chips command
	*
	* @param sender The nick of the person who sent the message.
    * @param login The login of the person who sent the message.
    * @param hostname The hostname of the person who sent the message.
    * @param message The actual message sent to the channel.
	*/	
	private synchronized void onChips(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if ((msg.length == 1 && msg[0].compareTo("") == 0)) {		
			Player found = findPlayer( sender );
				
			if (found != null) {
				String out = Strings.CheckChips.replaceAll( "%id", Integer.toString(tableID) );
				out = out.replaceAll( "%creds", Integer.toString(found.getChips()) );		
				ircClient.sendIRCMessage( ircChannel, out );
			} else {
				String out = Strings.CheckChipsFailed.replaceAll( "%id", Integer.toString(tableID) );	
				ircClient.sendIRCMessage( ircChannel, out );
			}
		} else {
			invalidArguments( sender, CommandType.REBUY.getFormat() );
		}
	}
	
	/**
	 * This method handles the rebuy command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private synchronized void onRebuy(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		Integer buy_in = Utils.tryParse( msg[0] );
		if ((msg.length == 1 && msg[0].compareTo("") != 0) || buy_in != null) {
			int maxbuy = (bigBlind*Variables.MaxBuyIn);
			int minbuy = (bigBlind*Variables.MinBuyIn);
			
			if (buy_in != null && !ircClient.userHasCredits( sender, buy_in, profileID ) ) {
				String out = Strings.NoChipsMsg.replaceAll( "%chips", Integer.toString(buy_in));
				out = out.replaceAll( "%profile", profileName );
				ircClient.sendIRCNotice(sender, out);
			} else if (buy_in != null) {
				Player found = findPlayer(sender);
				
				if (found != null) {
					int total = buy_in + found.getRebuy() + found.getChips();
					int diff = maxbuy - total;
					
					if ( total >= maxbuy ) {
						String out = Strings.RebuyFailure.replaceAll( "%id", Integer.toString(tableID) );
						out = out.replaceAll( "%maxbuy", Integer.toString(maxbuy) );
						out = out.replaceAll( "%total", Integer.toString(found.getChips() + found.getRebuy()) );		
						ircClient.sendIRCNotice( sender, out );
					} else if (diff < 0) {
						String out = Strings.IncorrectBuyInMsg.replaceAll("%buyin", Integer.toString(buy_in) );
						out = out.replaceAll( "%maxbuy", Integer.toString(maxbuy) );
						out = out.replaceAll( "%minbuy", Integer.toString(minbuy) );
						out = out.replaceAll( "%maxBB", Integer.toString(Variables.MaxBuyIn) );
						out = out.replaceAll( "%minBB", Integer.toString(Variables.MinBuyIn) );
						ircClient.sendIRCNotice(sender, out);
					} else {
						// Remove chips from db
						Database.getInstance().buyIn(sender, buy_in, profileID);
			    		Database.getInstance().addPokerTableCount(found.getName(), tableID, profileID, buy_in);
						
						found.rebuy(buy_in);
						
						String out = Strings.RebuySuccess.replaceAll( "%id", Integer.toString(tableID) );
						out = out.replaceAll( "%user", found.getName() );
						out = out.replaceAll( "%new", msg[0] );
						out = out.replaceAll( "%total", Integer.toString(found.getChips() + found.getRebuy()) );		
						ircClient.sendIRCMessage( ircChannel, out );
						
						if (satOutPlayers.contains(found)) playerSitsDown(found);
					}
				} else {
					EventLog.log("RECOVERABLE: User tried to rebuy but is not on the table", "Table", "onRebuy");
					ircClient.deVoice(ircChannel, sender);
					observers.add(sender.toLowerCase());
				}
			}	
		} else {
			invalidArguments( sender, CommandType.REBUY.getFormat() );
		}
	}
	
	/**
	 * This method handles the leave command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private synchronized void onLeave(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			Player found = findPlayer( sender );
			boolean is_active = (found != null ? players.contains(found) : false);

			if (found != null) { 
				playerLeaves(found, is_active);
			} else {
				EventLog.log(sender + "failed to leave as they should not have been voiced.", "Table", "onLeave");
				ircClient.deVoice(ircChannel, sender);
			}
		} else {
			invalidArguments( sender, CommandType.LEAVE.getFormat() );
		}
	}
	
	/**
	 * This method handles the sitdown command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private synchronized void onSitDown(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			Player found = null;
			for (Player plyr: satOutPlayers) {
				if (plyr.getName().compareToIgnoreCase( sender ) == 0) {
					found = plyr;
					break;
				}
			}
			
			if (found != null && (found.isBroke() && found.getRebuy() == 0)) {
				ircClient.sendIRCNotice( sender, Strings.SitOutFailed.replaceAll("%id", Integer.toString(tableID)) );
			} else if (found != null) {
				playerSitsDown(found);
			}
		} else {
			invalidArguments( sender, CommandType.SITDOWN.getFormat() );
		}
	}
	
	/**
	 * This method handles the sitout command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */	
	private synchronized void onSitOut(String sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			Player found = null;
			for (Player plyr: players) {
				if (plyr.getName().compareToIgnoreCase( sender ) == 0) {
					found = plyr;
					break;
				}
			}
			
			if (found != null) {
				playerSitsOut(found);
			} else {
				ircClient.sendIRCNotice( sender, Strings.SitOutFailed.replaceAll("%id", Integer.toString(tableID)) );
			}
		} else {
			invalidArguments( sender, CommandType.SITOUT.getFormat() );
		}
	}
	
	/**
	 * Announces the next actor and valid actions and waits
	 */
	private synchronized void getAction() {
		Set<ActionType> allowed = getAllowedActions(actor);
		String actions = allowed.toString();
		if ( allowed.contains(ActionType.CALL) )
			actions += " {" + (bet - actor.getBet()) + " to call}";
		if ( allowed.contains(ActionType.BET) )
			actions += " {" + minBet + " to bet}";
		if ( allowed.contains(ActionType.RAISE) )
			actions += " {" + ((bet - actor.getBet()) + minBet) + " to raise}";
		
		String out = Strings.GetAction.replaceAll( "%hID", Integer.toString(handID) );
		out = out.replaceAll( "%actor", actor.getName() );
		out = out.replaceAll( "%valid", actions );
		out = out.replaceAll( "%hID", Integer.toString(handID) );		
		ircClient.sendIRCMessage( ircChannel, out );
		
		actionReceived = false;
		
		// Schedule time out
		actionTimer = new Timer();
		actionTimer.schedule( new TableTask( this, TableTask.ActionTaskName ),
				(Variables.ActionTimeSecs-Variables.ActionWarningTimeSecs)*1000 );
	}
	
    /**
     * This method handles the results of an action and moves to the next action
     */
    private synchronized void actionReceived(boolean is_first) {
		try {
			rotateActor();
	    	while (actor.isBroke() && playersToAct > 1) {
	    		if (actor.isBroke()) playersToAct--;
	    		rotateActor();
	    	} 
		} catch (IllegalStateException e) {
			EventLog.fatal(e,"Table", "actionReceived");
			System.exit(1);
		}
    	
    	if (playersToAct > 0 && !(is_first && playersToAct == 1)) {
    		getAction();
    	} else {
    		// Reset the results of betting in the previous round
    		for (Player player : activePlayers) { player.resetBet(); }
    		
    		// deal next round
    		switch (currentRound) {
    		case PREFLOP:
    			currentRound++;
                if (activePlayers.size() > 1) {
                    bet = 0;
                    minBet = bigBlind;
                    dealCommunityCards("Flop", 3);
                    doBettingRound();
                }
    			break;
    		case FLOP:
    			currentRound++;
                if (activePlayers.size() > 1) {
                    bet = 0;
                    dealCommunityCards("Turn", 1);
                    minBet = bigBlind;
                    doBettingRound();
                }
    			break;
    		case TURN:
    			currentRound++;
                if (activePlayers.size() > 1) {
                    bet = 0;
                    dealCommunityCards("River", 1);
                    minBet = bigBlind;
                    doBettingRound();
                }
    			break;
    		case RIVER:
    			currentRound++;
                if (activePlayers.size() > 1) {
                    bet = 0;
                    doShowdown();
                }
    			break;
    		default:
    			throw new IllegalStateException("We have reached an unknown round");
    		}
    	}
    }
	
    /**
     * This method is called whenever the correct users acts
     * 
     * @param sourceNick The nick of the user that sent the notice.
     * @param sourceLogin The login of the user that sent the notice.
     * @param sourceHostname The hostname of the user that sent the notice.
     * @param extra Additional data for bet/raise
     */
	private synchronized void onAction(ActionType action, String extra, boolean timeout) {
		actionReceived = true;
		if (actionTimer != null) actionTimer.cancel();
		if (actor != null) {
			Set<ActionType> allowedActions = getAllowedActions(actor);        
	        if (!allowedActions.contains(action)) {
	            String out = Strings.InvalidAction.replaceAll("%hID", Integer.toString(handID));
	            out = out.replaceAll("%invalid", action.getName() );
	            out = out.replaceAll("%valid", allowedActions.toString() );
	            ircClient.sendIRCMessage( ircChannel, out );
	            ircClient.sendIRCNotice( actor.getName(), out );
	        } else {
	        	Integer amount = Utils.tryParse(extra);
	        	if (amount == null) amount = 0;
	        	
	        	int valid = 0;
	        	boolean stop = false;
	    		if ( action == ActionType.CALL ) {
	    			valid = bet - actor.getBet();
	    			if (actor.getChips() >= valid) {
	    				amount = valid;
					} else if (amount > actor.getChips()) {
						amount = actor.getChips();
					}
	    		} else if ( action == ActionType.BET ) {
	    			valid = minBet;
	    			if (amount < valid && actor.getChips() >= valid) {
	    				String out = Strings.InvalidBet.replaceAll("%hID", Integer.toString(handID));
	    				out.replaceAll("%pChips", Integer.toString(actor.getChips()));
	    				out.replaceAll("%min", Integer.toString(valid));
	    				stop = true;
	    			} else if (amount > actor.getChips()) {
	    				amount = actor.getChips();
	    			}
	    		} else if (  action == ActionType.RAISE ) {
	    			valid = (bet - actor.getBet()) + minBet;
	    			int to_call = bet - actor.getBet();
	    			if (amount < valid && actor.getChips() >= valid) {
	    				String out = Strings.InvalidBet.replaceAll("%hID", Integer.toString(handID));
	    				out.replaceAll("%pChips", Integer.toString(actor.getChips()));
	    				out.replaceAll("%min", Integer.toString(valid));
	    				stop = true;
	    			} else if (actor.getChips() <= to_call) {
	    				action = ActionType.CALL;
	    			} else if (amount > (actor.getChips() +  actor.getBet())) {
	    				amount = actor.getChips() + actor.getBet();
	    			}
	    		}
	        	
		        if (!stop) {   	
		            playersToAct--;
		            switch (action) {
		                case CHECK:
		                case CALL:
		                	extra = "";	                	
		                    break;
		                case BET:
		                case RAISE:
		                	minBet = amount;
		                    bet = minBet;
		                    // Other players get one more turn.
		                    playersToAct = activePlayers.size() - 1;
		                	extra = " to " + Integer.toString(minBet);
		                    break;
		                case FOLD:
		                	try {
			                    actor.setCards(null);
		                    } catch (IllegalArgumentException | IllegalStateException e) {
		                    	EventLog.fatal(e, "Table", "onAction");
		                    	System.exit(1);
		                    }
		                    activePlayers.remove(actor);
		                    extra = "";
		                    break;
		                default:
		                	throw new IllegalArgumentException("Blind actions are not valid at this point");
		            }
		            
		            try {
		            	actor.act(action, minBet, bet);
		            } catch (IllegalArgumentException e) {
		            	EventLog.fatal(e, "Table", "onAction");
		            	System.exit(1);	            	
		            }
	                pot += actor.getBetIncrement();
	                
	                int player_total = playerBets.get(actor) + actor.getBetIncrement();
	                playerBets.put(actor, player_total);
		            
		            // If user is all in, announce it and side up a new side pot if needed
		            if (actor.isBroke()) {	                	                
		                String out = Strings.PlayerAllIn.replaceAll("%hID", Integer.toString(handID));
		                out = out.replaceAll("%actor", actor.getName());
		                ircClient.sendIRCMessage( ircChannel, out );
		            }
		            
		            // Re-calulate all the pots
		            calculateSidePots();
		            
		            // TODO: add side pot output
		            String out = Strings.TableAction.replaceAll("%hID", Integer.toString(handID));
		            out = out.replaceAll("%actor", actor.getName());
		            out = out.replaceAll("%action", action.getText());
		            out = out.replaceAll("%amount", extra);
		            out = out.replaceAll("%chips", Integer.toString(actor.getChips()) );
		            out = out.replaceAll("%pot", Integer.toString(pot));
		            ircClient.sendIRCMessage( ircChannel, out );
	
	                if (action == ActionType.FOLD && activePlayers.size() == 1) {
	                    playersToAct = 0;
	                    // The player left wins.
	                    playerWins(activePlayers.get(0));
	                } else {
	    	            // Continue play
	    	            actionReceived(false);
	                }
		        }
	        }
		}
	}
	
	/**
	 * Sends a message to the channel notifying the user that an action was attempted out of turn
	 * 
	 * @param user the user who tried to act
	 */
	private synchronized void invalidAction(String user) {
		String out;
		if (actor == null) out = Strings.InvalidActTime;
		else {
			out = Strings.InvalidActor.replaceAll( "%actor", actor.getName() );
			out = out.replaceAll("%hID", Integer.toString(handID));
		}
		
		out = out.replaceAll( "%user", user );
		ircClient.sendIRCMessage( ircChannel, out );
	}
    
    /**
     * Performs a betting round.
     */
    private synchronized void doBettingRound() {    	
        // Determine the number of active players.
    	playersToAct = activePlayers.size();
    	for (Player p: activePlayers) {
    		if (p.isBroke()) playersToAct--;
    	}
        
        // Determine the initial player and bet size.
        if (board.size() == 0) {
            // Pre-Flop; player left of big blind starts, bet is the big blind.
            bet = bigBlind;
            if (players.size() == 2)
            	actorPosition = (dealerPosition + 1) % activePlayers.size();
            else
            	actorPosition = (dealerPosition + 2) % activePlayers.size();
        } else {
            // Otherwise, player left of dealer starts, no initial bet.
            bet = 0;
            if (players.size() == 2)
            	actorPosition = (dealerPosition + 1) % activePlayers.size();
            else
            	actorPosition = dealerPosition;
        }
        
        actionReceived(true);
    }
	
    /**
     * Returns the allowed actions of a specific player.
     * 
     * @param player The player.
     * 
     * @return The allowed actions.
     */
    private synchronized Set<ActionType> getAllowedActions(Player player) {
        int actorBet = actor.getBet();
        Set<ActionType> actions = new HashSet<ActionType>();
        if (bet == 0) {
            actions.add(ActionType.CHECK);
            if (player.getRaises() < MAX_RAISES && activePlayers.size() > 1) {
                actions.add(ActionType.BET);
            }
        } else {
            if (actorBet < bet) {
                actions.add(ActionType.CALL);
                if (player.getRaises() < MAX_RAISES && activePlayers.size() > 1) {
                    actions.add(ActionType.RAISE);
                }
            } else {
                actions.add(ActionType.CHECK);
                if (player.getRaises() < MAX_RAISES && activePlayers.size() > 1) {
                    actions.add(ActionType.RAISE);
                }
            }
        }
        actions.add(ActionType.FOLD);
        return actions;
    }
    
    /**
     * Performs the Showdown.
     */
    private synchronized void doShowdown() {    	
    	// Add final pot to side pots
     	SidePot lastpot = new SidePot();
    	for (Player left: activePlayers) {
    		if (!left.isBroke()) {
    			if (lastpot.getBet() == 0) {
    				lastpot.setBet( left.getTotalBet() );
    				lastpot.call( left );
    			} else {
    				lastpot.call( left );
    			}
    		}
    	}
    	if (lastpot.getPlayers().size() > 0) {
    		sidePots.add(lastpot);
    	}
    	
    	// Process each pot separately
    	int totalpot = 0;
    	int totalbet = 0;
    	int totalrake = 0;
    	String potname = "";
    	int i = 0;
    	for (SidePot side: sidePots) {
    		if (i == 0) potname = "Main Pot";
    		else potname = "Side Pot " + Integer.toString(i);
    		
    		// More than one player so decide the winner
    		if (side.getPlayers().size() > 1) {
        		// Take the rake
        		int rake = 0;
        		if ( pot > (bigBlind*2) ) {
        			rake += side.rake();
        		}
        		int potsize = side.getPot() - totalpot;
        		
                // Look at each hand value, sorted from highest to lowest.
                Map<HandValue, List<Player>> rankedPlayers = getRankedPlayers( side.getPlayers() );
                for (HandValue handValue : rankedPlayers.keySet()) {
                    // Get players with winning hand value.
                    List<Player> winners = rankedPlayers.get(handValue);
                    if (winners.size() == 1) {
                        // Single winner.
                        Player winner = winners.get(0);
                    	winner.win( potsize );
                    	String out = Strings.PotWinner.replaceAll("%winner", winner.getName());
                    	out = out.replaceAll("%hand", handValue.toString());
                    	out = out.replaceAll("%pot", potname);
                    	out = out.replaceAll("%amount", Integer.toString(potsize));
                    	out = out.replaceAll("%id", Integer.toString(tableID));
                    	out = out.replaceAll("%hID", Integer.toString(handID));
                    	
                    	ircClient.sendIRCMessage( ircChannel, out );
                        break;
                    } else {                    
                        // Tie; share the pot amongst winners.
                        int remainder = potsize % winners.size();
                        if (remainder != 0) {
                        	potsize -= remainder;
                        	rake += remainder;
                        }
                        int potShare = potsize / winners.size();
                        
                        for (Player winner : winners) {                        
                            // Give the player his share of the pot.
                            winner.win(potShare);
                            
                            // Announce
                        	String out = Strings.PotWinner.replaceAll("%winner", winner.getName());
                        	out = out.replaceAll("%hand", handValue.toString());
                        	out = out.replaceAll("%pot", potname);
                        	out = out.replaceAll("%amount", Integer.toString(potShare));
                        	out = out.replaceAll("%hID", Integer.toString(handID));
                        	ircClient.sendIRCMessage( ircChannel, out );
                        }
                        break;
                    }
                }
                // Add this pot's rake to the total rake
        		totalrake += rake;
	    	} else {
	    		// Only one player, pot is returned.
	    		Player returnee = side.getPlayers().get(0);
	    		int returned = side.getPot() - totalbet;
	    		returnee.win( returned );
	    		
            	String out = Strings.PotReturned.replaceAll("%winner", returnee.getName());
            	out = out.replaceAll("%amount", Integer.toString(returned));
            	out = out.replaceAll("%id", Integer.toString(tableID));
            	out = out.replaceAll("%hID", Integer.toString(handID));
            	ircClient.sendIRCMessage( ircChannel, out );
	    	}

    		// Each side pot contains the previous pot's total, so remove it
    		totalbet = side.getBet();
    		totalpot = side.getPot();
    		
    		// Increase pot number
    		i++;
    	}
    	
    	// Announce rake
    	String out = Strings.RakeTaken.replaceAll("%rake", Integer.toString(totalrake));
    	out = out.replaceAll("%id", Integer.toString(tableID));
    	out = out.replaceAll("%hID", Integer.toString(handID));
        ircClient.sendIRCMessage( ircChannel, out );
        nextHand();
    }
	
    /**
     * Returns the active players mapped and sorted by their hand value.
     * 
     * @param player_list The list of players to compare
     * 
     * @return The active players mapped by their hand value (sorted). 
     */
    private synchronized Map<HandValue, List<Player>> getRankedPlayers(List<Player> player_list) {
		Map<HandValue, List<Player>> winners = new TreeMap<HandValue, List<Player>>();
		for (Player player : player_list) {
	            // Create a hand with the community cards and the player's hole cards.
	            Hand hand = null;
	            try {
	            	hand = new Hand(board);
	            } catch (IllegalArgumentException e) {
	            	EventLog.fatal(e, "Table", "getRankedPlayers");
	            	System.exit(1);
	            }
	            
		    	try {
		    		 hand.addCards(player.getCards());
	            } catch (IllegalArgumentException e) {
	            	EventLog.fatal(e, "Table", "getRankedPlayers");
	            	System.exit(1);
	            }
	            
	            // Store the player together with other players with the same hand value.
	            HandValue handValue = new HandValue(hand);
	            List<Player> playerList = winners.get(handValue);	            
	            if (playerList == null) {
	            	playerList = new LinkedList<Player>();
	            }
	            playerList.add(player);
	            winners.put(handValue, playerList);
		}
		return winners;
    }
	
    /**
     * Let's a player win the pot when everyone else folded.
     * 
     * @param player The winning player.
     */
    private synchronized void playerWins(Player player) {
    	// Rake
		// None if only the blinds are in
		// Otherwise Minimum or RakePercent whichever is greater 
		int rake = 0;
		if ( pot > (bigBlind*2) ) {
			rake = Variables.MinimumRake;
			int perc = (int)Math.round(pot * (Variables.RakePercentage / 100.0));
			if (perc < Variables.MinimumRake)
				rake = Variables.MinimumRake;
			else if (perc > Variables.MaximumRake)
				rake = Variables.MaximumRake;
			else
				rake = perc;
		}
    	pot = pot - rake;
    	
        player.win(pot);
        
        // Announce
        String out = Strings.PlayerWins.replaceAll("%hID", Integer.toString(handID));
        out = out.replaceAll("%who", player.getName());
        out = out.replaceAll("%id", Integer.toString(tableID));
        out = out.replaceAll("%amount", Integer.toString(pot));
        out = out.replaceAll("%total", Integer.toString(player.getChips()));   
        out = out.replaceAll("%rake", Integer.toString(rake));     
        ircClient.sendIRCMessage(ircChannel, out );
        
        pot = 0;
        
        // start next hand
        nextHand();
    }
    	
    /**
     * Schedules the wait for players timer
     */
    private synchronized void scheduleWaitForPlayers() {
		waitForPlayersTimer = new Timer();
		waitForPlayersTimer.schedule(new TableTask( this, TableTask.WaitForPlayersTaskName),
				Strings.WaitingForPlayersSecs*1000);
    }
    
    /**
     * Rotates the position of the player in turn (the actor).
     */
    private synchronized void rotateActor() {
        if (activePlayers.size() > 0) {
            do {
                actorPosition = (actorPosition + 1) % players.size();
                actor = players.get(actorPosition);
            } while (!activePlayers.contains(actor));
        } else {
            // Should never happen.
            throw new IllegalStateException("No active players left");
        }
    }
    
    /**
     * This method handles when a user didn't respond and is automatically folded.
     */
    private synchronized void noActionReceived() {
    	// Announce    	
    	String out = Strings.NoAction.replaceAll("%hID", Integer.toString(handID) );
    	out = out.replaceAll("%actor", actor.getName() );
    	ircClient.sendIRCMessage( ircChannel, out );
    	
    	// sit the player out
    	playerSitsOut(actor);
    }
    
    /**
     * Posts the small blind.
     */
    private synchronized void postSmallBlind() {
        final int smallBlind = bigBlind / 2;
        actor.postSmallBlind(smallBlind);
        pot += smallBlind;
        playerBets.put(actor, smallBlind);

        String out = Strings.SmallBlindPosted.replaceAll("%sb", Integer.toString(smallBlind) );
        out = out.replaceAll("%player", actor.getName() );
        ircClient.sendIRCMessage( ircChannel, out );
        
		try {
			rotateActor();
		} catch (IllegalStateException e) {
			EventLog.fatal(e,"Table", "postSmallBlind");
			System.exit(1);
		}
    }
    
    /**
     * Posts the big blind.
     */
    private synchronized void postBigBlind() {
        actor.postBigBlind(bigBlind);
        pot += bigBlind;
        playerBets.put(actor, bigBlind);

        String out = Strings.BigBlindPosted.replaceAll("%bb", Integer.toString(bigBlind) );
        out = out.replaceAll("%player", actor.getName() );
        ircClient.sendIRCMessage( ircChannel, out );
        
		try {
			rotateActor();
		} catch (IllegalStateException e) {
			EventLog.fatal(e,"Table", "postBigBlind");
			System.exit(1);
		}
    }
	
    /**
     * Deals the Hole Cards.
     */
    private synchronized void dealHoleCards() {
        for (int i = 0; i < activePlayers.size(); i++) {
        	// Order from left of dealer
        	int position = (dealerPosition + i + 1) % activePlayers.size();
        	Player player = activePlayers.get(position);
        	List<Card> holeCards = new ArrayList<Card>();
        	try {
        		holeCards = deck.deal(2);
                player.setCards( holeCards );
            } catch (IllegalArgumentException | IllegalStateException e) {
            	EventLog.fatal(e, "Table", "dealHoleCards");
            	System.exit(1);
            }
            String out = Strings.HoleCardsDealtPlayer.replaceAll("%hID", Integer.toString(handID));
            out = out.replaceAll( "%id", Integer.toString(tableID) );;
            out = out.replaceAll( "%card1", holeCards.get(0).toIRCString() );;
            out = out.replaceAll( "%card2", holeCards.get(1).toIRCString() );
            ircClient.sendIRCMessage( player.getName(), out );
        }
        ircClient.sendIRCMessage( ircChannel, Strings.HoleCardsDealt.replaceAll("%hID", Integer.toString(handID)) );
    }
    
    /**
     * Deals a number of community cards.
     * 
     * @param phaseName The name of the phase.
     * @param noOfCards The number of cards to deal.
     */
    private synchronized void dealCommunityCards(String phaseName, int noOfCards) {
    	String cardstr = "";
        for (int i = 0; i < noOfCards; i++) {
        	Card card = deck.deal();
        	try {
        		card = deck.deal();;
            } catch (IllegalArgumentException | IllegalStateException e) {
            	EventLog.fatal(e, "Table", "dealCommunityCards");
            	System.exit(1);
            }
            board.add( card );
            cardstr = cardstr + card.toIRCString() + "%n ";
        }
        
        // Notify channel of the card(s) dealt
   	 	String out = Strings.CommunityDealt.replaceAll("%hID", Integer.toString(handID));
   		out = out.replaceAll("%round", phaseName );
   		out = out.replaceAll("%cards", cardstr );
   		ircClient.sendIRCMessage( ircChannel, out );
   	 
   		// Notify each player of all their cards
        for (Player player : activePlayers) {
        	Card[] cards = player.getCards();
        	cardstr = "";
        	for (int i = 0; i < cards.length; i++) {
        		if (cards[i] != null) {
                    cardstr = cardstr + cards[i].toIRCString() + "%n ";        			
        		}
        	}
        	for (int i = 0; i < board.size(); i++) {
               cardstr = cardstr + board.get(i).toIRCString() + "%n ";
        	}
        	
       	 	out = Strings.CommunityDealtPlayer.replaceAll("%hID", Integer.toString(handID));
       	    out = out.replaceAll("%id", Integer.toString(tableID) );
       	    out = out.replaceAll("%round", phaseName );
       	    out = out.replaceAll("%cards", cardstr );
       	    ircClient.sendIRCMessage( player.getName(), out );
        }
        
    }
    
    /**
     * Prepares the table for a new hand
     */
    private synchronized void nextHand() {
        board.clear();
        bet = 0;
        pot = 0;
        currentRound = 0;
        handActive = false;
        List<Player> remove_list = new ArrayList<Player>();
        playerBets = new HashMap<Player,Integer>();
        
		sidePots.clear();
        
        activePlayers.clear();
        for (Player player : players) {
            player.resetHand();
            // Remove players without chips
           	if (player.isBroke()) {
           		remove_list.add(player);
           	}
           	playerBets.put( player, 0 );
           	
           	// Only add non-sat out players to activePlayers
            if (!remove_list.contains(player)) {
                activePlayers.add(player);
            }
    		Database.getInstance().addPokerTableCount(player.getName(), tableID, profileID, player.getChips());
        }
        
        // Sit all broke players out.
        for (Player player: remove_list) {
        	String out = Strings.OutOfChips.replaceAll("%player", player.getName());
        	out = out.replaceAll("%id", Integer.toString(tableID));
       		ircClient.sendIRCMessage(ircChannel, out);
        	playerSitsOut(player);
        }
        
        if (activePlayers.size() >= minPlayers) {
        	handActive = true;
        	deck.shuffle();        
    		
        	dealerPosition = (dealerPosition + 1) % activePlayers.size();
        	if (players.size() == 2) {
	        	actorPosition = dealerPosition;
        	} else {
        		actorPosition = (dealerPosition + 1) % activePlayers.size();
        	}
        	actor = activePlayers.get(actorPosition);
        	int bbPosition = (actorPosition + 1) % players.size();
        
	        minBet = bigBlind;
	        bet = minBet;
	        
	        handID = Database.getInstance().getHandID();
	        
	        String out = Strings.NewHandMessage.replaceAll( "%hID", Integer.toString(handID) );
	        out = out.replaceAll("%dealer", players.get(dealerPosition).getName() );
	        out = out.replaceAll("%sb", players.get( actorPosition ).getName() );
	        out = out.replaceAll("%bb", players.get( bbPosition ).getName() );
	        
	        ircClient.sendIRCMessage( ircChannel, out );
	        
	        // Small blind.
	        postSmallBlind();
	        
	        // Big blind.
	        postBigBlind();
	        
	        // Pre-Flop.
	        dealHoleCards();
	        doBettingRound();
        } else {
        	actor = null;
        	handActive = false;
        	// not enough players
			scheduleWaitForPlayers();
        }
    }
    
    /**
     * Shuts down this table when we have no active or sat out players
     */
	private synchronized void closeTable() {
    	tables.remove(tableID);
		ircClient.closeTable( this );
		this.interrupt();
	}
    
    /**
     * Set's the table channel's topic
     */
    private void setTopic() {
		super.setTopic( formatTableInfo(Strings.TableTopic) );
    }

    /**
     * Voices/Devoices players/non-players when the bot joins the channel.
     */
	public void joinedChannel(User[] users) {
		EventLog.info("Bot joined a new table, giving and taking user modes on the channel", "Table", "joinedChannel");
		List<Player> playerlist = new ArrayList<Player>(players);
		playerlist.addAll(satOutPlayers);
		
		for (int i = 0; i < users.length; i++) {
			String nick = users[i].getNick();
			for (Player player: playerlist) {
				if (player.getName().compareToIgnoreCase(nick) == 0) {
					ircClient.voice(ircChannel, nick);
					if ( satOutPlayers.contains(player ) ) { playerSitsDown( player ); }
					break;
				}
			}
			// user didn't exist, devoice
			if ( nick.compareToIgnoreCase(ircClient.getNick()) != 0 ) {
				ircClient.deOp(ircChannel, nick);
				ircClient.deVoice(ircChannel, nick);
			}
		}
				
		if (disconnected) {
			scheduleWaitForPlayers();
		}
	}

	/**
	 * Used to cancel the currently running hand
	 * Generally called after a disconnect
	 */
	public synchronized void cancelHand() {
		for (Player player: players)	{
			player.cancelBet();
		}
		
		if (waitForPlayersTimer != null) waitForPlayersTimer.cancel();
		if (actionTimer != null) actionTimer.cancel();
		if (startGameTimer != null) startGameTimer.cancel();
		
		disconnected = true;
	}
	
	/**
	 * Calculates the players in each side pot and the amount of each
	 */
	private synchronized void calculateSidePots() {
		// Reset the sidepots
		sidePots.clear();
		
		// For each unique "all-in" bet amount, create a new sidepot
		List<Integer> sidepot_amounts = new ArrayList<Integer>();
		for (Player player: activePlayers) {
			if (player.isBroke()) {
				// Player is all in, do we have a side pot for this size?
				// If not, create one
				int totalbet = player.getTotalBet();
				if ( !sidepot_amounts.contains( totalbet ) ) {
					sidePots.add( new SidePot( totalbet ) );
					sidepot_amounts.add( totalbet );
				}
			}
		}
		
		// For each player that has any chips in the pot,
		// add them or their chips to the sidePots
		for (SidePot sidepot: sidePots) {
			for (Entry<Player, Integer> entry: playerBets.entrySet()) {
				Integer pbet = entry.getValue();
				if ( pbet >= sidepot.getBet() ) {
					sidepot.call( entry.getKey() );
				} else {
					sidepot.add( pbet );
				}
			}
		}
		
        // Order the pots (ascending)
        Collections.sort( sidePots );
	}
	
	/**
	 * Finds a player object with a specific name
	 * 
	 * @param name the player name
	 * @return the player
	 */
	private Player findPlayer(String name) {
		Player found = null;
		// find the player
		for (Player plyr: players) {
			if (plyr.getName().compareToIgnoreCase( name ) == 0) {
				found = plyr;
				break;
			}
		}
		
		// check if they are sat out
		if (found == null) {
			for (Player plyr: satOutPlayers) {
				if (plyr.getName().compareToIgnoreCase( name ) == 0) {
					found = plyr;
					break;
				}
			}
		}
		
		return found;
	}
}
