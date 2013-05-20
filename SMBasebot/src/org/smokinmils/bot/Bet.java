package org.smokinmils.bot;

public class Bet {

	private String user;
	private int amount;
	private String choice;
	private boolean valid;
	private String profile;
	
	public Bet(String user, String profile, int amount, String choice) {
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
	public int getAmount()	{ return amount; }
	public String getChoice() { return choice; }
	public boolean isValid() { return valid; }
	public String getProfile() { return profile; }
}
