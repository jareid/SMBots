package org.smokinmils.games.rpg;

import org.smokinmils.games.rpg.enums.Skills;
/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */

/**
 * Provides the functionality for individual skills.
 * 
 * @author Jamie
 */
public class Skill {
    /** Base experience, used in the level up formula. */
    private static final int BASE_EXP = 200;

    /** The user this stats object is for. */
    private final String username;    

    /** The type of skill. */
    private final Skills type;

    /** The amount of experience the user has in this skill. */
    private long experience;
    
    /** The value the user has. */
    private int value;
    
    /**
     * Constructor.
     * @param user the user name.
     * @param skill the type of skill.
     */
    public Skill(final String user, final Skills skill) {
        username = user;
        type = skill;
        value = 0; // TODO: pull from database.
        experience = 0; // TODO: pull from database.
    }

    /**
     * @return the value
     */
    public final int getValue() {
        return value;
    }

    /**
     * @param amount the amount of exp gained.
     * @return the experience
     */
    public final long increaseExperience(final int amount) {
        experience += amount;
        // TODO: db functions here.
        // TODO: check for level ups.
        // use (level_to_get*level_to_get+level_to_get+3)4 or
        //  base_xp * (level_to_get ^ factor)
        long nextlevel = BASE_EXP * ((value + 1) ^ 2);
        if (experience >= nextlevel) {
            value++;
            // TODO: store to database
        }
        return experience;
    }

    /**
     * @return the username
     */
    public final String getUsername() {
        return username;
    }

    /**
     * @return the type
     */
    public final Skills getType() {
        return type;
    }
}

