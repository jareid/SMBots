package org.smokinmils.cashier.rake;

import org.smokinmils.database.types.ProfileType;

/**
 * Provides a class for referral data to be queued and processed.
 * 
 * @author Jamie Reid
 */
public class Event {
    /** The user of this event. */
    private String      user;

    /** The profile for this event. */
    private ProfileType profile;

    /** The amount of rake for the event. */
    private double      amount;

    /**
     * Constructor.
     * @param usr The user
     * @param prof The profile
     * @param amnt The amount of rake
     */
    public Event(final String usr, final ProfileType prof, final double amnt) {
        setUser(usr);
        setProfile(prof);
        setAmount(amnt);
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
    private void setUser(final String usr) {
        user = usr;
    }

    /**
     * @return the profile
     */
    public final ProfileType getProfile() {
        return profile;
    }

    /**
     * @param prof the profile to set
     */
    private void setProfile(final ProfileType prof) {
        profile = prof;
    }

    /**
     * @return the amount
     */
    public final double getAmount() {
        return amount;
    }

    /**
     * @param amnt the amount to set
     */
    private void setAmount(final double amnt) {
        amount = amnt;
    }
}
