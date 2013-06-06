package org.smokinmils.bot;

import org.smokinmils.database.types.ProfileType;

public class Bet {

	private String user;
	private double amount;
	private String choice;
	private boolean valid;
	private ProfileType profile;
	
	public Bet(String user, ProfileType profile, double amount, String choice) {
		this.user = user;
		this.amount = amount;
		this.choice = choice;
		this.valid = true;
		this.profile = profile;
	}
	
	public void invalidate() {
		this.valid = false;
	}
	
	public void reset() {
		this.valid = true;
	}
	
	public String getUser() { return user; }
	public double getAmount()	{ return amount; }
	public String getChoice() { return choice; }
	public boolean isValid() { return valid; }
	public ProfileType getProfile() { return profile; }
}
