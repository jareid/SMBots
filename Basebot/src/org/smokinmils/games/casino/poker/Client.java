/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.BaseBot;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Action;
import org.smokinmils.bot.events.Invite;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.NickChange;
import org.smokinmils.bot.events.Notice;
import org.smokinmils.bot.events.Op;
import org.smokinmils.bot.events.Part;
import org.smokinmils.bot.events.Quit;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.poker.enums.EventType;
import org.smokinmils.games.casino.poker.game.rooms.Lobby;
import org.smokinmils.games.casino.poker.game.rooms.Room;
import org.smokinmils.games.casino.poker.game.rooms.Table;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.PokerStrs;
import org.smokinmils.settings.PokerVars;

/**
 * An IRC client based on PircBot that provides a poker playing system
 * 
 * Extends the PircBot class. This class is used to pull in IRC functionality
 * 
 * @author Jamie Reid
 */
public class Client extends Event {
    /** The name of the server this for. */
    private final String               serverName;

    /** The bot object for this server. */
    private final IrcBot               bot;

    /** A mapping of channel to Room objects. */
    private final Map<String, Room>    pokerValidChannels;

    /** A mapping of table ID to table channel. */
    private final Map<Integer, String> validTables;

    /** Main channel for the poker bot. */
    private final String               lobbyChan;

    /**
     * Constructor.
     * 
     * @param server The server name.
     * @param lobby The lobby channel.
     */
    public Client(final String server, final String lobby) {
        serverName = server;
        bot = BaseBot.getInstance().getBot(serverName);
        pokerValidChannels = new HashMap<String, Room>();
        validTables = new HashMap<Integer, String>();
        lobbyChan = lobby;
    }

    /**
     * Initialise the poker lobby.
     */
    public final void initialise() {
        // At a minimum, we should exist in a lobby
        if (pokerValidChannels.isEmpty()) {
            bot.sendIRC().joinChannel(lobbyChan);
            Channel lchan = bot.getUserChannelDao().getChannel(lobbyChan);
            Lobby lobby = new Lobby(lchan, this);
            lobby.start();
            pokerValidChannels.put(lobbyChan.toLowerCase(), lobby);
        }
        
        // (Re-)Check the user's status with NickServ
        if (validTables.size() == 0) {
            ProfileType[] profiles = ProfileType.values();
            for (Integer x : PokerVars.INITBB) {
                for (Integer y : PokerVars.INITTBLSIZES) {
                    for (int z = 0; z < profiles.length; z++) {
                        newTable(x, y, profiles[z], false);
                    }
                }
            }
        }

        // Request invites from Chanserv and attempt to join all channels
        for (Entry<String, Room> entry : pokerValidChannels.entrySet()) {
            bot.sendRaw().rawLine("ChanServ INVITE " + entry.getKey());
            bot.sendIRC().joinChannel(entry.getKey());
        }
    }

    /**
     * Ensures that the bot will join channels when invited by ChanServ.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onInvite(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)
     * @param event the invite event.
     */
    @Override
    public final void invite(final Invite event) {
        String chan = event.getChannel();
        if (pokerValidChannels.containsKey(chan)) {
            event.getBot().sendIRC().joinChannel(chan);
        }
    }

    /**
     * Keep a track of new users joining so we can: - check if they are
     * identified - send the event to the channel's (room's) event system.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onJoin(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String)
     * @param event The Join event.
     */
    @Override
    public final void join(final Join event) {
        String channel = event.getChannel().getName();
        String joinee = event.getUser().getNick();
        // Notify the correct room if required
        if (!joinee.equalsIgnoreCase(event.getBot().getNick())) {
            Room room = pokerValidChannels.get(channel.toLowerCase());
            if (room != null) {
                room.addEvent(event, EventType.JOIN);
            }
        }
    }

    /**
     * Send the part to the correct Room's events system.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onPart(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String)
     * @param event The Part event.
     */
    @Override
    public final void part(final Part event) {
        // Notify the correct room if required
        Room room = pokerValidChannels.get(event.getChannel().getName().toLowerCase());
        if (room != null) {
            room.addEvent(event, EventType.PART);
        }
    }

    /**
     * Send the quit as a part event to all the Rooms this user is in.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onQuit(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String)
     * @param event The Quit event.
     */
    @Override
    public final void quit(final Quit event) {
        String nick = event.getUser().getNick();
        if (nick.compareToIgnoreCase(event.getBot().getNick()) != 0) {
            // Notify the correct room if required
            for (Room room : pokerValidChannels.values()) {
                room.addEvent(event, EventType.QUIT);
            }
        }
    }

    /**
     * Send the quit as a nick change event to all the Rooms this user is in.
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onNickChange(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String)
     * @param event The nick change event.
     */
    @Override
    public final void nickChange(final NickChange event) {
        // Notify the correct rooms
        for (Entry<String, Room> room : pokerValidChannels.entrySet()) {
            room.getValue().addEvent(event, EventType.NICKCHANGE);
        }
    }

    /**
     * We joined a channel so beginning checking users' status.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onUserList(java.lang.String,
     *      org.jibble.pircbot.User[])
     * @param channel the Channel the list is for
     * @param users the list of users.
     */
    protected final void onUserList(final Channel channel,
                              final User[] users) {
        String chan = channel.getName().toLowerCase();
        if (validTables.containsValue(chan)) {
            Table table = (Table) pokerValidChannels.get(chan);
            table.joinedChannel(users);
        }
    }

    /**
     * Pass the message to the correct Room.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onMessage(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)
     * @param event The Message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot irc = event.getBot();
        String message = event.getMessage();
        User user = event.getUser();
        Channel chan = event.getChannel();

        // Get the first character and check if it is a command
        char fChar = message.charAt(0);
        if (fChar == PokerStrs.CommandChar) {
            if (irc.userIsIdentified(user)) {
                // Notify the correct room if required
                Room room = pokerValidChannels
                        .get(chan.getName().toLowerCase());
                if (room != null) {
                    room.addEvent(event, EventType.MESSAGE);
                }
            }
        }
    }

    /**
     * Pass the action to the correct Room.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onMessage(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)     *      
     * @param event The Action event.
     */
    @Override
    public final void action(final Action event) {
        // Notify the correct room if required
        Room room = pokerValidChannels.get(event.getChannel().getName()
                .toLowerCase());
        if (room != null) {
            room.addEvent(event, EventType.ACTION);
        }
    }

    /**
     * Processes NickServ responses. Currently uses - STATUS Passes onto the
     * correct room as an event.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onNotice(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)
     * @param event The notice event.
     */
    @Override
    public final void notice(final Notice event) {
        // Notify the correct room if required
        Room room = pokerValidChannels.get(event.getChannel());
        if (room != null) {
            room.addEvent(event, EventType.NOTICE);
        }
    }

    /**
     * Processes Op events Passes onto the correct room as an event.
     * 
     * (non-Javadoc)
     * @see org.jibble.pircbot.PircBot#onNotice(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)
     * @param event the op event.
     */
    @Override
    public final void op(final Op event) {
        // Notify the correct room if required
        Room room = pokerValidChannels.get(event.getChannel().getName()
                .toLowerCase());
        if (room != null) {
            room.addEvent(event, EventType.OP);
        }
    }

    /**
     * Returns the bot for this lobby.
     * 
     * @return the bot object
     */
    public final IrcBot getBot() {
        return bot;
    }

    /**
     * Creates a new poker table.
     * 
     * @param stake the big blind for this table
     * @param players the maximum number of players for this table
     * @param profile the profile used on the table
     * @param manual if this was created through a command.
     * 
     * @return the table id.
     */
    public final int newTable(final int stake,
                        final int players,
                        final ProfileType profile,
                        final boolean manual) {
        EventLog.info("Creating new table...", "Client", "newTable");

        Integer tableid = Table.getNextID();
        String chan = PokerVars.TABLECHAN + tableid;
        bot.sendIRC().joinChannel(chan);
        Channel achan = bot.getUserChannelDao().getChannel(chan);
        Table table = new Table(achan, this, tableid, stake,
                                players, profile, manual);
        pokerValidChannels.put(chan.toLowerCase(), table);
        validTables.put(tableid, chan.toLowerCase());
        table.start();

        if (manual) {
            sendIRCMessage(table.formatTableInfo(PokerStrs.NewTable));
        }

        return tableid;
    }

    /**
     * Gets the Table with a specified ID.
     * 
     * @param id the table id
     * 
     * @return the table that matches or null
     */
    public final Table getTable(final int id) {
        String chan = validTables.get(id).toLowerCase();
        return ((Table) pokerValidChannels.get(chan));
    }

    /**
     * Creates a new player on a table.
     * 
     * @param sender The user
     * @param id The table ID
     * @param buyin The initial buy in
     */
    public final void newPlayer(final User sender,
                          final int id,
                          final Integer buyin) {
        EventLog.info("Adding new player...", "Client", "newTable");
        // Get channel for the table id
        String chan = validTables.get(id);
        if (chan != null) {
            // Add player to table
            Table tbl = (Table) pokerValidChannels.get(chan.toLowerCase());
            if (tbl != null) {
                tbl.playerJoins(sender, buyin);
            } else {
                EventLog.log(sender + "tried to join "
                        + Integer.toString(id)
                        + "but couldn't find the table",
                        "Client", "newPlayer");
                sendIRCMessage("Something went wrong when the bot tried to add"
                                    + " you to the table, please inform staff");
            }
        } else {
            EventLog.log(sender + "tried to join "
                    + Integer.toString(id)
                    + "but couldn't find the table's channel",
                    "Client", "newPLayer");
            sendIRCMessage("Something went wrong when the bot tried to add"
                                    + " you to the table, please inform staff");
        }
    }

    /**
     * Allows a new user to join the channel to watch the game play.
     * 
     * @param sender The user
     * @param id The table ID
     */
    public final void newObserver(final User sender,
                                  final int id) {
        // Get channel for the table id
        String chan = validTables.get(id);
        if (chan != null) {
            // Invite the player to join channel
            bot.sendIRC().invite(sender.getNick(), chan);
        } else {
            EventLog.log(sender.getNick() + "tried to join "
                                + Integer.toString(id)
                                + "but couldn't find the table's channel",
                         "Client", "newPLayer");
            
            sendIRCMessage("Something went wrong when the bot tried to add"
                                + " you to the table, please inform staff");
        }
    }

    /**
     * Used to check whether a table is full.
     * 
     * @param id The ID of the table to be checked
     * 
     * @return boolean true if the table has no seats left
     */
    public final boolean tableIsFull(final int id) {
        boolean full = false;
        // Get table for the table id
        String chan = validTables.get(id);
        Table tbl = (Table) pokerValidChannels.get(chan);
        if (tbl != null) {
            full = tbl.isFull();
        }
        return full;
    }

    /**
     * Used to send a message to the target replacing formatting variables
     * correctly Also allows the sending of multiple lines separate by \n
     * character.
     * 
     * Sends to the lobby
     * 
     * @param in The message to send with formatting variables
     */
    public final void sendIRCMessage(final String in) {
        bot.sendIRCMessage(bot.getUserChannelDao().getChannel(lobbyChan), in);
    }

    /**
     * Method to shut down a table when all players leave.
     * 
     * @param table The table
     */
    public final void closeTable(final Table table) {
        int found = -1;
        // leave the channel
        table.getChannel().send().part("No players");

        // Remove from valid tables
        String tblchan = table.getChannel().getName();
        pokerValidChannels.remove(tblchan);
        for (Entry<Integer, String> tbl : validTables.entrySet()) {
            if (tblchan.compareToIgnoreCase(tbl.getValue()) == 0) {
                found = tbl.getKey();
                break;
            }
        }

        if (found != -1) {
            sendIRCMessage(PokerStrs.TableClosed.replaceAll(
                                               "%id", Integer.toString(found)));
            EventLog.info("Table " + Integer.toString(found) + " closed",
                          "Client", "closeTable");
            validTables.remove(found);
        } else {
            EventLog.log(
                    "Tried to close " + tblchan + " but not ID found",
                    "Client", "closeTable");
        }
    }
}
