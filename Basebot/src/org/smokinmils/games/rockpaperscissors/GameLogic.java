package org.smokinmils.games.rockpaperscissors;

import java.util.Comparator;

/**
 * Provides an enumerate for the RPS game that represents choices and decides
 * the winner.
 * 
 * @author Jamie
 */
public enum GameLogic {
    /** The rock choice. */
    ROCK("rock"),
    /** The paper choice. */
    PAPER("paper"),
    /** The scissors choice. */
    SCISSORS("scissors");

    /** The text. */
    private final String text;

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

/**
 * Provides a Comparator object for the RPS game.
 * 
 * @author Jamie
 */
class GameLogicComparator implements Comparator<GameLogic> {
    /** The resulting string. */
    private String              winstring      = null;

    /** Paper Vs Rock string. */
    private static final String PAPER_ROCK     = "Paper covers rock";

    /** Rock Vs Paper string. */
    private static final String ROCK_SCISSORS  = "Rock blunts scissors";

    /** Scissors Vs Paper string. */
    private static final String PAPER_SCISSORS = "Scissors cuts paper";

    /** Draw string. */
    private static final String DRAW           = "Draw";

    /**
     * Compares two GameLogic enumerate objects.
     * 
     * @param o1 first object
     * @param o2 second object
     * 
     * @return 0 if the objects draw, -1 if the left object is the winner +1 if
     *         the right object is the winner
     */
    @Override
    public int compare(final GameLogic o1,
                       final GameLogic o2) {
        Integer ret = 0;
        if (o1 == o2) {
            ret = 0;
            winstring = DRAW;
        } else if (o1 == GameLogic.ROCK) {
            if (o2 == GameLogic.PAPER) {
                ret = 1;
                winstring = PAPER_ROCK;
            } else if (o2 == GameLogic.SCISSORS) {
                ret = -1;
                winstring = ROCK_SCISSORS;
            } else {
                // Undefined state
                throw new IllegalStateException(
                        "o1 is an invalid GameLogic object");
            }
        } else if (o1 == GameLogic.PAPER) {
            if (o2 == GameLogic.ROCK) {
                ret = -1;
                winstring = PAPER_ROCK;
            } else if (o2 == GameLogic.SCISSORS) {
                ret = 1;
                winstring = PAPER_SCISSORS;
            } else {
                // Undefined state
                throw new IllegalStateException(
                        "o1 is an invalid GameLogic object");
            }
        } else if (o1 == GameLogic.SCISSORS) {
            if (o2 == GameLogic.ROCK) {
                ret = 1;
                winstring = ROCK_SCISSORS;
            } else if (o2 == GameLogic.PAPER) {
                ret = -1;
                winstring = PAPER_SCISSORS;
            } else {
                // Undefined state
                throw new IllegalStateException(
                        "o1 is an invalid GameLogic object");
            }
        } else {
            // Undefined state
            throw new IllegalStateException("o1 is an invalid "
                    + "GameLogic object");
        }
        return ret;
    }

    /**
     * @return the win string from this object.
     */
    public String getWinString() {
        return winstring;
    }
}
