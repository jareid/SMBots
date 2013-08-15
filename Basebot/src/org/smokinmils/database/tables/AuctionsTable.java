package org.smokinmils.database.tables;

/**
* Represents the auctions table from the database.
* 
* @author cjc
*/
public final class AuctionsTable {
    /**
     * Hiding the default constructor.
     */
    private AuctionsTable() { }
    
    /** The name of the table. */
    public static final String NAME = "auctions";
    
    /** Column for the unique id. */
    public static final String COL_ID = "id";
    
    /** Column for the user id of the bet. */
    public static final String COL_USERID = "userid";
    
    /** Column for the amount of the bet. */
    public static final String COL_AMOUNT = "amount";
    
    /** Column for the choice of the bet. */
    public static final String COL_ITEMNAME = "item_name";
    
    /** Column for the game id for the bet. */
    public static final String COL_PAID = "paid";
    
    /** Column for the profile id for the bet. */
    public static final String COL_FINISHED = "finished";
    
    /** Column for the time left. */
    public static final String COL_TIMELEFT = "timeleft";
    
    /** Column to determine if chips or not. */
    public static final String COL_CHIPS = "chips";
    
    /** Profile for the auction. */
    public static final String COL_PROFILE = "profile";
}
