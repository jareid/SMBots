package org.smokinmils.games.tournaments;

import org.pircbotx.Channel;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;

/**
 * A single match between two players.
 * @author cjc
 *
 */
public class Match {

    /** String when a user has rolled! */
    private static final String ROLLED = "%b%c12%player has rolled a %c04%roll%c12!";
    
    /** String when a game has finished. */
    private static final String DONE = "%b%c04%p1%c12 rolled a %c04%p1roll%c12 and %c04%p2%c12 "
            + "rolled a %c04%p2roll%c12, %c04%winner%c12 wins!";
    
    /** String of a game that is a regular game. */
    private static final String GAMESTR = "%b%c04%p1 %c12Vs %c04%p2";
    
    /** String when a bye game is finished. */
    private static final String BYEDONE = "%b%c04%winner%c12 had a bye and goes through to the "
            + "next round!";
    
    /** Sides on the die. */
    private static final Integer RANDOM = 6;

    /** String if there is one of those draws! */
    private static final String DRAW = "%b%c04%p1%c12 and %c04%p2%c12 drew! Roll again!";

    /** The String to represent player 1. */
    private String playerOne = "";
    
    /** The String to represent player 2. */
    private String playerTwo = "";
    
    /** playerOne's roll. */
    private int p1Roll = 0;
    
    /** playerTwo's roll. */ 
    private int p2Roll = 0;
    
    /** The String to represent the winner. */
    private String winner;
    
    /** Boolean that will tell us if the game is finished. */
    private boolean finished = false;
    
    /** If this is a bye. */
    private boolean isBye = false;
    
    /** The irc bot to send messages with. */
    private IrcBot ib;
    
    /** The channel that the match is taking place in. */
    private Channel channel;
    
    /**
     * Constructor for two players.
     * @param p1 player one
     * @param p2 player two
     * @param bot the bot to send messages with
     * @param chan the channel the match is taking place in
     */
    public Match(final String p1, final String p2, final IrcBot bot, final Channel chan) {
        playerOne = p1;
        playerTwo = p2;
        ib = bot;
        channel = chan;
    }
    
    /**
     * Constructor for a single player.
     * @param p1 player one
     * @param bot the bot to send messages with
     * @param chan the channel we are having this match in.
     */
    public Match(final String p1, final IrcBot bot, final Channel chan) {
        playerOne = p1;
        isBye = true;
        finished = true;
        winner = playerOne;
        ib = bot;
        channel = chan;
    }
    
    /**
     * Getter for player one.
     * @return player one
     */
    public final String getPlayerOne() {
        return playerOne;
    }
    
    /**
     * Getter for player two.
     * @return player two
     */
    public final String getPlayerTwo() {
        return playerTwo;
    }
    
    
    /**
     * Setter for player one.
     * @param player player name
     */
    public final void setPlayerOne(final String player) {
        playerOne = player;
    }
    
    /**
     * Setter for player two.
     * @param player player name
     */
    public final void setPlayerTwo(final String player) {
        playerTwo = player;
    }
    
    /**
     * Getter for the winner.
     * @return the winner's name
     */
    public final String getWinner() {
        return winner;
    }
    
    /**
     * Getter for the loser, assumes the match is finished!
     * @return the loser's name
     */
    public final String getLoser() {
        String retVal = "";
        if (winner.equalsIgnoreCase(playerOne)) {
            retVal = playerTwo;
        } else {
            retVal = playerOne;
        }
        return retVal;
    }
    
    /**
     * Tells us if the match is finished or not.
     * @return if we are finished or not
     */
    public final Boolean isDone() {
        return finished;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public final String toString() {
        String ret = "";
        if (isBye) {
            ret =  BYEDONE.replaceAll("%winner", playerOne);
        } else {
            ret =  GAMESTR.replaceAll("%p1", playerOne);
            ret = ret.replaceAll("%p2", playerTwo);
        }
        return ret;
    }
    
    /**
     * Checks if a certain user is part of this game.
     * @param user the user we are checking
     * @return true for if this is the user's game, false otherwise
     */
    public final boolean contains(final String user) {
        return (playerOne.equalsIgnoreCase(user) || playerTwo.equalsIgnoreCase(user));
    }

    /**
     * Checks if a player has rolled in this Match.
     * @param user The user who we want to know if rolled
     * @return true if they have rolled, false if they haven't (or are not part of this game)
     */
    public final boolean playerHasRolled(final String user) {
        boolean retBoo = false;
        if (playerOne.equalsIgnoreCase(user)) {
            retBoo = p1Roll != 0;
        } else if (playerTwo.equalsIgnoreCase(user)) {
            retBoo = p2Roll != 0;
        }
        return retBoo;
    }

    /**
     * Rolls for this user.
     * @param user the user to roll for
     */
    public final void roll(final String user) {
        if (!isBye) {
            int roll = (Random.nextInt(RANDOM) + 1) + (Random.nextInt(RANDOM) + 1);
            if (playerOne.equalsIgnoreCase(user)) {
                p1Roll = roll;
            } else if (playerTwo.equalsIgnoreCase(user)) {
                p2Roll = roll;
            }
            String out = ROLLED.replaceAll("%player", user);
            out = out.replaceAll("%roll", String.valueOf(roll));
            ib.sendIRCMessage(channel, out);
            
            // check if both have rolled, then annouce winner / put finished
            if (p1Roll != 0 && p2Roll != 0) {
                if (p1Roll == p2Roll) {
                    p1Roll = 0;
                    p2Roll = 0;
                    out = DRAW.replaceAll("%p1", playerOne);
                    out = out.replaceAll("%p2", playerTwo);
                    ib.sendIRCMessage(channel, out);
                } else {
                    if (p1Roll > p2Roll) { // p1 wins
                        winner = playerOne;
                    } else { // p2 wins
                        winner = playerTwo;
                        
                    }
                    finished = true;
                    
                    // send message to irc
                    out = DONE.replaceAll("%p1roll", String.valueOf(p1Roll));
                    out = out.replaceAll("%p2roll", String.valueOf(p2Roll));
                    out = out.replaceAll("%p1", playerOne);
                    out = out.replaceAll("%p2", playerTwo);
                    
                    out = out.replaceAll("%winner", winner);
                    ib.sendIRCMessage(channel, out);
                }
            }
        }
    }

    /**
     * Auto roll.
     */
    public final void timeout() {
        if (!playerHasRolled(playerOne)) {
            roll(playerOne);
        } 
        if (!playerHasRolled(playerTwo)) {
            roll(playerTwo);
        }
    }
}
