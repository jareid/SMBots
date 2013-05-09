/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.poker.game.rooms;

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

import org.pircbotx.Channel;
import org.pircbotx.User;

import org.smokinmils.Database;
import org.smokinmils.poker.Client;
import org.smokinmils.poker.Utils;
import org.smokinmils.poker.enums.ActionType;
import org.smokinmils.poker.enums.CommandType;
import org.smokinmils.poker.enums.RoomType;
import org.smokinmils.poker.game.Card;
import org.smokinmils.poker.game.Deck;
import org.smokinmils.poker.game.Hand;
import org.smokinmils.poker.game.HandValue;
import org.smokinmils.poker.game.SidePot;
import org.smokinmils.poker.settings.Strings;
import org.smokinmils.poker.settings.Variables;
import org.smokinmils.poker.tasks.TableTask;
import org.smokinmils.bot.Random;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;


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
	private ProfileType Profile;
    
	/** The size of the big blind. */
	private int bigBlind;
	
	/** The size of the small blind. */
	private int smallBlind;
	
	/** The players at the table. */
	private List<Player> players;
	
    /** The active players in the current hand. */
    private final List<Player> activePlayers;

    /** The active players in the current hand. */
    private final List<Player> jackpotPlayers;
    
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
	
	/** Timer to handle a request to show cards */
	private Timer showCardsTimer;
	private boolean canShow;
	
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
	
	/** Hand card strings */
	private Map<String, String> phaseStrings;
	
	/** Rounds */
	private static final int PREFLOP = 0;
	private static final int FLOP 	 = 1;
	private static final String FLOPSTR = "Flop";
	private static final int TURN	 = 2;
	private static final String TURNSTR = "Turn";
	private static final int RIVER	 = 3;
	private static final String RIVERSTR = "River";
	
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
     * @param profile  The profile this table is for
     * @param manual   Whether the table was created auto or manually
     */
	public Table(Channel channel, Client irc, int id, int bb, int max,
				ProfileType profile, boolean manual) {
    	super(channel, irc, RoomType.TABLE);	
    	boolean failed = false;
		players = new ArrayList<Player>();
		activePlayers = new ArrayList<Player>();
		satOutPlayers = new ArrayList<Player>();		
		jackpotPlayers = new ArrayList<Player>();
        board = new ArrayList<Card>();
        phaseStrings = new HashMap<String,String>();

		sidePots = new ArrayList<SidePot>();
        
        try {
	        deck = null;
	        deck = new Deck();
		} catch (Exception e) {
			failed = true;
			EventLog.fatal(e, "Table", "Constructor");
		}
        
		startGameTimer = null;
		showCardsTimer = null;
		canShow = false;
		actionTimer = null;
		waitForPlayersTimer = null;
		waitedCount = 0;
		handActive = false;
		disconnected = false;
		
		createdManually = manual;
		
		CurrentID++;
		tableID = id;
		
		Profile = profile;
		
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
			IrcClient.sendIRCMessage("Something caused the bot to crash... please notify the staff.");
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
	 * Returns the profile name
	 * 
	 * @return The profile name
	 */
	public ProfileType getProfile() { return Profile; }
	
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
		in = in.replaceAll( "%profile", Profile.toString() );
    	return in;
    }
   
    
    /**
     * Adds a player to the table
     * 
     * @param player  The player.
     */
	public synchronized void playerJoins(User sender, Integer buy_in) {
		Player player = new Player(sender, sender.getNick(), buy_in);
		boolean found = false;
		Set<User> users = IrcClient.getBot().getUsers(IrcChannel);
		for (User user: users) {
			if (user.compareTo(sender) == 0) {
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
		try {
			Database.getInstance().buyIn(sender.getNick(), buy_in, Profile);
			Database.getInstance().addPokerTableCount(sender.getNick(), tableID, Profile, buy_in);
		} catch (Exception e) {
			EventLog.log(e, "Table", "playerJoins");
		}
		
		// Invite the player to join channel
		IrcClient.getBot().sendInvite( sender, IrcChannel.getName() );
		
		// Try to voice the user incase they were watching
		IrcClient.getBot().voice( IrcChannel, sender );
		observers.remove( sender );
		
		// Announce
		String out = Strings.PlayerJoins.replaceAll("%id", Integer.toString(tableID) );
		out = out.replaceAll( "%player", player.getName() );
		out = out.replaceAll( "%chips",Integer.toString(player.getChips()) );		
		IrcClient.getBot().sendIRCMessage( IrcChannel, out);
		setTopic();
	}
	
    /**
     * A player has left the table
     * 
     * @param player the player who left
     * @param sat_out true if the player was sat out prior to leaving
     */
    public synchronized void playerLeaves(Player player, boolean sat_out) {
    	User user = player.getUser();
    	String name = player.getName();
    	int chips = player.getChips() + player.getRebuy();
    	
    	// Ensure user has left the table
    	if (createdManually) {
    		IrcClient.getBot().kick( IrcChannel, user );
    	} else {
    		IrcClient.getBot().deVoice( IrcChannel, user );
    	}
    	
    	// Cash out
    	if (!player.isBroke()) {
    		player.cashOut();
    		try {
    			Database.getInstance().cashOut( name, chips, Profile );
    			Database.getInstance().addPokerTableCount(name, tableID, Profile, 0);
    		} catch (Exception e) {
    			EventLog.log(e, "Table", "playerLeaves");
    		}
    	}
    	
    	// Remove from our player list
		players.remove(player);
	    satOutPlayers.remove(player);
	    
		// handle current hands
		if (player == actor) {
	    	onAction(ActionType.FOLD, "", true);
		} else if (activePlayers.contains(player)) {
			playersToAct--;
	    	activePlayers.remove(player);
			if (activePlayers.size() == 1) {
				playerWins(activePlayers.get(0));
			}
		}
	    
	    // Cancel timer
		if (player.getSittingOutTimer() != null) player.getSittingOutTimer().cancel();
		
		// Announce
		String out = Strings.PlayerLeaves.replaceAll("%id", Integer.toString(tableID) );
		out = out.replaceAll( "%player", name );
		out = out.replaceAll( "%chips",Integer.toString(chips) );		
		IrcClient.getBot().sendIRCMessage( IrcChannel, out);
		IrcClient.getBot().sendIRCNotice( player.getName(),
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
		IrcClient.getBot().sendIRCMessage( IrcChannel, out);
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
		player.setSatOut(true);
    	
		// handle current hands
		if (player == actor) {
	    	onAction(ActionType.FOLD, "", true);
		} else if (activePlayers.contains(player) && !player.isBroke()) {
			playersToAct--;
	    	activePlayers.remove(player);
			if (activePlayers.size() == 1) {
				actor = null;
				playerWins(activePlayers.get(0));
			}
		}
    	
    	// Announce
		String out = Strings.PlayerSitsOut.replaceAll("%id", Integer.toString(tableID));
		out = out.replaceAll("%player", player.getName());
		IrcClient.getBot().sendIRCMessage( IrcChannel, out);
    }
    
    /**
     * This method is called whenever a message is sent to this channel.
     *
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
	protected synchronized void onMessage(User sender, String login, String hostname, String message) {
		int fSpace = message.indexOf(" ");
		if(fSpace == -1) fSpace = message.length();
		String firstWord = message.substring(0, fSpace).toLowerCase();
		
		if ((fSpace + 1) <= message.length()) fSpace++;
		String restofmsg = message.substring( fSpace, message.length() );
		String[] msg = restofmsg.split(" ");
		
		CommandType cmd = CommandType.fromString( firstWord );
		if (cmd != null) {
			boolean isActor = (actor != null ? (actor.getUser().compareTo( sender ) == 0) : false);
			switch (cmd) {
			case CHECK:
				if ((msg.length == 0 || msg[0].compareTo("") == 0) && isActor) {
					if (bet == 0 || actor.getBet() >= bet ) onAction( ActionType.CHECK, restofmsg, false );
					else onAction( ActionType.CALL, restofmsg, false);
				} else if (!isActor) {
					invalidAction( sender );
				} else {
					invalidArguments( sender.getNick(), cmd.getFormat() );
				}
				break;
			case RAISE:
				if (msg.length == 1 && msg[0].compareTo("") != 0 && isActor) {
					if (bet == 0) onAction( ActionType.BET, restofmsg, false );
					else onAction( ActionType.RAISE, restofmsg, false );	
				} else if (!isActor) {
					invalidAction( sender );
				} else {
					invalidArguments( sender.getNick(), cmd.getFormat() );
				}
				break;
			case FOLD:
				if ((msg.length == 0 || msg[0].compareTo("") == 0) && isActor) {
					onAction( ActionType.FOLD, restofmsg, false );			
				} else if (!isActor) {
					invalidAction( sender );
				} else {
					invalidArguments( sender.getNick(), cmd.getFormat() );
				}
				break;
			case SHOW:
				onShow(sender, login, hostname, restofmsg);
				break;
			case TBLCHIPS:
				onChips(sender, login, hostname, restofmsg);
				break;
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
	protected synchronized void onJoin(User sender, String login, String hostname) {
    	if ( sender.getNick().compareToIgnoreCase( IrcClient.getBot().getNick() ) != 0) {
    		Player found = null;
    		for (Player plyr: satOutPlayers) {
    			if (plyr.getUser().compareTo( sender ) == 0) {
    				found = plyr;
    				break;
    			}
    		}
    		
    		if ( found != null) {
    			IrcClient.getBot().voice( IrcChannel, sender );
    			playerSitsDown( found );
    		} else {
    			observers.add( sender.getNick().toLowerCase() );
    		}
    	} else {
    		IrcClient.getBot().setMode( IrcChannel, "+m");
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
    	if ( sender.compareToIgnoreCase( IrcClient.getBot().getNick() ) != 0) {
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
    	if ( sourceNick.compareToIgnoreCase( IrcClient.getBot().getNick() ) == 0) {
    		setTopic();
    		IrcClient.getBot().setMode( IrcChannel, "+m");
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
		   if (!actionReceived && actor != null) {
			   String out = Strings.NoActionWarning.replaceAll("%hID", Integer.toString(handID) );
		    	out = out.replaceAll("%actor", actor.getName() );
		    	out = out.replaceAll("%secs", Integer.toString(Variables.ActionWarningTimeSecs) );
		    	IrcClient.getBot().sendIRCMessage( IrcChannel, out );
			   
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
	   case TableTask.ShowCardTaskName:
		   if (showCardsTimer != null) showCardsTimer.cancel();
		   canShow = false;
		   nextHand();
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
				if (createdManually && waitedCount > Variables.MaxWaitCount) closeTable();
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
				IrcClient.getBot().sendIRCMessage(IrcChannel, out);
				
				scheduleWaitForPlayers();
			} else {
				String out = Strings.GameStartMsg.replaceAll("%bb", Integer.toString(getBigBlind()) );
				out = out.replaceAll("%sb", Integer.toString(getSmallBlind()) );
				out = out.replaceAll("%secs", Integer.toString(Variables.GameStartSecs) );
				out = out.replaceAll("%seatedP", Integer.toString(getPlayersSatDown()) );
				IrcClient.getBot().sendIRCMessage(IrcChannel, out);

				waitedCount = 0;
				
				// Schedule the game to start
				startGameTimer = new Timer(true);
				startGameTimer.schedule(new TableTask( this, TableTask.StartGameTaskName ),
						Variables.GameStartSecs*1000);
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
	private synchronized void onChips(User sender, String login, String hostname, String message) {
		String sender_nick = sender.getNick();
		String[] msg = message.split(" ");
		if ((msg.length == 0 || msg[0].compareTo("") == 0)) {		
			Player found = findPlayer( sender_nick );
				
			if (found != null) {
				String out = Strings.CheckChips.replaceAll( "%id", Integer.toString(tableID) );
				out = out.replaceAll( "%creds", Integer.toString(found.getChips()) );		
				IrcClient.getBot().sendIRCNotice( sender_nick, out );
			} else {
				String out = Strings.CheckChipsFailed.replaceAll( "%id", Integer.toString(tableID) );	
				IrcClient.getBot().sendIRCNotice( sender_nick, out );
			}
		} else if ((msg.length == 1 && msg[0].compareTo("") != 0)) {		
			Player found = findPlayer( msg[0] );
				
			if (found != null) {
				String out = Strings.CheckChipsUser.replaceAll( "%id", Integer.toString(tableID) );
				out = out.replaceAll( "%user", msg[0] );
				out = out.replaceAll( "%creds", Integer.toString(found.getChips()) );		
				IrcClient.getBot().sendIRCNotice( sender_nick, out );
			} else {
				String out = Strings.CheckChipsUserFailed.replaceAll( "%id", Integer.toString(tableID) );
				out = out.replaceAll( "%user", msg[0] );	
				IrcClient.getBot().sendIRCNotice( sender_nick, out );
			}
		} else {
			invalidArguments( sender_nick, CommandType.TBLCHIPS.getFormat() );
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
	private synchronized void onShow(User sender, String login, String hostname, String message) {
		String sender_nick = sender.getNick();
		Player found = findPlayer( sender_nick );
		
		if (found == null) {
			String out = Strings.ShowCardFailNoPlayer.replaceAll( "%id", Integer.toString(tableID) );
			out = out.replaceAll( "%hID", Integer.toString(handID) );
			IrcClient.getBot().sendIRCNotice( sender_nick, out );
		} else if (found.getCards().length == 0) {
			String out = Strings.ShowCardFailNotActive.replaceAll( "%id", Integer.toString(tableID) );
			out = out.replaceAll( "%hID", Integer.toString(handID) );
			IrcClient.getBot().sendIRCNotice( sender_nick, out );			
		} else if (!canShow) {
			String out = Strings.ShowCardFailInHand.replaceAll( "%id", Integer.toString(tableID) );
			out = out.replaceAll( "%hID", Integer.toString(handID) );
			IrcClient.getBot().sendIRCNotice( sender_nick, out );					
		} else {
        	Card[] cards = found.getCards();
        	String cardstr = "%n[";
        	for (int i = 0; i < cards.length; i++) {
        		if (cards[i] != null) {
                    cardstr = cardstr + cards[i].toIRCString() + "%n ";        			
        		}
        	}
			cardstr += "] ";
			
			String out = Strings.ShowCards.replaceAll( "%id", Integer.toString(tableID) );
			out = out.replaceAll( "%hID", Integer.toString(handID) );
			out = out.replaceAll( "%who", found.getName() );
			out = out.replaceAll( "%cards", cardstr );
			IrcClient.getBot().sendIRCMessage( IrcChannel, out );			
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
	private synchronized void onRebuy(User sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		Integer buy_in = Utils.tryParse( msg[0] );
		if ((msg.length == 1 && msg[0].compareTo("") != 0) || buy_in != null) {
			int maxbuy = (bigBlind*Variables.MaxBuyIn);
			int minbuy = (bigBlind*Variables.MinBuyIn);
			
			if (buy_in != null && !IrcClient.userHasCredits( sender.getNick(), buy_in, Profile ) ) {
				String out = Strings.NoChipsMsg.replaceAll( "%chips", Integer.toString(buy_in));
				out = out.replaceAll( "%profile", Profile.toString() );
				IrcClient.getBot().sendIRCNotice(sender.getNick(), out);
			} else if (buy_in != null) {
				Player found = findPlayer(sender.getNick());
				
				if (found != null) {
					int total = buy_in + found.getRebuy() + found.getChips();
					int diff = maxbuy - total;
					
					if ( total > maxbuy ) {
						String out = Strings.RebuyFailure.replaceAll( "%id", Integer.toString(tableID) );
						out = out.replaceAll( "%maxbuy", Integer.toString(maxbuy) );
						out = out.replaceAll( "%total", Integer.toString(found.getChips() + found.getRebuy()) );		
						IrcClient.getBot().sendIRCNotice( sender.getNick(), out );
					} else if (diff < 0) {
						String out = Strings.IncorrectBuyInMsg.replaceAll("%buyin", Integer.toString(buy_in) );
						out = out.replaceAll( "%maxbuy", Integer.toString(maxbuy) );
						out = out.replaceAll( "%minbuy", Integer.toString(minbuy) );
						out = out.replaceAll( "%maxBB", Integer.toString(Variables.MaxBuyIn) );
						out = out.replaceAll( "%minBB", Integer.toString(Variables.MinBuyIn) );
						IrcClient.getBot().sendIRCNotice(sender.getNick(), out);
					} else {
						// Remove chips from db
						try  {
							Database.getInstance().buyIn(sender.getNick(), buy_in, Profile);
				    		Database.getInstance().addPokerTableCount(found.getName(), tableID, Profile, buy_in);
				        } catch (Exception e) {
				        	EventLog.log(e, "Table", "nextHand");
				        }
						
						found.rebuy(buy_in);
						
						String out = Strings.RebuySuccess.replaceAll( "%id", Integer.toString(tableID) );
						out = out.replaceAll( "%user", found.getName() );
						out = out.replaceAll( "%new", msg[0] );
						out = out.replaceAll( "%total", Integer.toString(found.getChips() + found.getRebuy()) );		
						IrcClient.getBot().sendIRCMessage( IrcChannel, out );
						
						if (satOutPlayers.contains(found)) playerSitsDown(found);
					}
				} else {
					EventLog.log("RECOVERABLE: User tried to rebuy but is not on the table", "Table", "onRebuy");
					IrcClient.getBot().deVoice(IrcChannel, sender);
					observers.add(sender.getNick().toLowerCase());
				}
			}	
		} else {
			invalidArguments( sender.getNick(), CommandType.REBUY.getFormat() );
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
	private synchronized void onLeave(User sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			Player found = findPlayer( sender.getNick() );
			boolean is_active = (found != null ? players.contains(found) : false);

			if (found != null) { 
				playerLeaves(found, is_active);
			} else {
				EventLog.log(sender + "failed to leave as they should not have been voiced.", "Table", "onLeave");
				IrcClient.getBot().deVoice(IrcChannel, sender);
			}
		} else {
			invalidArguments( sender.getNick(), CommandType.LEAVE.getFormat() );
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
	private synchronized void onSitDown(User sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			Player found = null;
			for (Player plyr: satOutPlayers) {
				if (plyr.getUser().compareTo( sender ) == 0) {
					found = plyr;
					break;
				}
			}
			
			if (found != null && (found.isBroke() && found.getRebuy() == 0)) {
				IrcClient.getBot().sendIRCNotice( sender.getNick(), Strings.SitOutFailed.replaceAll("%id", Integer.toString(tableID)) );
			} else if (found != null) {
				playerSitsDown(found);
			}
		} else {
			invalidArguments( sender.getNick(), CommandType.SITDOWN.getFormat() );
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
	private synchronized void onSitOut(User sender, String login, String hostname, String message) {
		String[] msg = message.split(" ");
		if (msg.length == 0 || msg[0].compareTo("") == 0) {
			Player found = null;
			for (Player plyr: players) {
				if (plyr.getUser().compareTo( sender ) == 0) {
					found = plyr;
					break;
				}
			}
			
			if (found != null) {
				playerSitsOut(found);
			} else {
				IrcClient.getBot().sendIRCNotice( sender.getNick(),
						Strings.SitOutFailed.replaceAll("%id", Integer.toString(tableID)) );
			}
		} else {
			invalidArguments( sender.getNick(), CommandType.SITOUT.getFormat() );
		}
	}
	
	/**
	 * Announces the next actor and valid actions and waits
	 */
	private synchronized void getAction() {
		Set<ActionType> allowed = getAllowedActions(actor);
		String actions = allowed.toString();
		if ( allowed.contains(ActionType.CALL) )
			actions += " {%c04" + (bet - actor.getBet()) + "%c12 to call}";
		if ( allowed.contains(ActionType.BET) )
			actions += " {%c04" + minBet + "%c12 to bet}";
		if ( allowed.contains(ActionType.RAISE) )
			actions += " {%c04" + ((bet - actor.getBet()) + minBet) + "%c12 to raise}";
		
		String out = Strings.GetAction.replaceAll( "%hID", Integer.toString(handID) );
		out = out.replaceAll( "%actor", actor.getName() );
		out = out.replaceAll( "%valid", actions );
		out = out.replaceAll( "%hID", Integer.toString(handID) );		
		IrcClient.getBot().sendIRCMessage( IrcChannel, out );
		
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
	    		playersToAct--;
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
                    dealCommunityCards(FLOPSTR, 3);
                    doBettingRound();
                }
    			break;
    		case FLOP:
    			currentRound++;
                if (activePlayers.size() > 1) {
                    bet = 0;
                    dealCommunityCards(TURNSTR, 1);
                    minBet = bigBlind;
                    doBettingRound();
                }
    			break;
    		case TURN:
    			currentRound++;
                if (activePlayers.size() > 1) {
                    bet = 0;
                    dealCommunityCards(RIVERSTR, 1);
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
		if (actor == null)
			EventLog.debug("actor is null and we received an action: " +action.toString(), "Table", "onAction");
		if (actor != null) {
			Set<ActionType> allowedActions = getAllowedActions(actor);        
	        if (!allowedActions.contains(action)) {
	            String out = Strings.InvalidAction.replaceAll("%hID", Integer.toString(handID));
	            out = out.replaceAll("%invalid", action.getName() );
	            out = out.replaceAll("%valid", allowedActions.toString() );
	            IrcClient.getBot().sendIRCMessage( IrcChannel, out );
	            IrcClient.getBot().sendIRCNotice( actor.getName(), out );
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
	    			if (amount < valid) {
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
		                IrcClient.getBot().sendIRCMessage( IrcChannel, out );
		            }
		            
		            // Re-calulate all the pots
		            calculateSidePots();

		            String out = Strings.TableAction.replaceAll("%hID", Integer.toString(handID));
		            out = out.replaceAll("%actor", actor.getName());
		            out = out.replaceAll("%action", action.getText());
		            out = out.replaceAll("%amount", extra);
		            out = out.replaceAll("%chips", Integer.toString(actor.getChips()) );
		            out = out.replaceAll("%pot", Integer.toString(pot));
		            IrcClient.getBot().sendIRCMessage( IrcChannel, out );
	
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
	 * @param sender.getNick() the user who tried to act
	 */
	private synchronized void invalidAction(User sender) {
		String out;
		if (actor == null) out = Strings.InvalidActTime;
		else {
			out = Strings.InvalidActor.replaceAll( "%actor", actor.getName() );
			out = out.replaceAll("%hID", Integer.toString(handID));
		}
		
		out = out.replaceAll( "%user", sender.getNick() );
		IrcClient.getBot().sendIRCMessage( IrcChannel, out );
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
    	
    	EventLog.debug(Integer.toString(handID) + ": next betting round", "Table", "doBettingRound");
    	EventLog.debug(Integer.toString(handID) + ": playersToAct = " + Integer.toString(playersToAct),
    			 "Table", "doBettingRound");
    	EventLog.debug(Integer.toString(handID) + ": activePlayers = " + activePlayers.toString(),
   			 "Table", "doBettingRound");
    	
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
            actorPosition = dealerPosition;
        }

    	EventLog.debug(Integer.toString(handID) + ": dealerPosition = " + Integer.toString(dealerPosition),
   			 "Table", "doBettingRound");
    	EventLog.debug(Integer.toString(handID) + ": actorPosition = " + Integer.toString(actorPosition),
      			 "Table", "doBettingRound");
        
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
    		
     		if (side.getPot() < 0) {
    			EventLog.log("Hand " + Integer.toString(handID) + " " + potname + " is less than 0, something went wrong!", "Table", "doShowdown");
    			continue;
    		} else if (side.getPot() == 0) {
    			EventLog.log("Hand " + Integer.toString(handID) + " " + potname + " is 0, ignoring!", "Table", "doShowdown");
    			continue;
    		}
    		
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
                        // Add to DB
                    	try {
	                    	if (i == 0) {
	                            Database.getInstance().setHandWinner(handID, winner.getName(), potsize);
	                    	} else {
	                            Database.getInstance().addHandWinner(handID, winner.getName(), potsize);                    		
	                    	}
	        	        } catch (Exception e) {
	        	        	EventLog.log(e, "Table", "nextHand");
	        	        }
                    	String out = Strings.PotWinner.replaceAll("%winner", winner.getName());
                    	out = out.replaceAll("%hand", handValue.toString());
                    	out = out.replaceAll("%pot", potname);
                    	out = out.replaceAll("%amount", Integer.toString(potsize));
                    	out = out.replaceAll("%id", Integer.toString(tableID));
                    	out = out.replaceAll("%hID", Integer.toString(handID));
                    	
                    	IrcClient.getBot().sendIRCMessage( IrcChannel, out );
                        break;
                    } else {                    
                        // Tie; share the pot amongst winners.
                        int remainder = potsize % winners.size();
                        if (remainder != 0) {
                        	potsize -= remainder;
                        	rake += remainder;
                        }
                        int potShare = potsize / winners.size();
                        
                        int y = 0;
                        for (Player winner : winners) {
                            // Give the player his share of the pot.
                            winner.win(potShare);
                            
                            // Add to db
                            try {
	                            if (i == 0 && y == 0) {
	                                Database.getInstance().setHandWinner(handID, winner.getName(), potShare);
	                        	} else {
	                                Database.getInstance().addHandWinner(handID, winner.getName(), potShare);                    		
	                        	}
                	        } catch (Exception e) {
                	        	EventLog.log(e, "Table", "nextHand");
                	        }
                            
                            // Announce
                        	String out = Strings.PotWinner.replaceAll("%winner", winner.getName());
                        	out = out.replaceAll("%hand", handValue.toString());
                        	out = out.replaceAll("%pot", potname);
                        	out = out.replaceAll("%amount", Integer.toString(potShare));
                        	out = out.replaceAll("%id", Integer.toString(tableID));
                        	out = out.replaceAll("%hID", Integer.toString(handID));
                        	IrcClient.getBot().sendIRCMessage( IrcChannel, out );   
                        	y++;
                        }
                        break;
                    }
                }
                // Add this pot's rake to the total rake
        		totalrake += rake;
	    	} else {
	    		// Only one player, pot is returned.
	    		Player returnee = side.getPlayers().get(0);
	    		int returned = side.getBet() - totalbet;
	    		returnee.win( returned );
	    		
            	String out = Strings.PotReturned.replaceAll("%winner", returnee.getName());
            	out = out.replaceAll("%amount", Integer.toString(returned));
            	out = out.replaceAll("%id", Integer.toString(tableID));
            	out = out.replaceAll("%hID", Integer.toString(handID));
            	IrcClient.getBot().sendIRCMessage( IrcChannel, out );
	    	}

    		// Each side pot contains the previous pot's total, so remove it
    		totalpot = side.getPot();
    		totalbet = side.getBet();
    		
    		// Increase pot number
    		i++;
    	}
    	
    	// Announce rake
    	String out = Strings.RakeTaken.replaceAll("%rake", Integer.toString(totalrake));
    	out = out.replaceAll("%id", Integer.toString(tableID));
    	out = out.replaceAll("%hID", Integer.toString(handID));
        IrcClient.getBot().sendIRCMessage( IrcChannel, out );

        startShowCards();
        
        // Update the jackpot.
        boolean can_win = updateJackpot(IrcClient, totalrake, Profile);
    	
    	// Check if this hand wins.
    	if ( can_win && checkJackpot() ) jackpotWon();
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
        
        // Add to DB
        try {
	        Database.getInstance().setHandWinner(handID, player.getName(), pot);	
	    } catch (Exception e) {
	    	EventLog.log(e, "Table", "nextHand");
	    }
        
        // Announce
        String out = Strings.PlayerWins.replaceAll("%hID", Integer.toString(handID));
        out = out.replaceAll("%who", player.getName());
        out = out.replaceAll("%id", Integer.toString(tableID));
        out = out.replaceAll("%amount", Integer.toString(pot));
        out = out.replaceAll("%total", Integer.toString(player.getChips()));   
        out = out.replaceAll("%rake", Integer.toString(rake));     
        IrcClient.getBot().sendIRCMessage(IrcChannel, out );
        
        pot = 0;
        
        startShowCards();        
        
        // Update the jackpot.
    	boolean can_win = updateJackpot(IrcClient, rake, Profile);
    	
    	// Check if this hand wins.
    	if ( can_win && checkJackpot() ) jackpotWon();
    }
    	
    /**
     * Schedules the wait for players timer
     */
    private synchronized void scheduleWaitForPlayers() {
		waitForPlayersTimer = new Timer(true);
		waitForPlayersTimer.schedule(new TableTask( this, TableTask.WaitForPlayersTaskName),
				Variables.WaitingForPlayersSecs*1000);
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
    	IrcClient.getBot().sendIRCMessage( IrcChannel, out );
    	
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
        IrcClient.getBot().sendIRCMessage( IrcChannel, out );
        
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
        bet = bigBlind;
        minBet = bigBlind;
        playerBets.put(actor, bigBlind);

        String out = Strings.BigBlindPosted.replaceAll("%bb", Integer.toString(bigBlind) );
        out = out.replaceAll("%player", actor.getName() );
        IrcClient.getBot().sendIRCMessage( IrcChannel, out );
        
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
            IrcClient.getBot().sendIRCMessage( player.getName(), out );
        }
        IrcClient.getBot().sendIRCMessage( IrcChannel,
        		Strings.HoleCardsDealt.replaceAll("%hID", Integer.toString(handID)) );
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
        phaseStrings.put(phaseName, cardstr);
        
        String board_out = createBoardOutput(phaseName);
        
        // Notify channel of the card(s) dealt
   	 	String out = Strings.CommunityDealt.replaceAll("%hID", Integer.toString(handID));
   		out = out.replaceAll("%round", phaseName );
   		out = out.replaceAll("%cards", board_out );
   		IrcClient.getBot().sendIRCMessage( IrcChannel, out );
   	 
   		// Notify each player of all their cards
        for (Player player : activePlayers) {
        	Card[] cards = player.getCards();
        	cardstr = " %n[";
        	for (int i = 0; i < cards.length; i++) {
        		if (cards[i] != null) {
                    cardstr = cardstr + cards[i].toIRCString() + "%n ";        			
        		}
        	}
			cardstr += "] " + board_out;
        	
       	 	out = Strings.CommunityDealtPlayer.replaceAll("%hID", Integer.toString(handID));
       	    out = out.replaceAll("%id", Integer.toString(tableID) );
       	    out = out.replaceAll("%round", phaseName );
       	    out = out.replaceAll("%cards", cardstr );
       	    IrcClient.sendIRCMessage( player.getName(), out );
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
        phaseStrings.clear();
        
		sidePots.clear();
        
        activePlayers.clear();
        jackpotPlayers.clear();
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
                jackpotPlayers.add(player);
            }
            try {
            	Database.getInstance().addPokerTableCount(player.getName(), tableID, Profile, player.getChips());
	        } catch (Exception e) {
	        	EventLog.log(e, "Table", "nextHand");
	        }
        }
        
        // Sit all broke players out.
        for (Player player: remove_list) {
        	String out = Strings.OutOfChips.replaceAll("%player", player.getName());
        	out = out.replaceAll("%id", Integer.toString(tableID));
       		IrcClient.getBot().sendIRCMessage(IrcChannel, out);
        	playerSitsOut(player);
        }
        
        if (activePlayers.size() >= minPlayers) {
        	handActive = true;
        	deck.shuffle();        
    		
        	dealerPosition = (dealerPosition + activePlayers.size() - 1) % activePlayers.size();
        	if (players.size() == 2) {
	        	actorPosition = dealerPosition;
        	} else {
        		actorPosition = (dealerPosition + 1) % activePlayers.size();
        	}
        	actor = activePlayers.get(actorPosition);
        	int bbPosition = (actorPosition + 1) % activePlayers.size();
        
	        minBet = bigBlind;
	        bet = minBet;
	        
	        try {
	        	handID = Database.getInstance().getHandID();
	        } catch (Exception e) {
	        	EventLog.log(e, "Table", "nextHand");
	        }
	        
	        String out = Strings.NewHandMessage.replaceAll( "%hID", Integer.toString(handID) );
	        out = out.replaceAll("%dealer", players.get(dealerPosition).getName() );
	        out = out.replaceAll("%sb", players.get( actorPosition ).getName() );
	        out = out.replaceAll("%bb", players.get( bbPosition ).getName() );
	        
	        IrcClient.getBot().sendIRCMessage( IrcChannel, out );
	        
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
		IrcClient.closeTable( this );
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
			for (Player player: playerlist) {
				if (player.getUser().compareTo(users[i]) == 0) {
					IrcClient.getBot().voice(IrcChannel, users[i]);
					if ( satOutPlayers.contains(player ) ) { playerSitsDown( player ); }
					break;
				}
			}
			
			// user didn't exist, devoice
			if ( users[i].getNick().compareToIgnoreCase(IrcClient.getBot().getNick()) != 0 ) {
				IrcClient.getBot().deOp(IrcChannel, users[i]);
				IrcClient.getBot().deVoice(IrcChannel, users[i]);
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
	
	/**
	 * Creates the public output of cards for the board
	 * 
	 * @param phaseName The current board
	 * @return the cards output
	 */
	private String createBoardOutput(String phaseName) {
		String output = "";
		if (phaseName.compareTo(FLOPSTR) == 0) {
			output = phaseStrings.get(FLOPSTR);
		} else if (phaseName.compareTo(TURNSTR) == 0) {
			output = phaseStrings.get(FLOPSTR) + "| ";
			output += phaseStrings.get(TURNSTR);
		} else if (phaseName.compareTo(RIVERSTR) == 0) {
			output = phaseStrings.get(FLOPSTR) + "| ";
			output += phaseStrings.get(TURNSTR) + "| ";
			output += phaseStrings.get(RIVERSTR);
		}
		return output;
	}
	
	/**
	 * Enables the ability for players to show cards.
	 */
	private void startShowCards() {
		actor = null;
        canShow = true;
		String out = Strings.StartShowCard.replaceAll( "%id", Integer.toString(tableID) );
		out = out.replaceAll( "%hID", Integer.toString(handID) );
		out = out.replaceAll( "%secs", Integer.toString(Variables.ShowCardSecs));
		IrcClient.getBot().sendIRCMessage( IrcChannel, out );					
        showCardsTimer = new Timer(true);
        showCardsTimer.schedule( new TableTask( this, TableTask.ShowCardTaskName), Variables.ShowCardSecs*1000 );
	}
	
	
	/**
	 * Save the new jackpot value
	 */
	private static synchronized boolean updateJackpot(Client irc, int rake, ProfileType profile) {
		boolean added = false;
		int jackpot = 0;
		try {
			jackpot = Database.getInstance().getJackpot(profile);
		} catch (Exception e) {
			EventLog.log(e, "Table", "updateJackpot");
		}
		
		double incr = (rake * (Variables.JackpotRakePercentage / 100.0));
		int incrint = (int) Math.round(incr);
		
		EventLog.log(profile + " jackpot: " + Integer.toString(jackpot) + " + "
					 + Integer.toString(incrint) + " (" + Integer.toString(rake) + ")",
					 "Table", "updateJackpot");
		
		if (incrint > 0) {
			added = true;
			jackpot += incrint;
			// Announce to lobbyChan
			String out = Strings.JackpotIncreased.replaceAll("%chips", Integer.toString(jackpot));
			out = out.replaceAll("%profile", profile.toString());
			irc.sendIRCMessage(out);
			 
			try {
				Database.getInstance().updateJackpot(profile, incrint);
			} catch (Exception e) {
				EventLog.log(e, "Table", "updateJackpot");
			}
		}
		return added;
	}
	
	/**
	 * Check if the jackpot has been won
	 */
	private static synchronized boolean checkJackpot() {
		return (Random.nextInt(Variables.JackpotChance + 1) == Variables.JackpotChance);		
	}
	
	/**
	 * Jackpot has been won, split between all players on the table
	 */
	private void jackpotWon() {		
		int jackpot = 0;
		try {
			Database.getInstance().getJackpot(Profile);
		} catch (Exception e) {
			EventLog.log(e, "Table", "jackpotWon");
		}
		 
		if (jackpot > 0) {
			int remainder = jackpot % players.size();
			jackpot -= remainder;
			
			if (jackpot != 0) {
				int win = jackpot / players.size();
				for (Player player: jackpotPlayers) {
					try {
						Database.getInstance().jackpot(player.getName(), win, Profile);
					} catch (Exception e) {
						EventLog.log(e, "Table", "jackpotWon");
					}
				}
				
				// Announce to lobby
				String out = Strings.JackpotWon.replaceAll("%chips", Integer.toString(jackpot));
				out = out.replaceAll("%profile", Profile.toString());
				out = out.replaceAll("%winners", jackpotPlayers.toString());
				IrcClient.sendIRCMessage(out);
				IrcClient.sendIRCMessage(out);
				IrcClient.sendIRCMessage(out);
				
				// Announce to table
				out = Strings.JackpotWonTable.replaceAll("%chips", Integer.toString(win));
				out = out.replaceAll("%profile", Profile.toString());
				out = out.replaceAll("%winners", jackpotPlayers.toString());
				IrcClient.getBot().sendIRCMessage(IrcChannel, out);
				IrcClient.getBot().sendIRCMessage(IrcChannel, out);
				IrcClient.getBot().sendIRCMessage(IrcChannel, out);
				
				// Update jackpot with remainder
				if (remainder > 0) {
					out = Strings.JackpotIncreased.replaceAll("%chips", Integer.toString(remainder));
					out = out.replaceAll("%profile", Profile.toString());
					IrcClient.sendIRCMessage(out);
				}
				try {
					Database.getInstance().updateJackpot(Profile, remainder);
				} catch (Exception e) {
					EventLog.log(e, "Table", "jackpotWon");
				}
			}
		}
	}
}
