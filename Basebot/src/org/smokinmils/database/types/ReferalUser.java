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
}
