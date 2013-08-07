package org.smokinmils.games.rpg;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;

/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */


/**
 * Provides the functionality for basic duelling.
 * 
 * @author Jamie
 */
public class Duel extends Event {
    /** The dm command. */
    public static final String  DM_CMD        = "!dm";

    /** The call command. */
    public static final String  TRAIN_CMD       = "!train";

    /**
     * Constructor.
     */
    public Duel() {
    }

    /**
     * This method handles the commands.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        
        if (isValidChannel(chan.getName())
                && bot.userIsIdentified(sender)) {
            if (Utils.startsWith(message, DM_CMD)) {
                newDM(event);
            } else if (Utils.startsWith(message, TRAIN_CMD)) {
                train(event);
            }
        }
    }

    /**
     * Let's the user train a certain skill.
     * 
     * Limited to twice per day.
     * 
     * @param event The message event.
     */
    private void train(final Message event) {
        
    }

    /**
     * Creates a new duel match.
     * 
     * @param event The message event.
     */
    private void newDM(final Message event) {
        
    }
}
