/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.enums;

/**
 * An enumerate of the possible events the IRC/poker system cares about.
 * 
 * @author Jamie Reid
 */
public enum EventType {
    /** An IRC action event. */
    ACTION,

    /** An action when a user / the bot joins a channel. */
    JOIN,

    /** An event for when a user messages a channel / the bot. */
    MESSAGE,

    /** An event when someone changes nick on a channel. */
    NICKCHANGE,

    /** An event for when a user notices a channel / the bot. */
    NOTICE,

    /** An action when a user is opped in a channel, may be us. */
    OP,

    /** An action when a user / the parts joins a channel. */
    PART,
    
    /** An action when a user quits IRC. */
    QUIT,

    /** Action used by timers. */
    TIMER;
}
