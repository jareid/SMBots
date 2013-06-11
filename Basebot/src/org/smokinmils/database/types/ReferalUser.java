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
* A class used to return values about a transaction for a referal user
* 
* @author Jamie Reid
*/
public class ReferalUser {
	public String user;

    public String group;
	
    public ReferalUser(String user) {
        this.user = user;
        this.group = null;
    }
    
	public ReferalUser(String user, String group) {
	    this.user = user;
	    this.group = group;
	}
	
	public String toString() {
	    return this.user;
	}
	
	@Override
    public boolean equals(Object object) {
        boolean ret = false;

        if (object != null) {
            if (object instanceof String) {
                ret = (this.user == ((String) object));
            } else if (object != null && object instanceof String) {
                ret = (this.user == ((ReferalUser) object).user);
            }
        }           

        return ret;
    }
}
