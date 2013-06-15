/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.game.events;

import org.smokinmils.games.casino.poker.enums.EventType;

/**
 * The base class for the poker/irc events.
 * 
 * @author Jamie Reid
 */
public class Event {
    /** The nick of the person who caused the event. */
    private String    sender;

    /** The login/ident of the person who caused the event. */
    private String    login;

    /** The hostname of the person who caused the event. */
    private String    hostname;

    /** The additional details for this event. */
    private String    extra;

    /** The type of event. */
    private EventType type;

    /**
     * Constructor.
     * 
     * @param who The nick of the person who caused the event.
     * @param log The login of the person who caused the event.
     * @param host The hostname of the person who caused the event.
     * @param ext The additional details for this event
     * @param ev The type of event to add.
     */
    public Event(final String who, final String log,
            final String host, final String ext,
            final EventType ev) {
        setSender(who);
        setLogin(log);
        setHostname(host);
        setExtra(ext);
        setType(ev);
    }

    /**
     * @return the sender
     */
    public final String getSender() {
        return sender;
    }

    /**
     * @param who the sender to set
     */
    public final void setSender(final String who) {
        sender = who;
    }

    /**
     * @return the login
     */
    public final String getLogin() {
        return login;
    }

    /**
     * @param whologin the login to set
     */
    public final void setLogin(final String whologin) {
        login = whologin;
    }

    /**
     * @return the hostname
     */
    public final String getHostname() {
        return hostname;
    }

    /**
     * @param host the hostname to set
     */
    public final void setHostname(final String host) {
        hostname = host;
    }

    /**
     * @return the extra
     */
    public final String getExtra() {
        return extra;
    }

    /**
     * @param ext the extra to set
     */
    public final void setExtra(final String ext) {
        extra = ext;
    }

    /**
     * @return the type
     */
    public final EventType getType() {
        return type;
    }

    /**
     * @param ev the type to set
     */
    public final void setType(final EventType ev) {
        type = ev;
    }
}
