package org.smokinmils.auctions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Models an item / chips being auctioned
 * TODO - better name? (yay for refactoring)
 * @author cjc
 *
 */
public class AnAuction {
    
    /** String letting the user know to pay, pay now! */
    private static final String WINNER = "%b%c04%who%c12: You have won the auction for %c04%name"
    		+ " %c12if the auction was for chips these have been added to your account, if an item " 
    		+ ", please query a member of staff! ";
   
    /** String for the final minute. */
    private static final String LT_MIN = "%b%c04%name%c12 auction has only %time left!" 
                                    + " type %c04!bid %id%c12 now to bid before it's too late!";

    /** How often in seconds to spam when less than 10 seconds. */
    private static final int SPAM_EVERY_LT_10 = 2;

    /** String to announce auction winnor. */
    private static final String AUCTION_WON = "%b%c04%who%c12 has won %c04%item%c12 for"
    		+ " %c04%price%c12 chips ";

    /** The cost to bid. */
    public static final int BID_PRICE = 20;
    
    /** At what time in seconds do we extend the time left?. */
    private static final int END_TIME = 20;
    
    /** How often to spam in the last minute. */
    private static final int SPAM_EVERY_LT_MIN = 10;
    
    /** the price of the item. */
    private double price = 0.0;
    
    /** the name of the item. */
    private String name = "";
    
    /** the ID of the item. */
    private int id = 0;
    
    /** the winning user. */
    private User winner = null;
    
    /** The profile the current winner is using. */
    private ProfileType winnerProf = null;
    
    /** the time left for the auction. */
    private int time = 0;
    
    /** if this auction is for chips? */
    private boolean chips = false;
    
    /** If this is for chips, how much for. */
    private double chipsWin = 0.0;
    
    /** The profile for this auction. */
    private ArrayList<ProfileType> profiles;
    
    /**
     * Contructor.
     * @param p the price
     * @param n the name
     * @param t the initial time in minutes 
     * @param pr valid profiles If selling chips, the first one is the chips we are selling too
     */
    public AnAuction(final double p,
                     final String n, 
                     final int t,
                     final ArrayList<ProfileType> pr) {
        price = p;
        name = n;
        time = t * Utils.MIN_IN_HOUR;
        profiles = pr;
        
        try {
            id = DB.getInstance().addAuction(name, price, pr);
        } catch (SQLException e) {
            EventLog.log(e, "Auctions", "addItem");
        }
        
    }
    
    /**
     * Secondary constructor for chips.
     * @param p the price
     * @param c the number of chips
     * @param t the initial time in minutes
     * @param pr the profiles
     */
    public AnAuction(final double p,
                     final double c, 
                     final int t,
                     final ArrayList<ProfileType> pr) {
        this(p, Utils.chipsToString(c) + " " + pr.get(0) + " chips", t, pr);
        chips = true;
        chipsWin = c;
    }

    /**
     * Attempts to let the user bid, fails if not enough coinchips.
     * @param event the message event to get info from
     * @return true if success, false if not
     */
    public final boolean bid(final Message event) {
        IrcBot bot = event.getBot();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        boolean ret = false;
        if (Utils.checkCredits(sender, (double) BID_PRICE + price + Auctions.INCR_AMOUNT, bot, chan)
                                                                                    >= BID_PRICE) {
            DB db = DB.getInstance();
            try {
                // refund the previous winner
                if (winnerProf != null) {
                    db.adjustChips(winner.getNick(), price, winnerProf, 
                            GamesType.AUCTION, TransactionType.CANCEL);
                }
                
                // increase the bid amount
                price += Auctions.INCR_AMOUNT;
                
                // remove the bid price
                db.adjustChips(sender.getNick(), -BID_PRICE, db.getActiveProfile(sender.getNick()), 
                        GamesType.AUCTION, TransactionType.AUCTION_BID);
                
                // take away the number of chips from the user so if they win we have been paid
                db.adjustChips(sender.getNick(), -price, db.getActiveProfile(sender.getNick()),
                         GamesType.AUCTION, TransactionType.AUCTION_PAY);
                
                // update to the new winner / winner profile
                winner = sender;
                winnerProf = db.getActiveProfile(winner.getNick());
                
                
                ret = true;
            } catch (SQLException e) {
                EventLog.log(e, "anAuction", "bid");
            }
           
            if (time < END_TIME) {
                time = END_TIME;
            }
            
        } else {
            ret = false;
        }
        return ret;
        
    }
    /**
     * Gets the current price of the auction.
     * @return the price
     */
    public final double getPrice() {
        return price;
    }
    
    /**
     * Gets the name of the item.
     * @return the name
     */
    public final String getName() {
        return name;
    }

    /**
     * Gets the id of the auction.
     * @return the id
     */
    public final int getId() {
        return id;
    }
    
    /**
     * Gets the current winning bidder (or when over, the winner).
     * @return the user who is winning / won. or null
     */
    public final User getWinner() {
        return winner;
    }
    
    /**
     * Gets the time left in seconds.
     * @return the time left
     */
    public final int getTime() {
        return time;
    }
    
    /**
     * Checks if this auction is for chips.
     * @return true if a chips auction false if an item auction
     */
    public final boolean isChips() {
        return chips;
    }
    
    /**
     * Returns the number of chips they would win with!
     * @return value of chips to be won
     */
    public final double getChips() {
        return chipsWin;
    }
    /**
     * Subtracts a minute from the auction.
     */
    public final void tick() {
        tick(Utils.MIN_IN_HOUR);
    }
    
    /**
     * Subtracts t seconds from the auction.
     * @param t the time to subtract
     */
    public final void tick(final int t) {
        time = time - t;
    }
    
    /**
     * Checks if profile p is valid for this auction.
     * @param p the profile to check
     * @return true if valid, otherwise false
     */
    public final boolean isValidProfile(final ProfileType p) {
        return profiles.contains(p);
    }
    
    /**
     * Gets the profiles valid for this auction.
     * @return dem profiles
     */
    public final ArrayList<ProfileType> getProfiles() {
        return profiles;
    }
    
    /**
     * Auction timer to announce open auctions.
     * @author cjc
     *
     */
    public class FinalCountDown extends TimerTask {

        /** The bot used for announcing. */
        private final IrcBot irc;
        
        /** The channel we announce on. */
        private final String chan;
        
        /**
         * Constructor.
         * 
         * @param bot The bot to announce with.
         * @param channel the channel to announce on
         */
        public FinalCountDown(final IrcBot bot,
                            final String channel) {
            irc = bot;
            chan = channel;
        }

        /**
         * (non-Javadoc).
         * @see java.util.TimerTask#run()
         */
        @SuppressWarnings("deprecation")
        @Override
        public final void run() {
            String winnerName = "";
            if (getWinner() == null) {
                winnerName = "No one";
            } else {
                winnerName = getWinner().getNick();
            }
           
           int timeToTick = SPAM_EVERY_LT_MIN;
           if (getTime() <= timeToTick) {
               timeToTick = SPAM_EVERY_LT_10;
           }
           tick(timeToTick);
           if (getTime() > 0) { 
               
               String out = LT_MIN.replaceAll("%name", getName());
               out = out.replaceAll("%id", String.valueOf(getId()));
               out = out.replaceAll("%bid", Utils.chipsToString(getPrice()));
               out = out.replaceAll("%winner", winnerName);
               out = out.replaceAll("%time", String.valueOf(getTime()) + " seconds");
               irc.sendIRCMessage(chan, out);
               Timer finalAuctionTimer = new Timer(true);
               finalAuctionTimer.schedule(new FinalCountDown(irc, chan), 
                                                                   timeToTick * Utils.MS_IN_SEC);
           } else {
               
               try {
                DB db = DB.getInstance();
                db.endAuction(getId());
                String out = AUCTION_WON.replaceAll("%who", winnerName);
                out = out.replaceAll("%item", getName());
                out = out.replaceAll("%price", Utils.chipsToString(getPrice()));
                irc.sendIRCMessage(chan, out);
                
                if (chips) {
                  db.adjustChips(winnerName, chipsWin, getProfiles().get(0), GamesType.AUCTION, 
                          TransactionType.WIN); 
                }
                out = WINNER.replaceAll("%who", winnerName);
                out = out.replaceAll("%name", getName());
                out = out.replaceAll("%id", String.valueOf(getId()));
                irc.sendIRCNotice(getWinner(), out);
                
            } catch (SQLException e) {
               EventLog.log(e, "AnAuction", "ending auction");
            }
               
           } 
        }
    }

    /**
     * Start the final minute shenanigans.
     * @param irc the irc bot to send messages
     * @param chan the channel to spam to
     */
    public final void finalMinute(final IrcBot irc, 
                            final String chan) {
        Timer finalAuctionTimer = new Timer(true);
        finalAuctionTimer.schedule(new FinalCountDown(irc, chan), 
                                                            SPAM_EVERY_LT_MIN * Utils.MS_IN_SEC);
        
    }
}
