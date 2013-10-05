package org.smokinmils.games.tournaments;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a round of matches in a tournament.
 * @author cjc
 *
 */
public class Round {

    /** The matches in this round. */
    private List<Match> matches;
    
    /** The time in minutes left for the game. */
    private int timeleft;
    
    /**
     * Constructor.
     * @param timeout the time in minutes per round
     */
    public Round(final int timeout) {
        timeleft = timeout;
        matches = new ArrayList<Match>();
    }
    
    /**
     * Adds a new match to the round.
     * @param m the match to add
     */
    public final void addMatch(final Match m) {
        matches.add(m);
    }

    /**
     * Checks to see if a round is finished.
     * @return yay or nay
     */
    public final boolean isFinished() {
        boolean retBoo = true;
        for (Match m : matches) {
            if (!m.isDone()) {
                retBoo = false;
                break;
            }
        }
        return retBoo;
    }
    
    /**
     * Gets all the winners from the matches, assumes finished.
     * @return a List of winners!
     */
    public final List<String> getWinners() {
        List<String> retList = new ArrayList<String>();
        for (Match m : matches) {
            retList.add(m.getWinner());
        }
        return retList;
    }
 
    /**
     * Gets the losers for this round.
     * @return the losers
     */
    public final List<String> getLosers() {
        List<String> retList = new ArrayList<String>();
        for (Match m : matches) {
            retList.add(m.getLoser());
        }
        return retList;
    }
    
    /**
     * Gets the current matches in the round.
     * @return the matches
     */
    public final List<Match> getMatches() {
        return matches;
    }
    
    /**
     * Auto rolls all games if the time has run out!
     */
    public final void timeOut() {
        for (Match m : matches) {
            m.timeout();
        }
    }

    /**
     * Gets the time left and decrements it.
     * @return time left post-decrementation;
     */
    public final int getTimeLeft() {
        int retVal = timeleft;
        timeleft--;
        return retVal;
    }
}
