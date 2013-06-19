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
import org.pircbotx.User;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Message;
import org.smokinmils.bot.events.NickChange;
import org.smokinmils.bot.events.Notice;
import org.smokinmils.bot.events.Op;
import org.smokinmils.bot.events.Part;
import org.smokinmils.games.casino.poker.Client;
import org.smokinmils.games.casino.poker.enums.CommandType;
import org.smokinmils.games.casino.poker.enums.EventType;
import org.smokinmils.games.casino.poker.enums.RoomType;
import org.smokinmils.games.casino.poker.game.events.PokerEvent;
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
    private Channel            channel;

    /** The type of Room this is. */
    private final RoomType     roomType;

    /** The topic for the IRC channel. */
    private String             roomTopic;

    /** A queue of events this room needs to process. */
    private final Deque<PokerEvent> eventQueue;

    /**
     * Constructor.
     * 
     * @param chan The channel this bot is running on
     * @param irc The IRC client
     * @param rt The type of Room this is
     */
    public Room(final Channel chan, final Client irc, final RoomType rt) {
        eventQueue = new ArrayDeque<PokerEvent>();

        setIrcClient(irc);
        setChannel(chan);
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
                PokerEvent event = eventQueue.removeFirst();
                try {
                    switch (event.getType()) {
                    case JOIN:
                        if (event.getEvent() instanceof Join) {
                            Join jev = (Join) event.getEvent();
                            String botnick = jev.getBot().getNick();
                            String jnick = jev.getUser().getNick();
                            if (botnick.equalsIgnoreCase(jnick)
                                    && getChannel() == null) {
                                setChannel(jev.getChannel());
                            }
                            
                            onJoin((Join) event.getEvent());
                        } else {
                            EventLog.log("Received Join event with invalid "
                                         + "details", "Room", "run");
                        }
                        break;
                    case MESSAGE:
                        if (event.getEvent() instanceof Message) {
                            onMessage((Message) event.getEvent());
                        } else {
                            EventLog.log("Received Message event with invalid "
                                         + "details", "Room", "run");
                        }
                        break;
                    case NICKCHANGE:
                        if (event.getEvent() instanceof NickChange) {
                            onNickChange((NickChange) event.getEvent());
                        } else {
                            EventLog.log("Received NickChange event with "
                                         + "invalid details", "Room", "run");
                        }
                        break;
                    case NOTICE:
                        if (event.getEvent() instanceof Notice) {
                            onNotice((Notice) event.getEvent());
                        } else {
                            EventLog.log("Received Notice event with invalid "
                                         + "details", "Room", "run");
                        }
                        break;
                    case PART:
                        if (event.getEvent() instanceof Part) {
                            onPart((Part) event.getEvent());
                        } else {
                            EventLog.log("Received Part event with invalid "
                                         + "details", "Room", "run");
                        }
                        break;
                    case OP:
                        if (event.getEvent() instanceof Op) {
                            onOp((Op) event.getEvent());
                        } else {
                            EventLog.log("Received Op event with invalid "
                                         + "details", "Room", "run");
                        }
                        break;
                    case TIMER:
                        this.onTimer(event.getText());
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
     * @param event The details for this event
     * @param type The type of event to add.
     */
    public final void addEvent(final Object event,
                               final EventType type) {
        eventQueue.addLast(new PokerEvent(event, type));
    }
    
    /**
     * Adds an event to be handled by this Room's thread.
     * 
     * @param event The details for this event
     * @param type The type of event to add.
     */
    public final void addEvent(final String event,
                               final EventType type) {
        eventQueue.addLast(new PokerEvent(event, type));
    }

    /**
     * This method is called whenever a message is sent to this channel.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param event the message event.
     */
    protected void onMessage(final Message event) {

    }

    /**
     * This method is called whenever we receive a notice to this channel.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param event The notice message.
     */
    protected void onNotice(final Notice event) {

    }

    /**
     * This method is called whenever someone joins a channel which we are on.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param event The event
     */
    protected void onJoin(final Join event) {

    }

    /**
     * This method is called whenever someone parts this channel which we are
     * on. This is also the handler for whenever someone quits from the channel
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param event The event
     */
    protected void onPart(final Part event) {

    }

    /**
     * This method is called whenever someone changes nick on this channel.
     * <p>
     * The implementation of this method in the Room abstract class performs no
     * actions and may be overridden as required.
     * 
     * @param event The event
     */
    protected void onNickChange(final NickChange event) {

    }

    /**
     * Called when a user (possibly us) gets granted operator status for a
     * channel.
     * 
     * @param event The event
     */
    protected void onOp(final Op event) {

    }

    /**
     * Called when a user needs to send a message back to the room.
     * 
     * @param timername The type of timer that requires attention.
     */
    protected void onTimer(final String timername) {

    }

    /**
     * Sends the invalid argument message.
     * 
     * @param who The user to send to
     * @param format The command format
     */
    protected final void invalidArguments(final User who,
                                          final String format) {
        getIrcClient().getBot().sendIRCNotice(who, PokerStrs.InvalidArgs);
        getIrcClient().getBot().sendIRCNotice(who, format);
    }

    /**
     * Sends a command's format message.
     * 
     * @param who The user to send to
     * @param cmd The command
     * @param format The command format
     */
    protected final void sendFormat(final User who,
                                    final String cmd,
                                    final String format) {
        getIrcClient().getBot().sendIRCNotice(who, 
                                   "%b%c04 " + cmd + "%c12 - Format:" + format);
    }

    /**
     * Sends a command's format followed by it's description.
     * 
     * @param who The user to send to
     * @param cmd The command
     */
    protected final void sendFullCommand(final User who,
                                         final CommandType cmd) {
        sendFormat(who, cmd.getCommandText(), cmd.getFormat());
        getIrcClient().getBot().sendIRCNotice(who, cmd.getDescription());
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
        getChannel().send().setTopic(getRoomTopic());
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
    public final Channel getChannel() {
        return channel;
    }

    /**
     * @param chan the ircChannel to set
     */
    protected final void setChannel(final Channel chan) {
        channel = chan;
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
