package org.smokinmils.games.rockpaperscissors;

import java.util.Comparator;

public enum GameLogic {
	ROCK("rock"),
	PAPER("paper"),
	SCISSORS("scissors");
	
	
	/** The text. */
	private final String text;
	
	/**
	 * Constructor.
	 * 
	 * @param name  The name.
	 * @param text  textual representation.
	 */
	GameLogic(String text) {
		this.text = text;
	}
	
	/**
	 * Returns a long textual form of this action.
	 * 
	 * @return The textual representation.
	 */
	public String getText() { return text; }
	
	/**
	 * (non-Javadoc)
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() { return text;	}
	
	/** 
	 * Converts a String to the correct GameLogic
	 * 
	 * @param text the string to check
	 * @return the correct enumerate object
	 */
    public static GameLogic fromString(String text) {
    	GameLogic ret = null;
        if (text != null) {
        	for (GameLogic gt : GameLogic.values()) {
        		if ( gt.getText().equalsIgnoreCase(text) ) {
        			ret = gt;
        		}
        	}
        }
        return ret;
    }
}

class GameLogicComparator implements Comparator<GameLogic> {
	private String WinString = null;
	private static final String PAPER_ROCK = "Paper covers rock";
	private static final String ROCK_SCISSORS = "Rock blunts scissors";
	private static final String PAPER_SCISSORS = "Scissors cuts paper";
	private static final String DRAW = "Draw";
	
	/**
	 * Compares two GameLogic enumerate objects
	 * 
	 * @param o1 first object
	 * @param o2 second object
	 * 
	 * @return 0 if the objects draw, -1 if the left object is the winner
	 * 		   +1 if the right object is the winner
	 */
    public int compare(GameLogic o1, GameLogic o2) throws IllegalStateException {
    	Integer ret = 0;
    	if (o1 == o2) {
    		ret = 0;
			WinString = DRAW;
    	} else if (o1 == GameLogic.ROCK) {
    		if (o2 == GameLogic.PAPER) {
    			ret = 1;
    			WinString = PAPER_ROCK;
    		} else if (o2 == GameLogic.SCISSORS) {
    			ret = -1;
    			WinString = ROCK_SCISSORS;
    		} else {
        		// Undefined state
        		throw new IllegalStateException("o1 is an invalid GameLogic object");
    		}
    	} else if (o1 == GameLogic.PAPER) {
    		if (o2 == GameLogic.ROCK) {
    			ret = -1;
    			WinString = PAPER_ROCK;
    		} else if (o2 == GameLogic.SCISSORS) {
    			ret = 1;
    			WinString = PAPER_SCISSORS;
    		} else {
        		// Undefined state
        		throw new IllegalStateException("o1 is an invalid GameLogic object");
    		}
    	} else if (o1 == GameLogic.SCISSORS) {
    		if (o2 == GameLogic.ROCK) {
    			ret = 1;
    			WinString = ROCK_SCISSORS;
    		} else if (o2 == GameLogic.PAPER) {
    			ret = -1;
    			WinString = PAPER_SCISSORS;
    		} else {
        		// Undefined state
        		throw new IllegalStateException("o1 is an invalid GameLogic object");
    		}
    	} else {
    		// Undefined state
    		throw new IllegalStateException("o1 is an invalid GameLogic object");
    	}
    	return ret;
    }
    
    public String getWinString() { return WinString; }
}
