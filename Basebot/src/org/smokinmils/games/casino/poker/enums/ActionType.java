/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.enums;

import org.smokinmils.settings.PokerStrs;

/**
 * An action enumerate.
 * 
 * @author Jamie Reid
 */
public enum ActionType {
    /** Small blind posted. */
    SMALL_BLIND("Small Blind", "posts the small blind", ""),

    /** Big blind posted. */
    BIG_BLIND("Big Blind", "posts the big blind", ""),

    /** Checks. */
    CHECK("Check", "checks", CommandType.CHECK.getFormat()),

    /** Call. */
    CALL("Call", "calls", CommandType.CHECK.getFormat()),

    /** First bet/raise. */
    BET("Bet", "bets", CommandType.RAISE.getFormat()),

    /** Raise the current bet. */
    RAISE("Raise", "raises", CommandType.RAISE.getFormat()),

    /** Fold. */
    FOLD("Fold", "folds", CommandType.FOLD.getFormat()),

    /** Continuing play. */
    CONTINUE("Continue", "continues", "");

    /** The name. */
    private final String name;

    /** The verb. */
    private final String text;

    /** The command format. */
    private final String format;

    /**
     * Constructor.
     * 
     * @param nme The name.
     * @param txt textual representation.
     * @param fmt The format.
     */
    ActionType(final String nme, final String txt, final String fmt) {
        name = nme;
        text = txt;
        format = fmt;
    }

    /**
     * Returns the name.
     * 
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a long textual form of this action.
     * 
     * @return The textual representation.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns a format to use this command.
     * 
     * @return The textual representation.
     */
    public String getFormat() {
        return format;
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Enum#toString()
     * @return the output string.
     */
    @Override
    public String toString() {
        String out = PokerStrs.AllowedActionString.replaceAll("%action", name);
        out = out.replaceAll("%cmd", format);
        return out;
    }
}
