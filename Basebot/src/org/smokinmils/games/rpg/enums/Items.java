/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.rpg.enums;

/**
 * An enumerate for the RPG items.
 * 
 * @author Jamie Reid
 */
public enum Items {
    /** A shield. */
    Sheild ("shield", "A basic shield.", null),
    
    /** A sword. */
    Sword ("sword", "A basic sword.", "!sword"),
    
    /** Invalid. used to handle errors. */
    INVALID ("invalid", "INVALID", null);

    /** The text. */
    private final String text;
    
    /** The text. */
    private final String description;
    
    /** The command used to use the item. */
    private final String command;
    
    /** If the item is passive, i.e always active. */
    private final boolean passive;


    /**
     * Constructor.
     * @param txt textual representation.
     * @param desc a description.
     * @param cmd the command text.
     * @return 
     */
    Items(final String txt, final String desc, final String cmd) {
        text = txt;
        description = desc;
        command = cmd;
        if (cmd == null) {
            passive = true;
        } else {
            passive = false;
        }
    }

    /**
     * Returns a textual form of this skill.
     * 
     * @return The textual representation.
     */
    public String getText() {
        return text;
    }
    
    /**
     * Returns a long textual form of this skill.
     * 
     * @return The textual representation.
     */
    public String getDescription() {
        return description;
    }


    /**
     * (non-Javadoc).
     * @see java.lang.Enum#toString()
     * @return the output
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Converts a String to the correct GamesType.
     * 
     * @param text the string to check
     * 
     * @return the correct enumerate object
     */
    public static Items fromString(final String text) {
        Items ret = INVALID;
        if (text != null) {
            for (Items gt : Items.values()) {
                if (gt.getCommand().equals(text)) {
                    ret = gt;
                    break;
                }
            }
        }
        return ret;
    }

    /**
     * @return the passive
     */
    public boolean isPassive() {
        return passive;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }
}
