/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.database.types;

/**
* A class used to return values about a transaction for a referral user.
* 
* @author Jamie Reid
*/
public class ReferalUser {
    /** The referrer/ee. */
	private String user;

    /** The rank group. */
    private String group;
	
    /**
     * Constructor.
     * @param usr The referrer/ee
     */
    public ReferalUser(final String usr) {
        setUser(usr);
        setGroup(null);
    }
    
    /**
     * Constructor.
     * @param usr The referrer/ee
     * @param grp The rank group
     */
	public ReferalUser(final String usr, final String grp) {
	    setUser(usr);
	    setGroup(grp);
	}
	
	/**
     * (non-Javadoc).
     * @see java.lang.Enum#toString()
     * @return the output
     */
    @Override
    public final String toString() {
	    return this.getUser();
	}
    
    /**
     * (non-Javadoc).
     * @see java.lang.Object#hashCode()
     * @return the hash code
     */
    @Override
    public final int hashCode() {
        return this.getUser().hashCode();
    }
    
    /**
     * (non-Javadoc).
     * @see java.lang.Enum#equals()
     * @param object the object
     * @return true if equal
     */
    @Override
    public final boolean equals(final Object object) {
        boolean ret = false;

        if (object != null) {
            if (object instanceof String) {
                ret = (this.getUser().equalsIgnoreCase((String) object));
            } else if (object != null && object instanceof String) {
                ret = (this.getUser().equalsIgnoreCase(
                        ((ReferalUser) object).getUser()));
            }
        }           

        return ret;
    }

    /**
     * @return the group
     */
    public final String getGroup() {
        return group;
    }

    /**
     * @param grp the group to set
     */
    public final void setGroup(final String grp) {
        group = grp;
    }

    /**
     * @return the user
     */
    public final String getUser() {
        return user;
    }

    /**
     * @param usr the user to set
     */
    public final void setUser(final String usr) {
        user = usr;
    }
}
