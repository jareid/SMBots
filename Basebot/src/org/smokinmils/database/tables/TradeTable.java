package org.smokinmils.database.tables;

/**
* Represents the bets table from the database.
* 
* @author Jamie
*/
public final class TradeTable {
    /**
     * Hiding the default constructor.
     */
    private TradeTable() { }
    
    /** The name of the table. */
	public static final String NAME = "trade";

    /** Column for the choice of the trade. */
    public static final String COL_ID = "id";
    
	/** Column for the user id of the trade. */
	public static final String COL_USER = "user";
	
	/** Column for the amount of the trade. */
	public static final String COL_AMOUNT = "amount";
    
    /** Column for the amount of the trade. */
    public static final String COL_PROFILE = "profile";
    
    /** Column for the amount of the trade. */
    public static final String COL_WANTED = "wanted_amount";
    
    /** Column for the amount of the trade. */
    public static final String COL_WANTEDPROF = "wanted_profile";
}
