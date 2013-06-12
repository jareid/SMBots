package org.smokinmils.database.tables;

/**
* Represents the bets table from the database.
* 
* @author Jamie
*/
public final class BetsTable {
    /**
     * Hiding the default constructor.
     */
    private BetsTable() { }
    
    /** The name of the table. */
	public static final String NAME = "bets";
	
	/** Column for the unique id. */
	public static final String COL_ID = "id";
	
	/** Column for the user id of the bet. */
	public static final String COL_USERID = "userid";
	
	/** Column for the amount of the bet. */
	public static final String COL_AMOUNT = "amount";
	
	/** Column for the choice of the bet. */
	public static final String COL_CHOICE = "choice";
	
	/** Column for the game id for the bet. */
	public static final String COL_GAMEID = "gameid";
	
	/** Column for the profile id for the bet. */
	public static final String COL_PROFILE = "profile";
}
