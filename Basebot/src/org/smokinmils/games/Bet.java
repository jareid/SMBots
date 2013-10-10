/**
 * This file is part of a commercial IRC bot that
 * allows users to play online IRC games.
 *
 * The project was commissioned by Julian Clark
 *
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games;

import java.sql.SQLException;
import java.util.ArrayList;

import org.pircbotx.Channel;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Random;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;

/**
 * A class to store the details of a bet for any of the games.
 *
 * @author Jamie
 */
public class Bet {
    /** Message when someone wins some super rolls.. */
    private static final String  SUPERROLLWIN = "%b%c04%username%c12: Congratulations you have won "
                                              + "%c04%rolls%c12 super rolls!";
    
    /** The user who placed this bet. */
	private final String user;
	
	/** The decimal amount for this bet. */
	private final double amount;
	
	/** The choice made on the bet, may be blank, i.e for DD bets. */
	private final String choice;

    /** The time the bet was made. */
    private final long time;
	
	/** 
	 * Used to activate bets to stop double bets.
	 * @deprecated No longer used as bets are processed in a thread safe manner
	 */
	@Deprecated
    private boolean valid;
	
	/**
	 * The profile used for this bet.
	 * @see ProfileType
	 */
	private final ProfileType profile;
	
	/**
     * The profile used for this bet.
     * @see GamesType
     */
    private final GamesType game;
    
    /** Number of super rolls that can be won per chip. */
    private static double superRolls = 0.0;
    
    /** Chance of winning a super roll per chip. */
    private static double superRollChance = 0.0;
    
    /** Chance of winning a super roll per chip. */
    private static double superRollMaxChance = 0.0;
	
	/**
	 * Constructor. 
	 * 
	 * @param usr     The user for the bet.
	 * @param prof  The profile for the bet.
     * @param gme   The decimal amount of the bet.
	 * @param amnt   The decimal amount of the bet.
	 * @param chce   The choice of the bet, can be null.
	 * 
     * @throws SQLException when the database failed.
	 */
	public Bet(final String usr,
	           final ProfileType prof,
	           final GamesType gme,
	           final double amnt,
	           final String chce) throws SQLException {
		this.user = usr;
		this.amount = amnt;
		this.choice = chce;
		this.valid = true;
		this.profile = prof;
		this.game = gme;
		this.time = System.currentTimeMillis();

        DB db = DB.getInstance();
        db.adjustChips(user, -amount, profile, game, TransactionType.BET);
        db.addBet(user, "", amount, profile, game);
	}
	
	/**
	 * Initialises the super roll info.
	 * 
	 * @param number the number per chip.
	 * @param chance the chance per chip.
     * @param maxchance the maximum chance.
	 * @return 
	 */
	public static void init(final double number, final double chance,
	                        final double maxchance) {
	    superRolls = number;
	    superRollChance = chance;
	    superRollMaxChance = maxchance;
	}
	
	/**
	 * Used to disable a bet to ensure bet's are only
	 * processed when they should be (i.e no double calls).
	 * 
	 * @deprecated
	 */
	@Deprecated
    public final void invalidate() {
		this.valid = false;
	}
	
	/**
     * Used to reactivate a bet to ensure bet's are only
     * processed when they should be (i.e no double calls).
     * 
     * @deprecated
     */
    @Deprecated
    public final void reset() {
    	this.valid = true;
    }
	
	/**
	 * Returns this bet's user.
	 * 
	 * @return The user.
	 */
	public final String getUser() { return user; }
	
	/**
     * Returns this bet's amount.
     * 
     * @return The user.
     */
	public final double getAmount()	{ return amount; }
	
	/**
	 * Returns this bet's choice.
	 * 
	 * @return The choice.
	 */
	public final String getChoice() { return choice; }
	
	/**
     * Returns this bet's choice.
     * 
     * @return The time.
     */
    public final long getTime() { return time; }
	
	/**
     * Returns this bet's validity.
     * 
     * @return true if the bet is valid, false otherwise.
     * 
     * @deprecated
     */
	@Deprecated
    public final boolean isValid() { return valid; }
	
	/**
     * Returns this bet's profile.
     * 
     * @return The profile.
     */
	public final ProfileType getProfile() { return profile; }

    /**
     * @return the game
     */
    public final GamesType getGame() {
        return game;
    }

    /** 
     * This bet was called.
     * 
     * @param caller  The caller's username.
     * @param cprof   The caller's profile.
     * 
     * @throws SQLException When the database failed
     */
    public final void call(final String caller,
                           final ProfileType cprof) throws SQLException {
        DB db = DB.getInstance();
        db.adjustChips(caller, -amount, cprof, game, TransactionType.BET);        
    }
    
	/** 
	 * Cancels this bet. 
	 * @throws SQLException when the database failed.
	 */
    public final void cancel() throws SQLException {
        DB db = DB.getInstance();
        db.adjustChips(user, amount, profile, game, TransactionType.CANCEL);
        db.deleteBet(user, game);        
    }

    /**
     * Provides the user with the winning chips from this game.
     * 
     * @param win the amount won.
     * 
     * @throws SQLException when the database fails.
     */ 
    public final void win(final double win) throws SQLException {
        DB db = DB.getInstance();
        db.adjustChips(user, win, profile, game, TransactionType.WIN);
    }
    
    /** 
     * This bet was lost by the better.
     * 
     * @param caller  The caller's username.
     * @param cprof   The caller's profile.
     * @param win     The amount won.
     * 
     * @throws SQLException When the database failed
     */
    public final void lose(final String caller,
                           final ProfileType cprof,
                           final double win) throws SQLException {
        lose(caller, cprof, win, null, null);
    }
    
    /** 
     * This bet was lost by the better.
     * Also does super rolls, if the details are provided.
     * 
     * @param caller  The caller's username.
     * @param cprof   The caller's profile.
     * @param win     The amount won.
     * @param bot     The bot to announce with.
     * @param chan    The channel to announce to.
     * 
     * @throws SQLException When the database failed
     */
    public final void lose(final String caller,
                           final ProfileType cprof,
                           final double win,
                           final IrcBot bot,
                           final Channel chan) throws SQLException {
        DB db = DB.getInstance();
        db.adjustChips(caller, win, cprof, game, TransactionType.WIN);
        
        // Award super rolls.
        if (bot != null && chan != null) {
            awardSuperRolls(getUser(), caller, getProfile(), cprof, getAmount(), bot, chan);
        }
    }

    /**
     * Awards a number of super rolls.
     * 
     * @param user1 The first user.
     * @param user2 The second user.
     * @param u1prof The first user's profile.
     * @param u2prof The second user's profile.
     * @param amnt  The bet amount.
     * @param bot   The bot to announce with.
     * @param chan  The channel to announce to.
     * 
     * @throws SQLException when db has an error
     */
    public static void awardSuperRolls(final String user1,
                                       final String user2,
                                       final ProfileType u1prof,
                                       final ProfileType u2prof,
                                       final double amnt,
                                       final IrcBot bot,
                                       final Channel chan) throws SQLException {
        // Award super rolls.
        int supers = (int) Math.round(superRolls * amnt);
        if (supers == 0) {
            supers = 1;
        }
        
        double chance = (superRollChance * amnt);
        if (chance > superRollMaxChance) {
            chance = superRollMaxChance;
        }
        
        // first user.
        double random = Random.nextDouble();
        if (u1prof != ProfileType.PLAY &&  random < chance) {
            DB.getInstance().giveSuperRolls(user1, supers);
            String out = SUPERROLLWIN.replaceAll("%username", user1);
            out = out.replaceAll("%rolls", Integer.toString(supers));
            bot.sendIRCMessage(chan, out);
            
        }
        
        // second user.
        random = Random.nextDouble();
        if (!user2.equals("") && u2prof != ProfileType.PLAY && random < chance) {
            DB.getInstance().giveSuperRolls(user2, supers);
            String out = SUPERROLLWIN.replaceAll("%username", user2);
            out = out.replaceAll("%rolls", Integer.toString(supers));
            bot.sendIRCMessage(chan, out);
        }
    }
    
    /**
     * Closes the bet in the database.
     * @throws SQLException when the database failed.
     */
    public final void close() throws SQLException {
        DB db = DB.getInstance();
        db.deleteBet(user, game);
    }

    /**
     * @return The amount of rake from this bet.
     */
    public final double getRake() {
        return Rake.getRake(user, amount, profile);
    }
    
    /**
     * Checks if this bet won the jackpot.
     * 
     * @param bot the bot used to announce.
     */
    public final void checkJackpot(final IrcBot bot) {
        if (Rake.checkJackpot(amount)) { 
            ArrayList<String> players = new ArrayList<String>();
            players.add(user);
            Rake.jackpotWon(profile, game, players, bot, null);
        }
    }
}
