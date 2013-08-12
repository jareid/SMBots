package org.smokinmils.games.rpg;

import java.util.HashMap;
import java.util.Map;

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
 * Provides the functionality for user stats such as level.
 * 
 * @author Jamie
 */
public class UserStats {
    /** Used in the experience calculation. */
    private static final int ADD_FORMULA = 3;

    /** Used in the experience calculation. */
    private static final int MULTIPLY_FORMULA = 4;

    /** The user this stats object is for. */
    private final String username;
    
    /** The user's level. */
    private int level;
    
    /** The user's experience. */
    private long experience;
    
    /** A map of the user's skills. */
    private final Map<Skills, Skill> skills;
    
    /**
     * Constructor.
     * @param user the user.
     */
    public UserStats(final String user) {
        username = user;
        level = 0;
        increaseExperience(0); 
        skills = new HashMap<Skills, Skill>();
        for (Skills skill: Skills.values()) {
            skills.put(skill, new Skill(user, skill));
        }
    }
    
    /**
     * @param skill the skill we want.
     * @return the user's skill object.
     */
    public final Skill getSkill(final Skills skill) {
        return skills.get(skill);
    }

    /**
     * @return the username
     */
    public final String getUsername() {
        return username;
    }

    /**
     * @return the level
     */
    public final int getLevel() {
        return level;
    }

    /**
     * @param amount the amount of exp gained.
     * @return the experience
     */
    public final long increaseExperience(final int amount) {
        experience += amount;
        // TODO: check for level ups && db functions here.
        // use (level_to_get*level_to_get+level_to_get+3)4 or
        //  base_xp * (level_to_get ^ factor)
        int nextlevel = level + 1;
        long levelup = (nextlevel * nextlevel + nextlevel + ADD_FORMULA) * MULTIPLY_FORMULA;
        if (experience >= levelup) {
            level++;
            // TODO: store to database
        }
        return experience;
    }
}

