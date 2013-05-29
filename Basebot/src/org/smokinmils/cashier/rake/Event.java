package org.smokinmils.cashier.rake;

import org.smokinmils.database.types.ProfileType;

public class Event {
    public String user;
    public ProfileType profile;
    public double amount;
    public Event(String user, ProfileType profile, double amount) {
        this.user = user;
        this.profile = profile;
        this.amount = amount;           
    }
}
