/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.game;

import java.util.List;
import java.util.Timer;

import org.pircbotx.User;
import org.smokinmils.bot.Utils;
import org.smokinmils.games.casino.cards.Card;
import org.smokinmils.games.casino.poker.enums.ActionType;
import org.smokinmils.games.casino.poker.game.rooms.Table;
import org.smokinmils.games.casino.poker.tasks.SittingOut;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.PokerVars;

/**
 * The player for the poker game.
 * 
 * @author Jamie Reid
 */
public class Player {    
    /** User. */
    private final User user;

    /** Players hand. */
    private final Hand   hand;

    /** Current chips. */
    private int          chips;

    /** Rebought chips. */
    private int          rebuy;

    /** Current bet. */
    private int          bet;

    /** Current bet (in this hand). */
    private int          totalBet;

    /** Number of bets and raises in the current betting round. */
    private int          raises;

    /** Last action performed. */
    private ActionType   action;

    /** Last action's bet increment. */
    private int          betIncrement;

    /** Whether the player has his hole cards being dealt. */
    private boolean      hasCards;

    /** Whether the player is sat down or not. */
    private boolean      sittingOut;

    /** Timer used when a player sits out. */
    private Timer        sittingOutTimer;

    /**
     * Constructor.
     * 
     * @param usr The player's user object.
     * @param buyin The player's buy in.
     */
    public Player(final User usr, final int buyin) {
        user = usr;
        chips = buyin;
        hand = new Hand();
        sittingOut = false;
    }
    
    /**
     * @return the user
     */
    public final User getUser() {
        return user;
    }

    /**
     * Returns the player's name.
     * 
     * @return The name.
     */
    public final String getName() {
        return getUser().getNick();
    }

    /**
     * Returns the player's current chip count.
     * 
     * @return The chip count
     */
    public final int getChips() {
        return chips;
    }

    /**
     * Returns the player's current amount of rebought chips.
     * 
     * @return The chip count
     */
    public final int getRebuy() {
        return rebuy;
    }

    /**
     * Adds chips to the user's total.
     * 
     * @param amount The chip count
     */
    public final void rebuy(final int amount) {
        rebuy += amount;
    }

    /**
     * Removes all chips.
     */
    public final void cashOut() {
        chips = 0;
        rebuy = 0;
    }

    /**
     * Prepares the player for another hand.
     */
    public final void resetHand() {
        chips += rebuy;
        totalBet = 0;
        rebuy = 0;
        hand.removeAllCards();
        hasCards = false;
        resetBet();
    }

    /**
     * Resets the player's bet.
     */
    public final void resetBet() {
        bet = 0;
        action = null;
        raises = 0;
        betIncrement = 0;
    }

    /**
     * Resets the player's bet.
     */
    public final void cancelBet() {
        chips += bet;
        bet = 0;
        action = null;
        raises = 0;
        betIncrement = 0;
    }

    /**
     * Returns the player's hand of cards.
     * 
     * @return The hand of cards.
     */
    public final Hand getHand() {
        return hand;
    }

    /**
     * Returns the player's hole cards.
     * 
     * @return The hole cards.
     */
    public final Card[] getCards() {
        return hand.getCards();
    }

    /**
     * Sets the hole cards.
     * 
     * @param cards the cards.
     */
    public final void setCards(final List<Card> cards) {
        hand.removeAllCards();
        if (cards == null) {
            hasCards = false;
        } else {
            if (cards.size() == 2) {
                try {
                    hand.addCards(cards);
                } catch (IllegalArgumentException e) {
                    EventLog.fatal(e, "Player", "setCards");
                    System.exit(1);
                }
                hasCards = true;
            } else {
                throw new IllegalArgumentException("Invalid number of cards");
            }
        }
    }

    /**
     * Returns whether the player is out of chips.
     * 
     * @return True if the player is out of chips
     */
    public final boolean isBroke() {
        return (chips <= 0);
    }

    /**
     * Returns whether the player sitting out.
     * 
     * @return True if the player sitting out
     */
    public final boolean isSatOut() {
        return sittingOut;
    }

    /**
     * Returns the player's current bet.
     * 
     * @return The current bet.
     */
    public final int getBet() {
        return bet;
    }

    /**
     * Returns the player's current bet for this hand.
     * 
     * @return The current bet.
     */
    public final int getTotalBet() {
        return totalBet;
    }

    /**
     * Returns the number of raises the player has performed in this betting
     * round.
     * 
     * @return The number of raises.
     */
    public final int getRaises() {
        return raises;
    }

    /**
     * Returns whether the player has his hole cards dealt.
     * 
     * @return True if the hole cards are dealt
     */
    public final boolean hasCards() {
        return hasCards;
    }

    /**
     * Returns the player's action.
     * 
     * @return the action
     */
    public final ActionType getAction() {
        return action;
    }

    /**
     * Returns the bet increment of the last action.
     * 
     * @return The bet increment.
     */
    public final int getBetIncrement() {
        return betIncrement;
    }

    /**
     * Posts the small blind.
     * 
     * @param blind The small blind.
     */
    public final void postSmallBlind(final int blind) {
        action = ActionType.SMALL_BLIND;
        chips -= blind;
        bet += blind;
        totalBet += blind;
    }

    /**
     * Posts the big blinds.
     * 
     * @param blind The big blind.
     */
    public final void postBigBlind(final int blind) {
        action = ActionType.BIG_BLIND;
        chips -= blind;
        bet += blind;
        totalBet += blind;
    }

    /**
     * Player has won the pot.
     * 
     * @param pot The pot.
     */
    public final void win(final int pot) {
        chips += pot;
    }

    /**
     * Receives and handles the action from the player
     * 
     * Determining the player's action is handled by the GamePlay task.
     * 
     * @param act The action.
     * @param minbet The minimum bet.
     * @param currentbet The current bet.
     * 
     * @return The selected action.
     */
    public final ActionType act(final ActionType act,
                                final int minbet,
                                final int currentbet) {
        switch (act) {
        case CHECK:
            break;
        case CALL:
            betIncrement = currentbet - bet;
            if (betIncrement > chips) {
                betIncrement = chips;
            }
            chips -= betIncrement;
            bet += betIncrement;
            totalBet += betIncrement;
            break;
        case BET:
            betIncrement += minbet;
            if (betIncrement >= chips) {
                betIncrement = chips;
            }
            chips -= betIncrement;
            bet += betIncrement;
            totalBet += betIncrement;
            raises++;
            break;
        case RAISE:
            // currentBet += minBet;
            betIncrement = currentbet - bet;
            if (betIncrement > chips) {
                betIncrement = chips;
            }
            chips -= betIncrement;
            bet += betIncrement;
            totalBet += betIncrement;
            raises++;
            break;
        case FOLD:
            hand.removeAllCards();
            break;
        default:
            throw new IllegalArgumentException(
                    "Blind actions are not valid at this point");
        }
        return act;
    }

    /**
     * The timer to monitor how long a player has sat out for.
     * 
     * @return the timer
     */
    public final Timer getSittingOutTimer() {
        return sittingOutTimer;
    }

    /**
     * Changes the sat out status of a player.
     * 
     * @param satout true if the player should be marked as sat out
     * @return
     */
    public final void setSatOut(final boolean satout) {
        sittingOut = satout;
    }

    /**
     * Schedules a player to be removed from the tbale after sitting out.
     * 
     * @param table the table.
     */
    public final void scheduleSitOut(final Table table) {
        sittingOutTimer = new Timer(true);
        sittingOutTimer.schedule(
                new SittingOut(table, this),
                PokerVars.SITOUTMINS * Utils.MS_IN_MIN);
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Object#toString()
     * @return the string
     */
    @Override
    public final String toString() {
        return getUser().getNick();
    }
}
