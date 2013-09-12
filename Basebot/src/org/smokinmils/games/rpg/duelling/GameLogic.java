package org.smokinmils.games.rpg.duelling;

import java.util.Comparator;

/**
 * Provides an enumerate for the NewDuel game that represents choices and decides
 * the winner.
 * 
 * @author cjc
 */
public enum GameLogic {
    /** The uppercut choice. */
    UPPERCUT("uppercut"),
    /** The jab choice. */
    JAB("jab"),
    /** The block choice. */
    BLOCK("block");

    /** The text. */
    private final String text;

    private 
    /**
     * Constructor.
     * 
     * @param txt textual representation.
     */
    GameLogic(final String txt) {
        text = txt;
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
     * (non-Javadoc).
     * @see java.lang.Enum#toString()
     * @return the string object.
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * Converts a String to the correct GameLogic.
     * 
     * @param text the string to check
     * @return the correct enumerate object
     */
    public static GameLogic fromString(final String text) {
        GameLogic ret = null;
        if (text != null) {
            for (GameLogic gt : GameLogic.values()) {
                if (gt.getText().equalsIgnoreCase(text)) {
                    ret = gt;
                }
            }
        }
        return ret;
    }
}
