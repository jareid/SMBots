package org.smokinmils.database.tables;

/**
* Represents the bets table from the database.
* 
* @author Jamie
*/
public final class EscrowTable {
    /**
     * Hiding the default constructor.
     */
    private EscrowTable() { }
    
    /** The name of the table. */
	public static final String NAME = "escrow";

    /** Column for the choice of the trade. */
    public static final String COL_RANK = "rank";
    
	/** Column for the user id of the trade. */
	public static final String COL_USER = "user";
	
	/** Column for the amount of the trade. */
	public static final String COL_AMOUNT = "amount";
    
    /** Column for the amount of the trade. */
    public static final String COL_PROFILE = "profile";
}
