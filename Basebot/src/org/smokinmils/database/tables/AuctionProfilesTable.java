package org.smokinmils.database.tables;

/**
 * Represents the auction profiles table.
 * @author cjc
 *
 */
public final class AuctionProfilesTable {
    
    /**
     * Hiding the default constructor.
     */
    private AuctionProfilesTable() { }
    
    /** The name of the table. */
    public static final String NAME = "auction_profiles";
    
    /** Column for the unique id. */
    public static final String COL_AUCTIONID = "aid";
    
    /** Column for the user id of the bet. */
    public static final String COL_PROFILEID = "pid";
}
