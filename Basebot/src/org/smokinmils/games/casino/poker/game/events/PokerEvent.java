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
public class PokerEvent {
    /** The details for this event. */
    private Object event;

    /** The type of event. */
    private String text;
    
    /** The type of event. */
    private EventType type;

    /**
     * Constructor.
     *
     * @param ev The event details
     * @param et The type of event to add.
     */
    public PokerEvent(final Object ev,
                      final EventType et) {
        setEvent(ev);
        setType(et);
    }

    /**
     * Constructor.
     *
     * @param ev The event details
     * @param et The type of event to add.
     */
    public PokerEvent(final String ev,
                      final EventType et) {
        setText(ev);
        setType(et);
    }
    
    /**
     * @return the event
     */
    public final Object getEvent() {
        return event;
    }

    /**
     * @param ev the extra to set
     */
    private void setEvent(final Object ev) {
        event = ev;
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
    private void setType(final EventType ev) {
        type = ev;
    }

    /**
     * @return the text
     */
    public final String getText() {
        return text;
    }

    /**
     * @param txt the text to set
     */
    private void setText(final String txt) {
        text = txt;
    }
}
