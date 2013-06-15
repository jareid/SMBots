/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.game.rooms;

import java.util.ArrayDeque;
import java.util.Deque;

import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.smokinmils.games.casino.poker.Client;
import org.smokinmils.games.casino.poker.enums.CommandType;
import org.smokinmils.games.casino.poker.enums.EventType;
import org.smokinmils.games.casino.poker.enums.RoomType;
import org.smokinmils.games.casino.poker.game.events.Event;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.PokerStrs;

/**
 * Provides the base class for the lobby and tables, provides the main run
 * method.
 * 
 * Implements Thread
 * 
 * @author Jamie Reid
 */
public class Room extends Thread {
    /** The IRC client. */
    private Client             ircClient;

    /** The channel this bot is running on. */
    private Channel            ircChannel;

    /** The type of Room this is. */
    private final RoomType     roomType;

    /** The topic for the IRC channel. */
    private String             roomTopic;

    /** A queue of events this room needs to process. */
    private final Deque<Event> eventQueue;

    /**
     * Constructor.
     * 
     * @param channel The channel this bot is running on
     * @param irc The IRC client
     * @param rt The type of Room this is
     */
    public Room(final Channel channel, final Client irc, final RoomType rt) {
        eventQueue = new ArrayDeque<Event>();

        setIrcClient(irc);
        setIrcChannel(channel);
        roomType = rt;
    }

    /**
     * Returns the room type.
     * 
     * @return The room type
     */
    public final RoomType getRoomType() {
        return roomType;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Thread#run()
     */
    @Override
    public final void run() {
        boolean interuptted = false;
        while (!(Thread.interrupted() || interuptted)) {
            if (!eventQueue.isEmpty()) {
                Event event = eventQueue.removeFirst();
                try {
                    switch (event.getType()) {
                    case ACTION:
                        this.onAction(
                                event.getSender(), event.getLogin(),
                                event.getHostname(),
                                event.getExtra());
                        break;
                    case JOIN:
                        this.onJoin(
                                event.getSender(), event.getLogin(),
                                event.getHostname());
                        break;
                    case MESSAGE:
                        this.onMessage(
                                event.getSender(), event.getLogin(),
                                event.getHostname(), event.getExtra());
                        break;
                    case NICKCHANGE:
                        this.onMessage(
                                event.getSender(), event.getLogin(),
                                event.getHostname(),
                                event.getExtra());
                        break;
                    case NOTICE:
                        this.onNotice(
                                event.getSender(), event.getLogin(),
                                event.getHostname(),
                                event.getExtra());
                        break;
                    case PART:
                        this.onPart(
                                event.getSender(), event.getLogin(),
                                event.getHostname());
                        break;
                    case OP:
                        this.onOp(
                                event.getSender(), event.getLogin(),
                                event.getHostname(),
                                event.getExtra());
                        break;
                    case TIMER:
                        this.onTimer(event.getSender());
                    default:
                        break;
                    }
                } catch (Exception e) {
                    getIrcClient().sendIRCMessage(
                            "Something caused the bot to "
                                    + "crash... please notify the staff.");
                    EventLog.fatal(e, "Room", "run");
                    System.exit(1);
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                interuptted = true;
            }
        }
        return;
    }

    /**
     * Adds an event to be handled by this Room's thread.
     * 
     * @param sender The nick of the person who caused the event.
     * @param login The login of the person who caused the event.
     * @param host The hostname of the person who caused the event.
     * @param extra The additional details for this event
     * @param type The type of event to add.
     */
    public final void addEvent(final String sender,
                               final String login,
                               final String host,
                               final String extra,
                               final EventType type) {
        eventQueue.addLast(new Event(sender, login, host, extra, type));
    }

    /**
     * Method to get this rooms IRC channel.
     * 
     * @return The channel for this event.
     */
    public final Channel getChannel() {
        return getIrcChannel();
    }

    /**
     * This method is called whenever a message is sent to this channel.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    protected void onMessage(final String sender,
                             final String login,
                             final String hostname,
                             final String message) {

    }

    /**
     * This method is called whenever an ACTION is sent from a user. E.g. such
     * events generated by typing "/me goes shopping" in most IRC clients.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param sender The nick of the user that sent the action.
     * @param login The login of the user that sent the action.
     * @param hostname The hostname of the user that sent the action.
     * @param action The action carried out by the user.
     */
    protected void onAction(final String sender,
                            final String login,
                            final String hostname,
                            final String action) {

    }

    /**
     * This method is called whenever we receive a notice to this channel.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param sourceNick The nick of the user that sent the notice.
     * @param sourceLogin The login of the user that sent the notice.
     * @param sourceHostname The hostname of the user that sent the notice.
     * @param notice The notice message.
     */
    protected void onNotice(final String sourceNick,
                            final String sourceLogin,
                            final String sourceHostname,
                            final String notice) {

    }

    /**
     * This method is called whenever someone joins a channel which we are on.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
    protected void onJoin(final String sender,
                          final String login,
                          final String hostname) {

    }

    /**
     * This method is called whenever someone parts this channel which we are
     * on. This is also the handler for whenever someone quits from the channel
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param sender The nick of the user who parted the channel.
     * @param login The login of the user who parted from the channel.
     * @param hostname The hostname of the user who parted from the channel.
     */
    protected void onPart(final String sender,
                          final String login,
                          final String hostname) {

    }

    /**
     * This method is called whenever someone changes nick on this channel.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The hostname of the user.
     * @param newNick The new nick.
     */
    protected void onNickChange(final String oldNick,
                                final String login,
                                final String hostname,
                                final String newNick) {

    }

    /**
     * Called when a user (possibly us) gets granted operator status for a
     * channel.
     * 
     * @param sourceNick The nick of the user that performed the mode change.
     * @param sourceLogin The login of the user that performed the mode change.
     * @param sourceHostname The hostname of the user that performed the mode
     *            change.
     * @param recipient The nick of the user that got 'opped'.
     */
    protected void onOp(final String sourceNick,
                        final String sourceLogin,
                        final String sourceHostname,
                        final String recipient) {

    }

    /**
     * Called when a user needs to send a message back to the room.
     * 
     * @param timerName The type of timer that requires attention.
     */
    protected void onTimer(final String timerName) {

    }

    /**
     * Sends the invalid argument message.
     * 
     * @param who The user to send to
     * @param format The command format
     */
    protected final void invalidArguments(final String who,
                                          final String format) {
        getIrcClient().sendIRCNotice(who, PokerStrs.InvalidArgs);
        getIrcClient().sendIRCNotice(who, format);
    }

    /**
     * Sends a command's format message.
     * 
     * @param who The user to send to
     * @param cmd The command
     * @param format The command format
     */
    protected final void sendFormat(final String who,
                                    final String cmd,
                                    final String format) {
        getIrcClient().sendIRCNotice(
                who, "%b%c04 " + cmd + "%c12 - Format:" + format);
    }

    /**
     * Sends a command's format followed by it's description.
     * 
     * @param who The user to send to
     * @param cmd The command
     */
    protected final void sendFullCommand(final String who,
                                         final CommandType cmd) {
        sendFormat(who, cmd.getCommandText(), cmd.getFormat());
        getIrcClient().sendIRCNotice(who, cmd.getDescription());
    }

    /**
     * This method is called whenever something changes in the channel and the
     * topic needs updating.
     * 
     * @param in the input string
     */
    protected final void setTopic(final String in) {
        String out = in.replaceAll("%c", "\u0003");
        out = out.replaceAll("%b", Colors.BOLD);
        out = out.replaceAll("%i", Colors.REVERSE);
        out = out.replaceAll("%u", Colors.UNDERLINE);
        out = out.replaceAll("%n", Colors.NORMAL);

        setRoomTopic(out);
        getIrcClient().getBot().setTopic(getIrcChannel(), getRoomTopic());
    }

    /**
     * @return the roomTopic
     */
    protected final String getRoomTopic() {
        return roomTopic;
    }

    /**
     * @param rt the roomTopic to set
     */
    protected final void setRoomTopic(final String rt) {
        roomTopic = rt;
    }

    /**
     * @return the ircChannel
     */
    protected final Channel getIrcChannel() {
        return ircChannel;
    }

    /**
     * @param channel the ircChannel to set
     */
    protected final void setIrcChannel(final Channel channel) {
        ircChannel = channel;
    }

    /**
     * @return the ircClient
     */
    protected final Client getIrcClient() {
        return ircClient;
    }

    /**
     * @param irc the ircClient to set
     */
    protected final void setIrcClient(final Client irc) {
        ircClient = irc;
    }
}
