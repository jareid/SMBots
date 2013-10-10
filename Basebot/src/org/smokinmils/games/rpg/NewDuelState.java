package org.smokinmils.games.rpg;

import java.sql.SQLException;

import org.pircbotx.User;
import org.smokinmils.bot.Utils;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.Bet;
import org.smokinmils.logging.EventLog;

/**
 * This class tracks the state of a NewDuel set of rounds.
 * 
 * @author cjc
 */
public class NewDuelState {

    
    /** toString() string representation of a game. */
    private static final String AS_STRING = "%p1(%1hp) vs %p2(%2hp)";
    
    /** var to return if no winner, because checkstyle mandates no magic numbers! */
    private static final int OVER_9000 = 9001;
    
    /** Player one. */
    private final User p1;
    /** Player two. */
    private User p2;
    
    /** temp initial hp. */
    private static final int INIT_HP = 100;
    
    /** Player 1 hp. */
    private double p1hp = INIT_HP;
    /** Player 2 hp. */
    private double p2hp = INIT_HP;
    
    /** The bet for this duel. */
    private Bet bet;
    
    /** The amount wagered. */
    private final double amount;
    
    /** The profile this duel is for. */
    private final ProfileType prof;
    
    /** The round number. */
    private int round = 1;
    
    /**
     * Constructor.
     * @param newp1 player 1
     * @param p the profile type
     * @param a the amount
     */
    public NewDuelState(final User newp1,
                        final ProfileType p,
                        final double a)  {
        p1 = newp1;
        
        prof = p;
        amount = a;
        
        try {
            bet = new Bet(p1.getNick(), prof, GamesType.DUEL, amount, "");
        } catch (SQLException e) {
           EventLog.log(e, "NewDuelState", "constructor");
        }
    }
    
    /**
     * Adds the new player, and starts the game.
     * @param newp2 player 2
     */
    public final void start(final User newp2) {
        p2 = newp2;
        
        // call the bet
        try {
           bet.call(p2.getNick(), prof);
        } catch (SQLException e) {
            EventLog.log(e, "NewDuelState", "start");
        }
       
    }
    
    /**
     * Ends a round.
     * @param p1damage the damage p1 does (and p2 takes)
     * @param p2damage the damage p2 does (and p1 takes)
     */
    public final void endRound(final double p1damage,
                         final double p2damage) {
        p1hp -= p2damage;
        p2hp -= p1damage;
        round++;
    }
    
    /** 
     * Gets the winner (if any) for this duel.
     * @return 0 for draw, 1 for p1, 2 for p2, >2 for none;
     */
    public final int getWinner() {
        int retVal = OVER_9000;
        try {
            Double win = amount; // eh?
            if (p1hp <= 0 && p2hp <= 0) {
                retVal = 0; // draw
                bet.draw(p2.getNick(), prof);
            } else if (p1hp <= 0 && p2hp > 0) {
                retVal = 2; // p2win
                bet.lose(p2.getNick(), prof, win);
            } else if (p1hp > 0 && p2hp <= 0) {
                retVal = 1; //p1win
                bet.win(win);
            }
            bet.close();
        } catch (Exception e) {
           EventLog.log(e, "NewDuelState", "getWinner"); 
        }
        return retVal;
    }
    
    /**
     * Checks if this is a duel between newp1 and newp2.
     * @param newp1 player 1
     * @param newp2 player 2
     * @return yay if its a fight between these two, nay otherwise
     */
    public final boolean contains(final User newp1,
                                final User newp2) {
        return ((newp1.equals(p1) && newp2.equals(p2))
             ||  (newp1.equals(p2) && newp2.equals(p1)));     
    }
    
    /**
     * Checks to see if this duel contains a user.
     * @param newp1 the user we are checking
     * @return yay if this user is part of this duel, nay otherwise
     */
    public final boolean contains(final User newp1) {
        return (newp1.equals(p1) || newp1.equals(p2));
    }
    
    /**
     * If we have two players or one who is waiting.
     * @return yay or nay
     */
    public final boolean hasPlayer2() {
        return !(p2 == null);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        String p1nick = "Nope";
        String p2nick = "Nope";
        if (p1 != null) {
            p1nick = p1.getNick();
        }
        if (p2 != null) {
            p2nick = p2.getNick();
        }
        String out = AS_STRING.replaceAll("%p1", p1nick);
        out = out.replaceAll("%1hp", Utils.chipsToString(p1hp));
        out = out.replaceAll("%p2", p2nick);
        out = out.replaceAll("%2hp",  Utils.chipsToString(p2hp));
        return out;
    }
    
    /**
     * Gets p1.
     * @return p1
     */
    public final User getP1() {
        return p1;
    }
    
    /**
     * Gets p2.
     * @return p2.
     */
    public final User getP2() {
        return p2;
    }
    
    /**
     * Gets profile type.
     * @return profile type
     */
    public final ProfileType getProfile() {
        return prof;
    }
    
    /**
     * Gets amount.
     * @return the amount
     */
    public final double getAmount() {
        return amount;
    }
    
    /**
     * Gets the current round number.
     * @return the round number
     */
    public final int getRound() {
        return round;
    }
    /**
     * Gets the HP for a user (assumes they are in this game, otherwise gives 0).
     * @param u the user in question
     * @return the hp for the user in question
     */
    public final double getHP(final User u) {
        double retVal = 0;
        if (u.equals(p1)) {
            retVal = p1hp;
        } else if (u.equals(p2)) {
            retVal = p2hp;
        }
        return retVal;
    }
    
    /**
     * Gets the HP for the enemy (assumes they are in this game, otherwise gives 0).
     * @param u the user in question
     * @return the hp for the users enemy
     */
    public final double getEnemyHP(final User u) {
        double retVal = 0;
        if (u.equals(p1)) {
            retVal = p2hp;
        } else if (u.equals(p2)) {
            retVal = p1hp;
        }
        return retVal;
    }
    
    /**
     * Given u, return the enemy.
     * @param u the user who's enemy we want
     * @return the enemy
     */
    public final User getEnemy(final User u) {
        User ret = null;
        if (u.equals(p1)) {
            ret = p2;
        } else {
            ret = p1;
        }
        return ret;
    }

    /**
     * Cancels the bet that we use in this here state.
     */
    public final void cancel() {
        if (bet != null) {
            try {
                bet.cancel();
            } catch (SQLException e) {
               EventLog.log(e, "NewDuelState", "cance");
            }
        }
        
    }
    
}
