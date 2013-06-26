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
 * An enumerate for the RPG skill types.
 * 
 * @author Jamie Reid
 */
public enum Skills {
    /** Strength skill. */
    Strength ("strength", "Higher damage in base combat"),
    
    /** Accuracy skill. */
    Accuracy ("accuracy", "Higher frequency of damage in base combat"),

    /** Magic skill. */
    Magic ("magic", "Good accompanying combat skill, and potion making ability"),

    /** Archery skill. */
    Archery ("archery", "Ability to deal extra blows in battle of increasing damage by level"),

    /** Speed skill. */
    Speed ("speed", "Increases frequency of archery blows as player can get distance" 
                    + "from enemy faster and use a bow"),
    

    /** Crafting skill. */
    Crafting ("craft", "For creating and repairing tools and weapons"),
    
    /** Enchanting skill. */
    Enchanting ("enchant", "For enchanting items, with higher levels enchanting better items"),
    
    /** Invalid. used to handle errors. */
    INVALID ("invalid", "INVALID");

    /** The text. */
    private final String text;
    
    /** The text. */
    private final String description;


    /**
     * Constructor.
     * @param txt textual representation.
     * @param desc a description.
     */
    Skills(final String txt, final String desc) {
        text = txt;
        description = desc;
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
     * @return the correct enumerate object
     */
    public static Skills fromString(final String text) {
        Skills ret = INVALID;
        if (text != null) {
            for (Skills gt : Skills.values()) {
                if (gt.getText().equals(text)) {
                    ret = gt;
                    break;
                }
            }
        }
        return ret;
    }
}
