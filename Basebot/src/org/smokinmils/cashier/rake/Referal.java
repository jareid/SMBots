/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.cashier.rake;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.smokinmils.database.DB;
import org.smokinmils.database.DBException;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.ReferalUser;
import org.smokinmils.database.types.ReferrerType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the class for referals, uses a queue to process referal earnings
 * 
 * Implements Thread
 * 
 * @author Jamie Reid
 */
public class Referal extends Thread {
    private static final double JACKPOT_PERCENT = 0.20;
    private static final double GROUP_PERCENT = 0.20;
    private static final double USER_PERCENT = 0.20;
    
    /** A queue of events this room needs to process */
    private static Deque<Event> Events;

    /** Instance variable */
    private static final Referal instance = new Referal();

    /** Static 'instance' method */
    public static Referal getInstance() {
       return instance;
    }

    /**
     * Constructor.
     */
	private Referal() {
		Events = new ArrayDeque<Event>();
		this.start();
	}
	
	/**
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		boolean interuptted = false;
    	while ( !(Thread.interrupted() || interuptted) ) { 
        	if ( !Events.isEmpty() ) {
                Event event = Events.removeFirst();
                doReferal(event);
            }
        	
        	try {
        		Thread.sleep(10);
        	} catch (InterruptedException e) {
        		interuptted = true;
        	}
    	}
    	return;
	}	
	
	/**
     * Save the new jackpot value
     * @return 
     */
    private void updateJackpot(double amount,
                                      ProfileType profile) {
        double jackpot = amount * JACKPOT_PERCENT;
        try {
            DB.getInstance().updateJackpot(profile, jackpot);
        } catch (Exception e) {
            EventLog.log(e, "Rake", "updateJackpot");
        }
    }
    
    /**
     * Take the referal amounts
     */
    private void doReferal(Event event) {
        DB db = DB.getInstance();
        
        try {
            ReferrerType reftype = db.getRefererType( event.user );

            double house_percent = 1.0 - (Rake.JackpotEnabled ? JACKPOT_PERCENT : 0.0);
            if (reftype == ReferrerType.GROUP) {
                house_percent -= (GROUP_PERCENT + USER_PERCENT);
                doGroupReferal(event);
            } else if (reftype == ReferrerType.PUBLIC) {
                house_percent -= USER_PERCENT;
                doPublicReferal(event);
            } else {
                // do nothing
            }
            
            double house_fee = event.amount * house_percent;
            db.houseFees(house_fee, event.profile);
            
            if ( Rake.JackpotEnabled ) {
                updateJackpot(event.amount, event.profile);
            }
        } catch (Exception e) {
            EventLog.log(e, "Referal", "doReferal");
        }
    }
	
    /**
     * Take the referal amounts
     * @throws SQLException 
     * @throws DBException 
     */
    private void doPublicReferal(Event event) throws DBException, SQLException {
        DB db = DB.getInstance();

        double ref_fee = event.amount * USER_PERCENT;
        
        ReferalUser referer = db.getReferalUsers(event.user).get(0);
        
        db.giveReferalFee(ref_fee, referer.user, event.profile);
    }
    
    /**
     * Take the referal amounts
     * @throws SQLException 
     * @throws DBException 
     */
    private void doGroupReferal(Event event) throws DBException, SQLException {
        DB db = DB.getInstance();

        double ref_fee = event.amount * USER_PERCENT;
        double grp_fee = event.amount * GROUP_PERCENT;
           
        List<ReferalUser> referals = db.getReferalUsers(event.user);
        Map<String, Integer> groups = new HashMap<String, Integer>();
        double user_fee = ref_fee / referals.size();
        int groupusers = 0;
        for (ReferalUser user: referals) {
            if (user.group != null) {
                if (!groups.containsKey(user.group)) groups.put(user.group, 1);
                else groups.put( user.group, groups.get(user.group) + 1 );
                groupusers++;
            }
            
            db.giveReferalFee(user_fee, user.user, event.profile);
        }
        
        // Give each group an equivalent amount based on number of users
        // in the group with that referal
        if (groupusers > 0)  {
            double each_grp_fee = grp_fee / groupusers;
            for (Entry<String, Integer> group: groups.entrySet()) {
                db.giveReferalFee(each_grp_fee * group.getValue(), group.getKey(), event.profile);
            }
        } else {
            db.houseFees(grp_fee, event.profile);
        }
    }
    
	/**
	 * Adds an event to be handled by this Room's thread
	 */
	public void addEvent(String user, ProfileType profile, double amount) {
		Events.addLast( new Event(user, profile, amount) );		
	}
}
