package org.smokinmils.casino;


public class Bet {

	private String user;
	private int amount;
	private String choice;
	private boolean valid;
	private String profile;
	
	public Bet(String user, String profile, int amount, String choice)
	{
		this.user = user;
		this.amount = amount;
		this.choice = choice;
		this.valid = true;
		this.profile = profile;
	}
	
	public void invalidate()
	{
		this.valid = false;
	}
	
	public void reset()
	{
		this.valid = true;
	}
	public String getUser() { return this.user; }
	public int getAmount()	{ return this.amount; }
	public String getChoice() { return this.choice; }
	public boolean isValid() { return this.valid; }
	public String getProfile() { return this.profile; }
}
