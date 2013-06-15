/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
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
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.ReferalUser;
import org.smokinmils.database.types.ReferrerType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the class for referrals, uses a queue to process referral earnings.
 * 
 * Implements Thread
 * 
 * @author Jamie Reid
 */
public final class Referal extends Thread {
    /** The percentage of the rake provided that goes to jackpot. */
    private static final double  JACKPOT_PERCENT = 0.10;

    /** The percentage of the rake provided that goes to group referral fees. */
    private static final double  GROUP_PERCENT   = 0.20;

    /** The percentage of the rake provided that goes to user referral fees. */
    private static final double  USER_PERCENT    = 0.20;

    /** A queue of events this room needs to process. */
    private static Deque<Event>  rakeQueue;

    /** Instance variable. */
    private static final Referal INSTANCE        = new Referal();

    /**
     * Static 'instance' method.
     * @return the instance of the Referal object
     */
    public static Referal getInstance() {
        return INSTANCE;
    }

    /**
     * Constructor.
     */
    private Referal() {
        rakeQueue = new ArrayDeque<Event>();
        this.start();
    }

    /**
     * (non-Javadoc).
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        boolean interuptted = false;
        while (!(Thread.interrupted() || interuptted)) {
            if (!rakeQueue.isEmpty()) {
                Event event = rakeQueue.removeFirst();
                doReferal(event);
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                interuptted = true;
            }
        }
        return;
    }

    /**
     * Save the new jackpot value.
     * 
     * @param amount The amount to update by
     * @param profile The profile of the jackpot
     */
    private void updateJackpot(final double amount,
                               final ProfileType profile) {
        double jackpot = amount * JACKPOT_PERCENT;
        try {
            DB.getInstance().updateJackpot(profile, jackpot);
        } catch (Exception e) {
            EventLog.log(e, "Rake", "updateJackpot");
        }
    }

    /**
     * Take the referral amounts.
     * 
     * @param event The event to process
     */
    private void doReferal(final Event event) {
        DB db = DB.getInstance();

        try {
            ReferrerType reftype = db.getRefererType(event.getUser());

            double housepercent = 1.0;
            if (Rake.JACKPOTENABLED) {
                housepercent -= JACKPOT_PERCENT;
            }

            if (reftype == ReferrerType.GROUP) {
                housepercent -= (GROUP_PERCENT + USER_PERCENT);
                doGroupReferal(event);
            } else if (reftype == ReferrerType.PUBLIC) {
                housepercent -= USER_PERCENT;
                doPublicReferal(event);
            }

            double housefee = event.getAmount() * housepercent;
            db.houseFees(housefee, event.getProfile());

            if (Rake.JACKPOTENABLED) {
                updateJackpot(event.getAmount(), event.getProfile());
            }
        } catch (Exception e) {
            EventLog.log(e, "Referal", "doReferal");
        }
    }

    /**
     * Take the referral amounts.
     * 
     * @param event The event being processed
     * 
     * @throws SQLException when there is an issue with the database
     */
    private void doPublicReferal(final Event event)
        throws SQLException {
        DB db = DB.getInstance();

        double reffee = event.getAmount() * USER_PERCENT;

        ReferalUser referer = db.getReferalUsers(event.getUser()).get(0);

        db.giveReferalFee(reffee, referer.getUser(), event.getProfile());
    }

    /**
     * Take the referral amounts.
     * 
     * @param event The event being processed
     * 
     * @throws SQLException when there is an issue with the database
     */
    private void doGroupReferal(final Event event)
        throws SQLException {
        DB db = DB.getInstance();

        double reffee = event.getAmount() * USER_PERCENT;
        double grpfee = event.getAmount() * GROUP_PERCENT;

        List<ReferalUser> referals = db.getReferalUsers(event.getUser());
        Map<String, Integer> groups = new HashMap<String, Integer>();
        double userfee = reffee / referals.size();
        int groupusers = 0;
        for (ReferalUser user : referals) {
            if (user.getGroup() != null) {
                if (!groups.containsKey(user.getGroup())) {
                    groups.put(user.getGroup(), 1);
                } else {
                    int count = groups.get(user.getGroup()) + 1;
                    groups.put(user.getGroup(), count);
                }
                groupusers++;
            }

            db.giveReferalFee(userfee, user.getUser(), event.getProfile());
        }

        // Give each group an equivalent amount based on number of users
        // in the group with that referal
        if (groupusers > 0) {
            double eachgrpfee = grpfee / groupusers;
            for (Entry<String, Integer> group : groups.entrySet()) {
                db.giveReferalFee(
                        eachgrpfee * group.getValue(), group.getKey(),
                        event.getProfile());
            }
        } else {
            db.houseFees(grpfee, event.getProfile());
        }
    }

    /**
     * Adds an event to be handled by this Room's thread.
     * 
     * @param user The user the event is for.
     * @param profile The profile.
     * @param amount The amount of rake taken.
     */
    public void addEvent(final String user,
                         final ProfileType profile,
                         final double amount) {
        rakeQueue.addLast(new Event(user, profile, amount));
    }
}
