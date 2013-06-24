/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.game.rooms;

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
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.NickChange;
import org.smokinmils.bot.events.Op;
import org.smokinmils.bot.events.Part;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.cards.Card;
import org.smokinmils.games.casino.cards.Deck;
import org.smokinmils.games.casino.poker.Client;
import org.smokinmils.games.casino.poker.enums.ActionType;
import org.smokinmils.games.casino.poker.enums.CommandType;
import org.smokinmils.games.casino.poker.enums.RoomType;
import org.smokinmils.games.casino.poker.game.Hand;
import org.smokinmils.games.casino.poker.game.HandValue;
import org.smokinmils.games.casino.poker.game.Player;
import org.smokinmils.games.casino.poker.game.SidePot;
import org.smokinmils.games.casino.poker.tasks.TableTask;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.PokerStrs;
import org.smokinmils.settings.PokerVars;

/**
 * The IRC and poker functionality for a poker table.
 * 
 * @author Jamie Reid
 */
public class Table extends Room {
    /** Stores the most recent table's ID. */
    private static Integer               currentID  = 0;

    /** A map of all tables and their big blind. */
    private static Map<Integer, Integer> tables
                                            = new HashMap<Integer, Integer>();

    /** The ID of this table. */
    private final int                    tableID;

    /** The profile ID of this table. */
    private final ProfileType            profile;

    /** The size of the big blind. */
    private final int                    bigBlind;

    /** The size of the small blind. */
    private final int                    smallBlind;

    /** The players at the table. */
    private final List<Player>           players;

    /** The active players in the current hand. */
    private final List<Player>           activePlayers;

    /** The active players in the current hand. */
    private final List<Player>           jackpotPlayers;

    /** The players at the table but sitting out. */
    private final List<Player>           satOutPlayers;

    /** The observers watching the table. */
    private final Vector<String>         observers;

    /** The minimum players permitted. */
    private final int                    minPlayers;

    /** The maximum players permitted. */
    private final int                    maxPlayers;

    /** The maximum buy in permitted. */
    private final int                    minBuyIn;

    /** The minimum buy in permitted. */
    private final int                    maxBuyIn;

    /** The current hand id. */
    private int                          handID;

    /** The maximum number of bets or raises in a single hand per player. */
    private static final int             MAX_RAISES = 4;

    /** The deck of cards. */
    private Deck                         deck;

    /** The community cards on the board. */
    private final List<Card>             board;

    /** The current dealer position. */
    private int                          dealerPosition;

    /** The position of the acting player. */
    private int                          actorPosition;

    /** The acting player. */
    private Player                       actor;

    /** The minimum bet in the current hand. */
    private int                          minBet;

    /** The current bet in the current hand. */
    private int                          bet;

    /** The pot in the current hand. */
    private int                          pot;

    /** Marks the table as having an active hand. */
    private boolean                      handActive;

    /** Timer to handle waiting for enough players. */
    private Timer                        waitForPlayersTimer;
    
    /** The number of times the waiting for players timer has run. */
    private int                          waitedCount;

    /** Timer to handle a request to show cards. */
    private Timer                        showCardsTimer;
    
    /** Denotes if showing cards is enabled. */
    private boolean                      canShow;

    /** Timer to start a game. */
    private Timer                        startGameTimer;

    /** Timer to wait for an action. */
    private Timer                        actionTimer;

    /** Action received lock. */
    private boolean                      actionReceived;

    /** Boolean used for when a hand was cancelled due to a disconnect. */
    private boolean                      disconnected;

    /** Side pots. */
    private final List<SidePot>          sidePots;

    /** Player bets. */
    private Map<Player, Integer>         playerBets;

    /** Hand card strings. */
    private final Map<String, String>    phaseStrings;

    /** Rounds - Preflop. */
    private static final int             PREFLOP    = 0;

    /** Rounds - Flop. */
    private static final int             FLOP       = 1;

    /** Rounds - Flop String. */
    private static final String          FLOPSTR    = "Flop";

    /** Rounds - Turn. */
    private static final int             TURN       = 2;

    /** Rounds - Turn String. */
    private static final String          TURNSTR    = "Turn";

    /** Rounds - River. */
    private static final int             RIVER      = 3;

    /** Rounds - River String. */
    private static final String          RIVERSTR   = "River";

    /** The round of the current hand. */
    private int                          currentRound;

    /** Number of players left to act. */
    private volatile int                 playersToAct;

    /** Number of players left to act. */
    private final boolean                createdManually;

    /**
     * Constructor.
     * 
     * @param channel The channel this bot is running on
     * @param irc The IRC client
     * @param id the table id
     * @param bb The size of the big blind.
     * @param max Maximum allowed players on the table
     * @param prof The profile this table is for
     * @param manual Whether the table was created auto or manually
     */
    public Table(final Channel channel, final Client irc, final int id,
            final int bb, final int max, final ProfileType prof,
            final boolean manual) {
        super(channel, irc, RoomType.TABLE);
        boolean failed = false;
        players = new ArrayList<Player>();
        activePlayers = new ArrayList<Player>();
        satOutPlayers = new ArrayList<Player>();
        jackpotPlayers = new ArrayList<Player>();
        board = new ArrayList<Card>();
        phaseStrings = new HashMap<String, String>();

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

        currentID++;
        tableID = id;

        profile = prof;

        observers = new Vector<String>();
        maxPlayers = max;
        minPlayers = PokerVars.MINPLAYERS;
        playersToAct = 0;
        currentRound = 0;
        actionReceived = false;

        // Ensure big blind is even
        if (bb <= 1) {
            bigBlind = 2;
        } else if ((bb % 2) == 1) {
            bigBlind = bb + 1;
        } else {
            bigBlind = bb;
        }
        tables.put(tableID, bigBlind);
        smallBlind = bigBlind / 2;

        // Calculate min and max buyins
        minBuyIn = PokerVars.MINBUYIN * bigBlind;
        maxBuyIn = PokerVars.MAXBUYIN * bigBlind;

        dealerPosition = -1;
        handID = -1;

        setTopic();
        scheduleWaitForPlayers();
        if (failed) {
            getIrcClient().sendIRCMessage(
               "Something caused the bot to crash... please notify the staff.");
            System.exit(1);
        }
    }

    /**
     * Returns the table ID.
     * 
     * @return The table ID
     */
    public final int getTableID() {
        return tableID;
    }

    /**
     * Returns the profile name.
     * 
     * @return The profile name
     */
    public final ProfileType getProfile() {
        return profile;
    }

    /**
     * Returns the small blind.
     * 
     * @return The small blind
     */
    public final int getSmallBlind() {
        return smallBlind;
    }

    /**
     * Returns the big blind.
     * 
     * @return The big blind
     */
    public final int getBigBlind() {
        return bigBlind;
    }

    /**
     * Returns the players at the table.
     * 
     * @return The players at the table
     */
    public final Player[] getPlayers() {
        return (Player[]) players.toArray();
    }

    /**
     * Returns the minimum buy in.
     * 
     * @return The minimum buy in
     */
    public final int getMinBuyIn() {
        return minBuyIn;
    }

    /**
     * Returns the maximum buy in.
     * 
     * @return The maximum buy in
     */
    public final int getMaxBuyIn() {
        return maxBuyIn;
    }

    /**
     * Returns the number of players at the table.
     * 
     * @return The number of players
     */
    public final int getNoOfPlayers() {
        return players.size() + satOutPlayers.size();
    }

    /**
     * Returns the minimum number of players for play to commence.
     * 
     * @return The minimum number of players
     */
    public final int getMinPlayers() {
        return minPlayers;
    }

    /**
     * Returns the maximum number of players for play to commence.
     * 
     * @return The maximum number of players
     */
    public final int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Returns the number of players who are sat down/active.
     * 
     * @return The number of players
     */
    public final int getPlayersSatDown() {
        return players.size();
    }

    /**
     * Returns the next table ID.
     * 
     * @return The next available ID
     */
    public static final Integer getNextID() {
        Integer nextid = currentID + 1;
        for (Integer i = 1; i < nextid; i++) {
            if (!tables.containsKey(i)) {
                nextid = i;
                break;
            }
        }
        return nextid;
    }

    /**
     * Returns the Map of all tables and their big blind.
     * 
     * @return The map of all table IDs and big blinds
     */
    public static Map<Integer, Integer> getTables() {
        return tables;
    }

    /**
     * This method checks if the table is full.
     * 
     * @return true if the table is full. false otherwise
     */
    public final boolean isFull() {
        return ((players.size() + satOutPlayers.size()) == maxPlayers);
    }

    /**
     * This method returns how many seats are left.
     * 
     * @return the number of remaining seats
     */
    public final int seatsLeft() {
        return (maxPlayers - players.size());
    }

    /**
     * This method is used to check if a user is already watching the table.
     * 
     * @param user The user.
     * 
     * @return true if the user is not already watching
     */
    public final boolean canWatch(final User user) {
        return observers.contains(user.getNick());
    }

    /**
     * This method is used to check if a user is already playing at the table.
     * 
     * @param user The user.
     * 
     * @return true if the user is not already playing
     */
    public final boolean canPlay(final User user) {
        Player found = findPlayer(user);
        boolean ret = false;
        if (found == null) {
            ret = true;
        }
        return ret;
    }

    /**
     * Outputs a table message replacing all variables.
     * 
     * @param in The unformatted string
     * 
     * @return The formatted string
     */
    public final String formatTableInfo(final String in) {
        int seats = maxPlayers - (players.size() + satOutPlayers.size());
        String out = in.replaceAll("%id", Integer.toString(tableID));
        out = out.replaceAll("%sb", Integer.toString(smallBlind));
        out = out.replaceAll("%bb", Integer.toString(bigBlind));
        out = out.replaceAll("%min", Integer.toString(minBuyIn));
        out = out.replaceAll("%max", Integer.toString(maxBuyIn));
        out = out.replaceAll("%Pcur",
                Integer.toString(players.size() + satOutPlayers.size()));
        out = out.replaceAll("%Pmin", Integer.toString(minPlayers));
        out = out.replaceAll("%Pmax", Integer.toString(maxPlayers));
        out = out.replaceAll("%seats", Integer.toString(seats));
        out = out.replaceAll("%watching", Integer.toString(observers.size()));
        out = out.replaceAll("%hID", Integer.toString(handID));
        out = out.replaceAll("%profile", profile.toString());
        return out;
    }

    /**
     * Adds a player to the table.
     * 
     * @param sender The player.
     * @param buyin The amount bought in for.
     */
    public final synchronized void playerJoins(final User sender,
                                               final Integer buyin) {
        Player player = new Player(sender, buyin);
        boolean found = false;
        Set<User> users = getChannel().getUsers();
        for (User user : users) {
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
            DB.getInstance().buyIn(sender.getNick(), buyin, profile);
            DB.getInstance().addPokerTableCount(sender.getNick(), tableID,
                                                profile, buyin);
        } catch (Exception e) {
            EventLog.log(e, "Table", "playerJoins");
        }

        // Invite the player to join channel
        getChannel().getBot().sendIRC().invite(sender.getNick(),
                                                getChannel().getName());

        // Try to voice the user incase they were watching
        getChannel().send().voice(sender);
        observers.remove(sender.getNick());

        // Announce
        String out = PokerStrs.PlayerJoins.replaceAll(
                "%id", Integer.toString(tableID));
        out = out.replaceAll("%player", player.getName());
        out = out.replaceAll("%chips", Integer.toString(player.getChips()));
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);
        setTopic();
    }

    /**
     * A player has left the table.
     * 
     * @param player the player who left
     * @param satout true if the player was sat out prior to leaving
     */
    public final synchronized void playerLeaves(final Player player,
                                                final boolean satout) {
        String name = player.getName();
        int chips = player.getChips() + player.getRebuy();

        // Ensure user has left the table
        User user = player.getUser();
        if (createdManually) {
            getChannel().send().kick(user);
        } else {
            getChannel().send().deVoice(user);
        }
        // Cash out
        if (!player.isBroke()) {
            player.cashOut();
            try {
                DB.getInstance().cashOut(name, chips, profile);
                DB.getInstance().addPokerTableCount(name, tableID, profile, 0);
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
        if (player.getSittingOutTimer() != null) {
            player.getSittingOutTimer().cancel();
        }

        // Announce
        String out = PokerStrs.PlayerLeaves.replaceAll(
                "%id", Integer.toString(tableID));
        out = out.replaceAll("%player", name);
        out = out.replaceAll("%chips", Integer.toString(chips));
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);
        getIrcClient().getBot().sendIRCNotice(player.getUser(),
                PokerStrs.PlayerLeavesPM.replaceAll(
                        "%id", Integer.toString(tableID)));
        setTopic();
    }

    /**
     * A player has sat back down.
     * 
     * @param player the player who left
     */
    private synchronized void playerSitsDown(final Player player) {
        if (player.getSittingOutTimer() != null) {
            player.getSittingOutTimer().cancel();
        }

        // Switch the player lists
        satOutPlayers.remove(player);
        players.add(player);

        player.setSatOut(false);

        // Announce
        String out = PokerStrs.PlayerSitsDown.replaceAll(
                "%id", Integer.toString(tableID));
        out = out.replaceAll("%player", player.getName());
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);
    }

    /**
     * Called when a player is auto sat out or chooses to sit out.
     * 
     * @param player the player.
     */
    private synchronized void playerSitsOut(final Player player) {
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
        String out = PokerStrs.PlayerSitsOut.replaceAll(
                "%id", Integer.toString(tableID));
        out = out.replaceAll("%player", player.getName());
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);
    }

    /**
     * This method is called whenever a message is sent to this channel.
     * 
     * @param event the Message event.
     */
    @Override
    protected final synchronized void onMessage(final Message event) {
        String message = event.getMessage();
        User sender = event.getUser();
        int fSpace = message.indexOf(" ");
        if (fSpace == -1) {
            fSpace = message.length();
        }
        String firstWord = message.substring(0, fSpace).toLowerCase();

        if ((fSpace + 1) <= message.length()) {
            fSpace++;
        }
        String restofmsg = message.substring(fSpace, message.length());
        String[] msg = restofmsg.split(" ");

        CommandType cmd = CommandType.fromString(firstWord);
        if (cmd != null) {
            boolean isActor = false;
            if (actor != null) {
                isActor = (actor.getUser().compareTo(sender) == 0);
            }
            
            switch (cmd) {
            case CHECK:
                if ((msg.length == 0 || msg[0].compareTo("") == 0) && isActor) {
                    if (bet == 0 || actor.getBet() >= bet) {
                        onAction(ActionType.CHECK, restofmsg, false);
                    } else {
                        onAction(ActionType.CALL, restofmsg, false);
                    }
                } else if (!isActor) {
                    invalidAction(sender);
                } else {
                    invalidArguments(sender, cmd.getFormat());
                }
                break;
            case RAISE:
                if (msg.length == 1 && msg[0].compareTo("") != 0 && isActor) {
                    if (bet == 0) {
                        onAction(ActionType.BET, restofmsg, false);
                    } else {
                        onAction(ActionType.RAISE, restofmsg, false);
                    }
                } else if (!isActor) {
                    invalidAction(sender);
                } else {
                    invalidArguments(sender, cmd.getFormat());
                }
                break;
            case FOLD:
                if ((msg.length == 0 || msg[0].compareTo("") == 0) && isActor) {
                    onAction(ActionType.FOLD, restofmsg, false);
                } else if (!isActor) {
                    invalidAction(sender);
                } else {
                    invalidArguments(sender, cmd.getFormat());
                }
                break;
            case SHOW:
                onShow(sender, restofmsg);
                break;
            case TBLCHIPS:
                onChips(sender, restofmsg);
                break;
            case REBUY:
                onRebuy(sender, restofmsg);
                break;
            case SITDOWN:
                onSitDown(sender, restofmsg);
                break;
            case LEAVE:
                onLeave(sender, restofmsg);
                break;
            case SITOUT:
                onSitOut(sender, restofmsg);
                break;
            default:
                // Nothing to be done
            }
        }
    }

    /**
     * This method is called whenever someone joins a channel which we are on.
     * 
     * @param event The event.
     */
    @Override
    protected final synchronized void onJoin(final Join event) {
        User sender = event.getUser();
        String senderu = sender.getNick();
        String botu = getIrcClient().getBot().getNick();
        if (!senderu.equalsIgnoreCase(botu)) {
            Player found = null;
            for (Player plyr : satOutPlayers) {
                if (plyr.getUser().compareTo(sender) == 0) {
                    found = plyr;
                    break;
                }
            }

            if (found != null) {
                getChannel().send().voice(sender);
                playerSitsDown(found);
            } else {
                observers.add(senderu.toLowerCase());
            }
        } else {
            getChannel().send().setMode("+m");
        }
        setTopic();
    }

    /**
     * This method is called whenever someone parts this channel which we are
     * on. This is also the handler for whenever someone quits from the channel
     * 
     * @param event the event.
     */
    @Override
    protected final synchronized void onPart(final Part event) {
        User sender = event.getUser();
        String senderu = sender.getNick();
        String botu = getIrcClient().getBot().getNick();
        if (!senderu.equalsIgnoreCase(botu)) {
            Player found = null;
            for (Player plyr : players) {
                if (plyr.getUser().compareTo(sender) == 0) {
                    found = plyr;
                    break;
                }
            }

            if (found != null) {
                playerSitsOut(found);

                // not enough players, wait.
                if (players.size() < minPlayers) {
                    if (waitForPlayersTimer == null) {
                        scheduleWaitForPlayers();
                    }
                }
            } else if (observers.contains(senderu.toLowerCase())) {
                observers.remove(senderu.toLowerCase());
            }
        }
    }

    /**
     * This method is called whenever someone changes nick on this channel.
     * 
     * @param event the event.
     */
    @Override
    protected synchronized void onNickChange(final NickChange event) {
        // TODO: code to handle this.
    }

    /**
     * Called when a user (possibly us) gets granted operator status for a
     * channel.
     * 
     * @param event the event.
     */
    @Override
    protected final synchronized void onOp(final Op event) {
        String who = event.getRecipient().getNick();
        if (who.equalsIgnoreCase(getIrcClient().getBot().getNick())) {
            setTopic();
            getChannel().send().setMode("+m");
        }
    }

    /**
     * Called when a user needs to send a message back to the room.
     * 
     * @param timername The type of timer that requires attention.
     */
    @Override
    protected final synchronized void onTimer(final String timername) {
        switch (timername) {
        case TableTask.ACTION:
            if (!actionReceived && actor != null) {
                String out = PokerStrs.NoActionWarning.replaceAll(
                        "%hID", Integer.toString(handID));
                out = out.replaceAll("%actor", actor.getName());
                out = out.replaceAll(
                        "%secs",
                        Integer.toString(PokerVars.ACTIONWARNINGSECS));
                getIrcClient().getBot().sendIRCMessage(getChannel(), out);

                if (actionTimer != null) {
                    actionTimer.cancel();
                }
                actionTimer = new Timer(true);
                actionTimer.schedule(
                        new TableTask(this, TableTask.ACTIONWARNING),
                        PokerVars.ACTIONWARNINGSECS * Utils.MS_IN_SEC);
            }
            break;
        case TableTask.ACTIONWARNING:
            if (actionTimer != null) {
                actionTimer.cancel();
            }
            if (!actionReceived) {
                noActionReceived();
            }
            break;
        case TableTask.SHOWCARDS:
            if (showCardsTimer != null) {
                showCardsTimer.cancel();
            }
            canShow = false;
            nextHand();
            break;
        case TableTask.STARTGAME:
            if (startGameTimer != null) {
                startGameTimer.cancel();
            }
            // Do we need players at the table?
            if (players.size() < minPlayers) {
                // Not enough players, wait again
                scheduleWaitForPlayers();
            } else if (!handActive) {
                // No players needed, start playing
                nextHand();
            }

            break;
        case TableTask.WAITFORPLAYERS:
            if (waitForPlayersTimer != null) {
                waitForPlayersTimer.cancel();
            }
            // Do we need players at the table?
            if (getNoOfPlayers() == 0) {
                if (createdManually && waitedCount > PokerVars.WAITTIMES) {
                    closeTable();
                } else {
                    waitedCount++;
                    scheduleWaitForPlayers();
                }
            } else if (players.size() < minPlayers) {
                waitedCount = 0;
                // Continue waiting
                int need = minPlayers - players.size();
                String out = PokerStrs.WaitingForPlayersMsg.replaceAll(
                        "%need", Integer.toString(need));
                out = out.replaceAll("%min", Integer.toString(minPlayers));
                out = out.replaceAll("%max", Integer.toString(maxPlayers));
                out = out.replaceAll(
                        "%seated", Integer.toString(players.size()));
                getIrcClient().getBot().sendIRCMessage(getChannel(), out);

                scheduleWaitForPlayers();
            } else {
                String out = PokerStrs.GameStartMsg.replaceAll(
                        "%bb", Integer.toString(getBigBlind()));
                out = out.replaceAll("%sb", Integer.toString(getSmallBlind()));
                out = out.replaceAll(
                        "%secs", Integer.toString(PokerVars.GAMESTARTSECS));
                out = out.replaceAll(
                        "%seatedP", Integer.toString(getPlayersSatDown()));
                getIrcClient().getBot().sendIRCMessage(getChannel(), out);

                waitedCount = 0;

                // Schedule the game to start
                startGameTimer = new Timer(true);
                startGameTimer.schedule(
                        new TableTask(this, TableTask.STARTGAME),
                        PokerVars.GAMESTARTSECS * Utils.MS_IN_SEC);
            }
            break;
        default:
            break;
        }
    }

    /**
     * This method handles the chips command.
     * 
     * @param sender The person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private synchronized void onChips(final User sender,
                                      final String message) {
        String[] msg = message.split(" ");
        if ((msg.length == 0 || msg[0].compareTo("") == 0)) {
            Player found = findPlayer(sender);

            if (found != null) {
                String out = PokerStrs.CheckChips.replaceAll(
                        "%id", Integer.toString(tableID));
                out = out.replaceAll(
                        "%creds", Integer.toString(found.getChips()));
                getIrcClient().getBot().sendIRCNotice(sender, out);
            } else {
                String out = PokerStrs.CheckChipsFailed.replaceAll(
                        "%id", Integer.toString(tableID));
                getIrcClient().getBot().sendIRCNotice(sender, out);
            }
        } else if ((msg.length == 1 && msg[0].compareTo("") != 0)) {
            Player found = findPlayer(msg[0]);

            if (found != null) {
                String out = PokerStrs.CheckChipsUser.replaceAll(
                        "%id", Integer.toString(tableID));
                out = out.replaceAll("%user", msg[0]);
                out = out.replaceAll(
                        "%creds", Integer.toString(found.getChips()));
                getIrcClient().getBot().sendIRCNotice(sender, out);
            } else {
                String out = PokerStrs.CheckChipsUserFailed.replaceAll(
                        "%id", Integer.toString(tableID));
                out = out.replaceAll("%user", msg[0]);
                getIrcClient().getBot().sendIRCNotice(sender, out);
            }
        } else {
            invalidArguments(sender, CommandType.TBLCHIPS.getFormat());
        }
    }

    /**
     * This method handles the chips command.
     * 
     * @param sender The nick of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private synchronized void onShow(final User sender,
                                     final String message) {
        Player found = findPlayer(sender);

        if (found == null) {
            String out = PokerStrs.ShowCardFailNoPlayer.replaceAll(
                    "%id", Integer.toString(tableID));
            out = out.replaceAll("%hID", Integer.toString(handID));
            getIrcClient().getBot().sendIRCNotice(sender, out);
        } else if (found.getCards().length == 0) {
            String out = PokerStrs.ShowCardFailNotActive.replaceAll(
                    "%id", Integer.toString(tableID));
            out = out.replaceAll("%hID", Integer.toString(handID));
            getIrcClient().getBot().sendIRCNotice(sender, out);
        } else if (!canShow) {
            String out = PokerStrs.ShowCardFailInHand.replaceAll(
                    "%id", Integer.toString(tableID));
            out = out.replaceAll("%hID", Integer.toString(handID));
            getIrcClient().getBot().sendIRCNotice(sender, out);
        } else {
            Card[] cards = found.getCards();
            String cardstr = "%n[";
            for (int i = 0; i < cards.length; i++) {
                if (cards[i] != null) {
                    cardstr = cardstr + cards[i].toIRCString() + "%n ";
                }
            }
            cardstr += "] ";

            String out = PokerStrs.ShowCards.replaceAll(
                    "%id", Integer.toString(tableID));
            out = out.replaceAll("%hID", Integer.toString(handID));
            out = out.replaceAll("%who", found.getName());
            out = out.replaceAll("%cards", cardstr);
            getIrcClient().getBot().sendIRCMessage(getChannel(), out);
        }
    }

    /**
     * This method handles the rebuy command.
     * 
     * @param sender The person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private synchronized void onRebuy(final User sender,
                                      final String message) {
        String[] msg = message.split(" ");
        Integer buyin = Utils.tryParse(msg[0]);
        if ((msg.length == 1 && msg[0].compareTo("") != 0) || buyin != null) {
            int maxbuy = (bigBlind * PokerVars.MAXBUYIN);
            int minbuy = (bigBlind * PokerVars.MINBUYIN);

            int credits = 0;
            try {
                credits = DB.getInstance().checkCreditsAsInt(
                                                 sender.getNick(), profile);
            } catch (Exception e) {
                EventLog.log(e, "Lobby", "onNewTable");
            }
            
            if (buyin != null && credits < buyin) {
                String out = PokerStrs.NoChipsMsg.replaceAll(
                        "%chips", Integer.toString(buyin));
                out = out.replaceAll("%profile", profile.toString());
                
                getIrcClient().getBot().sendIRCNotice(sender, out);
            } else if (buyin != null) {
                Player found = findPlayer(sender);

                if (found != null) {
                    int total = buyin + found.getRebuy() + found.getChips();
                    int diff = maxbuy - total;

                    if (total > maxbuy) {
                        String out = PokerStrs.RebuyFailure.replaceAll(
                                "%id", Integer.toString(tableID));
                        out = out.replaceAll(
                                "%maxbuy", Integer.toString(maxbuy));
                        out = out.replaceAll(
                                "%total",
                                Integer.toString(found.getChips()
                                        + found.getRebuy()));
                        getIrcClient().getBot().sendIRCNotice(sender, out);
                    } else if (diff < 0) {
                        String out = PokerStrs.IncorrectBuyInMsg.replaceAll(
                                "%buyin", Integer.toString(buyin));
                        out = out.replaceAll(
                                "%maxbuy", Integer.toString(maxbuy));
                        out = out.replaceAll(
                                "%minbuy", Integer.toString(minbuy));
                        out = out.replaceAll(
                                "%maxBB", Integer.toString(PokerVars.MAXBUYIN));
                        out = out.replaceAll(
                                "%minBB", Integer.toString(PokerVars.MINBUYIN));
                        getIrcClient().getBot().sendIRCNotice(sender, out);
                    } else {
                        // Remove chips from db
                        try {
                            DB.getInstance().buyIn(sender.getNick(),
                                                   buyin, profile);
                            DB.getInstance().addPokerTableCount(
                                    found.getName(), tableID, profile, buyin);
                        } catch (Exception e) {
                            EventLog.log(e, "Table", "nextHand");
                        }

                        found.rebuy(buyin);

                        String out = PokerStrs.RebuySuccess.replaceAll(
                                "%id", Integer.toString(tableID));
                        out = out.replaceAll("%user", found.getName());
                        out = out.replaceAll("%new", msg[0]);
                        out = out.replaceAll(
                                "%total",
                                Integer.toString(found.getChips()
                                        + found.getRebuy()));
                        getIrcClient().getBot().sendIRCMessage(
                                getChannel(), out);

                        if (satOutPlayers.contains(found)) {
                            playerSitsDown(found);
                        }
                    }
                } else {
                    EventLog.log(
                            "RECOVERABLE: User tried to rebuy but is not "
                             + "on the table", "Table", "onRebuy");

                    getChannel().send().deVoice(sender);

                    observers.add(sender.getNick().toLowerCase());
                }
            }
        } else {
            invalidArguments(sender, CommandType.REBUY.getFormat());
        }
    }

    /**
     * This method handles the leave command.
     * 
     * @param sender The nick of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private synchronized void onLeave(final User sender,
                                      final String message) {
        String[] msg = message.split(" ");
        if (msg.length == 0 || msg[0].compareTo("") == 0) {
            Player found = findPlayer(sender);
            boolean isactive = false;
            if (found != null) {
                isactive = players.contains(found);
            }

            if (found != null) {
                playerLeaves(found, isactive);
            } else {
                EventLog.log(sender
                      + "failed to leave as they should not have been voiced.",
                        "Table", "onLeave");
                getChannel().send().deVoice(sender);
            }
        } else {
            invalidArguments(sender, CommandType.LEAVE.getFormat());
        }
    }

    /**
     * This method handles the sitdown command.
     * 
     * @param sender The person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private synchronized void onSitDown(final User sender,
                                        final String message) {
        String[] msg = message.split(" ");
        if (msg.length == 0 || msg[0].compareTo("") == 0) {
            Player found = null;
            for (Player plyr : satOutPlayers) {
                if (plyr.getUser().compareTo(sender) == 0) {
                    found = plyr;
                    break;
                }
            }

            if (found != null && (found.isBroke() && found.getRebuy() == 0)) {
                getIrcClient().getBot().sendIRCNotice(
                        sender,
                        PokerStrs.SitOutFailed.replaceAll(
                                "%id", Integer.toString(tableID)));
            } else if (found != null) {
                playerSitsDown(found);
            }
        } else {
            invalidArguments(sender, CommandType.SITDOWN.getFormat());
        }
    }

    /**
     * This method handles the sitout command.
     * 
     * @param sender The user who sent the message.
     * @param message The actual message sent to the channel.
     */
    private synchronized void onSitOut(final User sender,
                                       final String message) {
        String[] msg = message.split(" ");
        if (msg.length == 0 || msg[0].compareTo("") == 0) {
            Player found = null;
            for (Player plyr : players) {
                if (plyr.getUser().compareTo(sender) == 0) {
                    found = plyr;
                    break;
                }
            }

            if (found != null) {
                playerSitsOut(found);
            } else {
                getIrcClient().getBot().sendIRCNotice(
                        sender,
                        PokerStrs.SitOutFailed.replaceAll(
                                "%id", Integer.toString(tableID)));
            }
        } else {
            invalidArguments(sender, CommandType.SITOUT.getFormat());
        }
    }

    /**
     * Announces the next actor and valid actions and waits.
     */
    private synchronized void getAction() {
        Set<ActionType> allowed = getAllowedActions(actor);
        String actions = allowed.toString();
        if (allowed.contains(ActionType.CALL)) {
            actions += " {%c04" + (bet - actor.getBet()) + "%c12 to call}";
        }
        if (allowed.contains(ActionType.BET)) {
            actions += " {%c04" + minBet + "%c12 to bet}";
        }
        if (allowed.contains(ActionType.RAISE)) {
            actions += " {%c04" + ((bet - actor.getBet()) + minBet)
                    + "%c12 to raise}";
        }

        String out = PokerStrs.GetAction.replaceAll(
                "%hID", Integer.toString(handID));
        out = out.replaceAll("%actor", actor.getName());
        out = out.replaceAll("%valid", actions);
        out = out.replaceAll("%hID", Integer.toString(handID));
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);

        actionReceived = false;

        // Schedule time out
        actionTimer = new Timer(true);
        actionTimer.schedule(new TableTask(this, TableTask.ACTION),
        (PokerVars.ACTIONSECS - PokerVars.ACTIONWARNINGSECS) * Utils.MS_IN_SEC);
    }

    /**
     * This method handles the results of an action and moves to the next
     * action.
     * 
     * @param isfirst If this was the first action for the round.
     */
    private synchronized void actionReceived(final boolean isfirst) {
        try {
            rotateActor();
            while (actor.isBroke() && playersToAct > 1) {
                playersToAct--;
                rotateActor();
            }
        } catch (IllegalStateException e) {
            EventLog.fatal(e, "Table", "actionReceived");
            System.exit(1);
        }

        if (playersToAct > 0 && !(isfirst && playersToAct == 1)) {
            getAction();
        } else {
            // Reset the results of betting in the previous round
            for (Player player : activePlayers) {
                player.resetBet();
            }

            // deal next round
            switch (currentRound) {
            case PREFLOP:
                currentRound++;
                if (activePlayers.size() > 1) {
                    bet = 0;
                    minBet = bigBlind;
                    dealCommunityCards(FLOPSTR, 1 + 1 + 1);
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
                throw new IllegalStateException(
                        "We have reached an unknown round");
            }
        }
    }

    /**
     * This method is called whenever the correct users acts.
     * 
     * @param act The action type.
     * @param extra Extra details for the action.
     * @param timeout If this action was due to a timeout.
     */
    private synchronized void onAction(final ActionType act,
                                       final String extra,
                                       final boolean timeout) {
        ActionType action = act;
        actionReceived = true;
        if (actionTimer != null) {
            actionTimer.cancel();
        }
        if (actor == null) {
            EventLog.debug(
                    "actor is null and we received an action: "
                            + action.toString(), "Table", "onAction");
        }
        if (actor != null) {
            Set<ActionType> allowedActions = getAllowedActions(actor);
            if (!allowedActions.contains(action)) {
                String out = PokerStrs.InvalidAction.replaceAll(
                        "%hID", Integer.toString(handID));
                out = out.replaceAll("%invalid", action.getName());
                out = out.replaceAll("%valid", allowedActions.toString());
                getIrcClient().getBot().sendIRCMessage(getChannel(), out);
                getIrcClient().getBot().sendIRCNotice(actor.getUser(), out);
            } else {
                Integer amount = Utils.tryParse(extra);
                if (amount == null) {
                    amount = 0;
                }

                int valid = 0;
                boolean stop = false;
                if (action == ActionType.CALL) {
                    valid = bet - actor.getBet();
                    if (actor.getChips() >= valid) {
                        amount = valid;
                    } else if (amount > actor.getChips()) {
                        amount = actor.getChips();
                    }
                } else if (action == ActionType.BET) {
                    valid = minBet;
                    if (amount < valid && actor.getChips() >= valid) {
                        String out = PokerStrs.InvalidBet.replaceAll(
                                "%hID", Integer.toString(handID));
                        out.replaceAll(
                                "%pChips", Integer.toString(actor.getChips()));
                        out.replaceAll("%min", Integer.toString(valid));
                        stop = true;
                    } else if (amount > actor.getChips()) {
                        amount = actor.getChips();
                    }
                } else if (action == ActionType.RAISE) {
                    valid = (bet - actor.getBet()) + minBet;
                    int tocall = bet - actor.getBet();
                    if (amount < valid) {
                        String out = PokerStrs.InvalidBet.replaceAll(
                                "%hID", Integer.toString(handID));
                        out.replaceAll(
                                "%pChips", Integer.toString(actor.getChips()));
                        out.replaceAll("%min", Integer.toString(valid));
                        stop = true;
                    } else if (actor.getChips() <= tocall) {
                        action = ActionType.CALL;
                    } else if (amount > (actor.getChips() + actor.getBet())) {
                        amount = actor.getChips() + actor.getBet();
                    }
                }
                String extrastr;
                if (!stop) {
                    playersToAct--;
                    switch (action) {
                    case CHECK:
                    case CALL:
                        extrastr = "";
                        break;
                    case BET:
                    case RAISE:
                        minBet = amount;
                        bet = minBet;
                        // Other players get one more turn.
                        playersToAct = activePlayers.size() - 1;
                        extrastr = " to " + Integer.toString(minBet);
                        break;
                    case FOLD:
                        try {
                            actor.setCards(null);
                        } catch (IllegalArgumentException
                                | IllegalStateException e) {
                            EventLog.fatal(e, "Table", "onAction");
                            System.exit(1);
                        }
                        activePlayers.remove(actor);
                        extrastr = "";
                        break;
                    default:
                        throw new IllegalArgumentException(
                                "Blind actions are not valid at this point");
                    }

                    try {
                        actor.act(action, minBet, bet);
                    } catch (IllegalArgumentException e) {
                        EventLog.fatal(e, "Table", "onAction");
                        System.exit(1);
                    }
                    pot += actor.getBetIncrement();

                    int playertotal = playerBets.get(actor)
                            + actor.getBetIncrement();
                    playerBets.put(actor, playertotal);

                    // If user is all in, announce it and side up a new side pot
                    // if needed
                    if (actor.isBroke()) {
                        String out = PokerStrs.PlayerAllIn.replaceAll(
                                "%hID", Integer.toString(handID));
                        out = out.replaceAll("%actor", actor.getName());
                        getIrcClient().getBot().sendIRCMessage(
                                getChannel(), out);
                    }

                    // Re-calulate all the pots
                    calculateSidePots();

                    String out = PokerStrs.TableAction.replaceAll(
                            "%hID", Integer.toString(handID));
                    out = out.replaceAll("%actor", actor.getName());
                    out = out.replaceAll("%action", action.getText());
                    out = out.replaceAll("%amount", extrastr);
                    out = out.replaceAll(
                            "%chips", Integer.toString(actor.getChips()));
                    out = out.replaceAll("%pot", Integer.toString(pot));
                    getIrcClient().getBot()
                            .sendIRCMessage(getChannel(), out);

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
     * Sends a message to the channel notifying the user that an action was
     * attempted out of turn.
     * 
     * @param sender the user who tried to act
     */
    private synchronized void invalidAction(final User sender) {
        String out;
        if (actor == null) {
            out = PokerStrs.InvalidActTime;
        } else {
            out = PokerStrs.InvalidActor.replaceAll("%actor", actor.getName());
            out = out.replaceAll("%hID", Integer.toString(handID));
        }

        out = out.replaceAll("%user", sender.getNick());
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);
    }

    /**
     * Performs a betting round.
     */
    private synchronized void doBettingRound() {
        // Determine the number of active players.
        playersToAct = activePlayers.size();
        for (Player p : activePlayers) {
            if (p.isBroke()) {
                playersToAct--;
            }
        }

        EventLog.debug(
                Integer.toString(handID) + ": next betting round", "Table",
                "doBettingRound");
        EventLog.debug(
                Integer.toString(handID) + ": playersToAct = "
                        + Integer.toString(playersToAct), "Table",
                "doBettingRound");
        EventLog.debug(Integer.toString(handID) + ": activePlayers = "
                + activePlayers.toString(), "Table", "doBettingRound");

        // Determine the initial player and bet size.
        if (board.size() == 0) {
            // Pre-Flop; player left of big blind starts, bet is the big blind.
            bet = bigBlind;
            if (players.size() == 2) {
                actorPosition = (dealerPosition + 1) % activePlayers.size();
            } else {
                actorPosition = (dealerPosition + 2) % activePlayers.size();
            }
        } else {
            // Otherwise, player left of dealer starts, no initial bet.
            bet = 0;
            actorPosition = dealerPosition;
        }

        EventLog.debug(Integer.toString(handID) + ": dealerPosition = "
                + Integer.toString(dealerPosition), "Table", "doBettingRound");
        EventLog.debug(Integer.toString(handID) + ": actorPosition = "
                + Integer.toString(actorPosition), "Table", "doBettingRound");

        actionReceived(true);
    }

    /**
     * Returns the allowed actions of a specific player.
     * 
     * @param player The player.
     * 
     * @return The allowed actions.
     */
    private synchronized Set<ActionType> getAllowedActions(final Player player) {
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
        for (Player left : activePlayers) {
            if (!left.isBroke()) {
                if (lastpot.getBet() == 0) {
                    lastpot.setBet(left.getTotalBet());
                    lastpot.call(left);
                } else {
                    lastpot.call(left);
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
        for (SidePot side : sidePots) {
            if (i == 0) {
                potname = "Main Pot";
            } else {
                potname = "Side Pot " + Integer.toString(i);
            }

            if (side.getPot() < 0) {
                EventLog.log(
                        "Hand " + Integer.toString(handID) + " " + potname
                                + " is less than 0, something went wrong!",
                        "Table", "doShowdown");
                continue;
            } else if (side.getPot() == 0) {
                EventLog.log("Hand " + Integer.toString(handID) + " " + potname
                        + " is 0, ignoring!", "Table", "doShowdown");
                continue;
            }

            // More than one player so decide the winner
            if (side.getPlayers().size() > 1) {
                // Take the rake
                int rake = 0;
                if (pot > (bigBlind * 2)) {
                    rake += side.rake();
                }
                int potsize = side.getPot() - totalpot;

                // Look at each hand value, sorted from highest to lowest.
                Map<HandValue, List<Player>> rankedPlayers = getRankedPlayers(side
                        .getPlayers());
                for (HandValue handValue : rankedPlayers.keySet()) {
                    // Get players with winning hand value.
                    List<Player> winners = rankedPlayers.get(handValue);
                    if (winners.size() == 1) {
                        // Single winner.
                        Player winner = winners.get(0);
                        winner.win(potsize);
                        // Add to DB
                        try {
                            if (i == 0) {
                                DB.getInstance().setHandWinner(
                                        handID, winner.getName(), potsize);
                            } else {
                                DB.getInstance().addHandWinner(
                                        handID, winner.getName(), potsize);
                            }
                        } catch (Exception e) {
                            EventLog.log(e, "Table", "nextHand");
                        }
                        String out = PokerStrs.PotWinner.replaceAll(
                                "%winner", winner.getName());
                        out = out.replaceAll("%hand", handValue.toString());
                        out = out.replaceAll("%pot", potname);
                        out = out.replaceAll(
                                "%amount", Integer.toString(potsize));
                        out = out.replaceAll("%id", Integer.toString(tableID));
                        out = out.replaceAll("%hID", Integer.toString(handID));

                        getIrcClient().getBot().sendIRCMessage(
                                getChannel(), out);
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
                                    DB.getInstance().setHandWinner(
                                            handID, winner.getName(), potShare);
                                } else {
                                    DB.getInstance().addHandWinner(
                                            handID, winner.getName(), potShare);
                                }
                            } catch (Exception e) {
                                EventLog.log(e, "Table", "nextHand");
                            }

                            // Announce
                            String out = PokerStrs.PotWinner.replaceAll(
                                    "%winner", winner.getName());
                            out = out.replaceAll("%hand", handValue.toString());
                            out = out.replaceAll("%pot", potname);
                            out = out.replaceAll(
                                    "%amount", Integer.toString(potShare));
                            out = out.replaceAll(
                                    "%id", Integer.toString(tableID));
                            out = out.replaceAll(
                                    "%hID", Integer.toString(handID));
                            getIrcClient().getBot().sendIRCMessage(
                                    getChannel(), out);
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
                returnee.win(returned);

                String out = PokerStrs.PotReturned.replaceAll(
                        "%winner", returnee.getName());
                out = out.replaceAll("%amount", Integer.toString(returned));
                out = out.replaceAll("%id", Integer.toString(tableID));
                out = out.replaceAll("%hID", Integer.toString(handID));
                getIrcClient().getBot().sendIRCMessage(getChannel(), out);
            }

            // Each side pot contains the previous pot's total, so remove it
            totalpot = side.getPot();
            totalbet = side.getBet();

            // Increase pot number
            i++;
        }

        // Announce rake
        String out = PokerStrs.RakeTaken.replaceAll(
                "%rake", Integer.toString(totalrake));
        out = out.replaceAll("%id", Integer.toString(tableID));
        out = out.replaceAll("%hID", Integer.toString(handID));
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);

        startShowCards();

        doRake(totalrake);
    }

    /**
     * Returns the active players mapped and sorted by their hand value.
     * 
     * @param playerlist The list of players to compare
     * 
     * @return The active players mapped by their hand value (sorted).
     */
    private synchronized Map<HandValue, List<Player>>getRankedPlayers(
                                               final List<Player> playerlist) {
        Map<HandValue, List<Player>> winners = new TreeMap<HandValue, List<Player>>();
        for (Player player : playerlist) {
            // Create a hand with the community cards and the player's hole
            // cards.
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

            // Store the player together with other players with the same hand
            // value.
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
    private synchronized void playerWins(final Player player) {
        // Rake
        // None if only the blinds are in
        // Otherwise Minimum or RakePercent whichever is greater
        int rake = 0;
        if (pot > (bigBlind * 2)) {
            rake = PokerVars.MINRAKE;
            int perc = (int) Math.round(pot * PokerVars.RAKEPERCENT);
            if (perc < PokerVars.MINRAKE) {
                rake = PokerVars.MINRAKE;
            } else {
                rake = perc;
            }
        }
        pot = pot - rake;

        player.win(pot);

        // Add to DB
        try {
            DB.getInstance().setHandWinner(handID, player.getName(), pot);
        } catch (Exception e) {
            EventLog.log(e, "Table", "nextHand");
        }

        // Announce
        String out = PokerStrs.PlayerWins.replaceAll(
                "%hID", Integer.toString(handID));
        out = out.replaceAll("%who", player.getName());
        out = out.replaceAll("%id", Integer.toString(tableID));
        out = out.replaceAll("%amount", Integer.toString(pot));
        out = out.replaceAll("%total", Integer.toString(player.getChips()));
        out = out.replaceAll("%rake", Integer.toString(rake));
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);

        pot = 0;

        startShowCards();

        doRake(rake);
    }

    /**
     * Takes rake for each player.
     * 
     * @param rake the amount of total rake.
     */
    private void doRake(final int rake) {
        double indrake = ((double) rake) / jackpotPlayers.size();
        for (Player player : jackpotPlayers) {
            if (player.getBet() >= bigBlind) {

                if (Rake.checkJackpot(player.getBet())) {
                    List<String> plyrs = new ArrayList<String>();
                    plyrs.add(player.getName());
                    Rake.jackpotWon(profile, GamesType.POKER, plyrs,
                                    getIrcClient().getBot(), getChannel());
                }
            }
            Rake.getPokerRake(player.getName(), indrake, profile);
        }
    }

    /**
     * Schedules the wait for players timer.
     */
    private synchronized void scheduleWaitForPlayers() {
        waitForPlayersTimer = new Timer(true);
        waitForPlayersTimer.schedule(
                new TableTask(this, TableTask.WAITFORPLAYERS),
                PokerVars.PLAYERWAITSECS * Utils.MS_IN_SEC);
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
     * This method handles when a user didn't respond and is automatically
     * folded.
     */
    private synchronized void noActionReceived() {
        // Announce
        String out = PokerStrs.NoAction.replaceAll(
                "%hID", Integer.toString(handID));
        out = out.replaceAll("%actor", actor.getName());
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);

        // sit the player out
        playerSitsOut(actor);
    }

    /**
     * Posts the small blind.
     */
    private synchronized void postSmallBlind() {
        final int sb = bigBlind / 2;
        actor.postSmallBlind(sb);
        pot += sb;
        playerBets.put(actor, sb);

        String out = PokerStrs.SmallBlindPosted.replaceAll(
                "%sb", Integer.toString(sb));
        out = out.replaceAll("%player", actor.getName());
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);

        try {
            rotateActor();
        } catch (IllegalStateException e) {
            EventLog.fatal(e, "Table", "postSmallBlind");
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

        String out = PokerStrs.BigBlindPosted.replaceAll(
                "%bb", Integer.toString(bigBlind));
        out = out.replaceAll("%player", actor.getName());
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);

        try {
            rotateActor();
        } catch (IllegalStateException e) {
            EventLog.fatal(e, "Table", "postBigBlind");
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
                player.setCards(holeCards);
            } catch (IllegalArgumentException | IllegalStateException e) {
                EventLog.fatal(e, "Table", "dealHoleCards");
                System.exit(1);
            }
            String out = PokerStrs.HoleCardsDealtPlayer.replaceAll(
                    "%hID", Integer.toString(handID));
            out = out.replaceAll("%id", Integer.toString(tableID));
            out = out.replaceAll("%card1", holeCards.get(0).toIRCString());
            out = out.replaceAll("%card2", holeCards.get(1).toIRCString());
            getIrcClient().getBot().sendIRCMessage(player.getUser(), out);
        }
        getIrcClient().getBot().sendIRCMessage(
                getChannel(),
                PokerStrs.HoleCardsDealt.replaceAll(
                        "%hID", Integer.toString(handID)));
    }

    /**
     * Deals a number of community cards.
     * 
     * @param phase The name of the phase.
     * @param nocards The number of cards to deal.
     */
    private synchronized void dealCommunityCards(final String phase,
                                                 final int nocards) {
        String cardstr = "";
        for (int i = 0; i < nocards; i++) {
            Card card = deck.deal();
            try {
                card = deck.deal();
            } catch (IllegalArgumentException | IllegalStateException e) {
                EventLog.fatal(e, "Table", "dealCommunityCards");
                System.exit(1);
            }
            board.add(card);
            cardstr = cardstr + card.toIRCString() + "%n ";
        }
        phaseStrings.put(phase, cardstr);

        String boardout = createBoardOutput(phase);

        // Notify channel of the card(s) dealt
        String out = PokerStrs.CommunityDealt.replaceAll(
                "%hID", Integer.toString(handID));
        out = out.replaceAll("%round", phase);
        out = out.replaceAll("%cards", boardout);
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);

        // Notify each player of all their cards
        for (Player player : activePlayers) {
            Card[] cards = player.getCards();
            cardstr = " %n[";
            for (int i = 0; i < cards.length; i++) {
                if (cards[i] != null) {
                    cardstr = cardstr + cards[i].toIRCString() + "%n ";
                }
            }
            cardstr += "] " + boardout;

            out = PokerStrs.CommunityDealtPlayer.replaceAll(
                    "%hID", Integer.toString(handID));
            out = out.replaceAll("%id", Integer.toString(tableID));
            out = out.replaceAll("%round", phase);
            out = out.replaceAll("%cards", cardstr);
            getIrcClient().getBot().sendIRCMessage(player.getUser(), out);
        }

    }

    /**
     * Prepares the table for a new hand.
     */
    private synchronized void nextHand() {
        board.clear();
        bet = 0;
        pot = 0;
        currentRound = 0;
        handActive = false;
        List<Player> removelist = new ArrayList<Player>();
        playerBets = new HashMap<Player, Integer>();
        phaseStrings.clear();

        sidePots.clear();

        activePlayers.clear();
        jackpotPlayers.clear();
        for (Player player : players) {
            player.resetHand();
            // Remove players without chips
            if (player.isBroke()) {
                removelist.add(player);
            }
            playerBets.put(player, 0);

            // Only add non-sat out players to activePlayers
            if (!removelist.contains(player)) {
                activePlayers.add(player);
                jackpotPlayers.add(player);
            }
            try {
                DB.getInstance().addPokerTableCount(
                        player.getName(), tableID, profile, player.getChips());
            } catch (Exception e) {
                EventLog.log(e, "Table", "nextHand");
            }
        }

        // Sit all broke players out.
        for (Player player : removelist) {
            String out = PokerStrs.OutOfChips.replaceAll(
                    "%player", player.getName());
            out = out.replaceAll("%id", Integer.toString(tableID));
            getIrcClient().getBot().sendIRCMessage(getChannel(), out);
            playerSitsOut(player);
        }

        if (activePlayers.size() >= minPlayers) {
            handActive = true;
            deck.shuffle();

            dealerPosition = (dealerPosition + activePlayers.size() - 1)
                    % activePlayers.size();
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
                handID = DB.getInstance().getHandID();
            } catch (Exception e) {
                EventLog.log(e, "Table", "nextHand");
            }

            String out = PokerStrs.NewHandMessage.replaceAll(
                    "%hID", Integer.toString(handID));
            out = out.replaceAll("%dealer", players.get(dealerPosition)
                    .getName());
            out = out.replaceAll("%sb", players.get(actorPosition).getName());
            out = out.replaceAll("%bb", players.get(bbPosition).getName());

            getIrcClient().getBot().sendIRCMessage(getChannel(), out);

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
     * Shuts down this table when we have no active or sat out players.
     */
    private synchronized void closeTable() {
        tables.remove(tableID);
        getIrcClient().closeTable(this);
        this.interrupt();
    }

    /**
     * Set's the table channel's topic.
     */
    private void setTopic() {
        super.setTopic(formatTableInfo(PokerStrs.TableTopic));
    }

    /**
     * Voices/Devoices players/non-players when the bot joins the channel.
     * 
     * @param users who joined the channel
     */
    public final void joinedChannel(final User[] users) {
        EventLog.info(
                "Bot joined a new table, giving and taking user modes on the channel",
                "Table", "joinedChannel");
        List<Player> playerlist = new ArrayList<Player>(players);
        playerlist.addAll(satOutPlayers);

        for (int i = 0; i < users.length; i++) {
            for (Player player : playerlist) {
                if (player.getName().equalsIgnoreCase(users[i].getNick())) {
                    getChannel().send().voice(users[i]);
                    if (satOutPlayers.contains(player)) {
                        playerSitsDown(player);
                    }
                    break;
                }
            }

            // user didn't exist, devoice
            if (users[i].getNick().compareToIgnoreCase(
                    getIrcClient().getBot().getNick()) != 0) {
                getChannel().send().deOp(users[i]);
                getChannel().send().deVoice(users[i]);
            }
        }

        if (disconnected) {
            scheduleWaitForPlayers();
        }
    }

    /**
     * Used to cancel the currently running hand Generally called after a
     * disconnect.
     */
    public final synchronized void cancelHand() {
        for (Player player : players) {
            player.cancelBet();
        }

        if (waitForPlayersTimer != null) {
            waitForPlayersTimer.cancel();
        }
        if (actionTimer != null) {
            actionTimer.cancel();
        }
        if (startGameTimer != null) {
            startGameTimer.cancel();
        }

        disconnected = true;
    }

    /**
     * Calculates the players in each side pot and the amount of each.
     */
    private synchronized void calculateSidePots() {
        // Reset the sidepots
        sidePots.clear();

        // For each unique "all-in" bet amount, create a new sidepot
        List<Integer> spamounts = new ArrayList<Integer>();
        for (Player player : activePlayers) {
            if (player.isBroke()) {
                // Player is all in, do we have a side pot for this size?
                // If not, create one
                int totalbet = player.getTotalBet();
                if (!spamounts.contains(totalbet)) {
                    sidePots.add(new SidePot(totalbet));
                    spamounts.add(totalbet);
                }
            }
        }

        // For each player that has any chips in the pot,
        // add them or their chips to the sidePots
        for (SidePot sidepot : sidePots) {
            for (Entry<Player, Integer> entry : playerBets.entrySet()) {
                Integer pbet = entry.getValue();
                if (pbet >= sidepot.getBet()) {
                    sidepot.call(entry.getKey());
                } else {
                    sidepot.add(pbet);
                }
            }
        }

        // Order the pots (ascending)
        Collections.sort(sidePots);
    }

    /**
     * Finds a player object with a specific name.
     * 
     * @param name the player name
     * @return the player
     */
    private Player findPlayer(final String name) {
        Player found = null;
        // find the player
        for (Player plyr : players) {
            if (plyr.getName().equalsIgnoreCase(name)) {
                found = plyr;
                break;
            }
        }

        // check if they are sat out
        if (found == null) {
            for (Player plyr : satOutPlayers) {
                if (plyr.getName().equalsIgnoreCase(name)) {
                    found = plyr;
                    break;
                }
            }
        }

        return found;
    }
    
    /**
     * Finds a player object with a specific name.
     * 
     * @param name the player name
     * @return the player
     */
    private Player findPlayer(final User name) {
        Player found = null;
        // find the player
        for (Player plyr : players) {
            if (plyr.getUser().compareTo(name) == 0) {
                found = plyr;
                break;
            }
        }

        // check if they are sat out
        if (found == null) {
            for (Player plyr : satOutPlayers) {
                if (plyr.getUser().compareTo(name) == 0) {
                    found = plyr;
                    break;
                }
            }
        }

        return found;
    }

    /**
     * Creates the public output of cards for the board.
     * 
     * @param phase The current board
     * @return the cards output
     */
    private String createBoardOutput(final String phase) {
        String output = "";
        if (phase.compareTo(FLOPSTR) == 0) {
            output = phaseStrings.get(FLOPSTR);
        } else if (phase.compareTo(TURNSTR) == 0) {
            output = phaseStrings.get(FLOPSTR) + "| ";
            output += phaseStrings.get(TURNSTR);
        } else if (phase.compareTo(RIVERSTR) == 0) {
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
        String out = PokerStrs.StartShowCard.replaceAll(
                "%id", Integer.toString(tableID));
        out = out.replaceAll("%hID", Integer.toString(handID));
        out = out.replaceAll("%secs", Integer.toString(PokerVars.SHOWCARDSECS));
        getIrcClient().getBot().sendIRCMessage(getChannel(), out);
        showCardsTimer = new Timer(true);
        showCardsTimer.schedule(new TableTask(this, TableTask.SHOWCARDS),
                                PokerVars.SHOWCARDSECS * Utils.MS_IN_SEC);
    }
}
