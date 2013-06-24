package org.smokinmils.database.tables;

/**
* Represents the Hostgroup Users Table from the database.
* 
* @author Jamie
*/
public final class HostGroupUsersTable {
    /**
     * Hiding the default constructor.
     */
    private HostGroupUsersTable() { }
    
    /** The name of the table. */
    public static final String NAME = "hostgroup_users";
    
    /** Column for the group id. */
    public static final String COL_GROUPID = "group_id";
    
    /** Column for the user id. */
    public static final String COL_USERID = "user_id";
    
    /** Column for the points. */
    public static final String COL_POINTS = "points";
}
