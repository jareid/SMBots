/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid & Carl Clegg
 */
package org.smokinmils.auctions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to auction items / chips.
 * 
 * @author Jamie & Carl
 */
public class Auctions extends Event {
    
    /** The Auctions command. */
    public static final String AUCCMD = "!auctions";
    
    /** The bid command. */
    public static final String  BIDCMD       = "!bid";
    
    /** The bid command format. */
    public static final String  BIDFMT        = "%b%c12" + BIDCMD + " <item_id>";
    
    /** The bid command length. */
    public static final int BID_CMD_LEN = 2;
    
    /** The add command. */
    public static final String  ADDCMD     = "!additem";

    /** The add command format. */
    public static final String  ADDFMT      = "%b%c12" + ADDCMD + " <item/chips> <length in minutes> "
            + "<start_price> <profile> <item name/amount of chips> ";

    /** The add command length. */
    public static final int     ADD_CMD_LEN     = 6;
    
    /** The amount to increase each auction by. */
    public static final double INCR_AMOUNT = 0.5;
    
    /** String informing the user that no auction with that ID is available. */
    public static final String NO_AUC_ID = "%b%c04%who%c12: Unable to find the auction with that "
    		                                                                            + "id";

    /** String informing the user bid has been accepted. */
    private static final String AUC_BID = "%b%c12Bid Accepted, %c04%who%c12 is now the highest"
                            + " bidder for the item %c04%itemname%c12 with the id %c04%id%c12 at"
                            + " %c04%newamount%c12 coins. Type %c04!bid %id%c12 to bid!";

    /** String informing users of the active auctions. */
    private static final String ACTIVE_AUCTIONS = "%b%c12The following auctions are active, to bid "
                    + "type %c04!bid <itemid>%c12 or type %c04!info auctions%c12 for instructions";
    
    /** String for each individual auction to be added to above. */
    private static final String SINGLE_AUCTION = " %b%c04%name %c12(id: %c04%id%c12, "
                                  + "Time left: %c04%time%c12,  Current bid: %c04%bid%c12 coins, "
                                  + "Profile(s): %c04%profile%c12)";
    
    /** String announcing auction that is in the final minute. */
    private static final String FINAL_MINUTE = "%b%c04%name%c12 auction has only 1 minute left!" 
                                        + " type %c04!bid %id%c12 now to bid before it's too late!";
    
    /** String to inform the user they can't use the play profile. */
    private static final String WRONG_PROFILE = "%b%c04%who%c12: This auction is using the "
            + "%c04%profile%c12 profile(s).";

    /** String to indicate there is no current winner for the auction. */
    public static final String NO_WINNER = "No one";

    /** String to let the admin know the item has been added with a specific id. */
    private static final String AUC_ADDED = "%b%c04%who%c12: The auction has been added with "
    		+ "id:%c04%id";

    /** String to check for when adding a chips auction. */
    private static final String ADD_CHIPS_VAR = "chips";
    
    /** The list of auctions that are active. */
    private ArrayList<AnAuction> auctions;
    
    /** The list of auctions that are in the final count down! */
    private ArrayList<AnAuction> finalAuctions;
    
    /** Timer to announce auctions. */
    private Timer auctionTimer;
    
    /**
     * Constructor.
     * @param irc the irc bot 
     * @param chan the channel we announce on
     */
    public Auctions(final IrcBot irc,
                    final String chan) {
        auctions = new ArrayList<AnAuction>();
        finalAuctions = new ArrayList<AnAuction>();
        
        auctionTimer = new Timer(true);
        auctionTimer.schedule(new AuctionTimer(irc, chan), Utils.MS_IN_MIN / 2, Utils.MS_IN_MIN);
    }
    
    /**
     * This method handles the auction commands.
     * 
     * @param event the Message event
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        
        if (isValidChannel(chan.getName()) && bot.userIsIdentified(sender)) {
            if (bot.userIsOp(sender, chan.getName()) && Utils.startsWith(message, ADDCMD)) {
                addItem(event);
            } else if (Utils.startsWith(message, BIDCMD)) {
                placeBid(event);
            } else if (Utils.startsWith(message, AUCCMD)) {
                listAuctions(event);
            } 
        }
    }

    /**
     * function that deals with the !auctions command.
     * @param event the event from which the request was sent.
     */
    private void listAuctions(final Message event) {
        Channel chan = event.getChannel();
        IrcBot bot = event.getBot();

    
        if (auctions.size() > 0) {
            bot.sendIRCMessage(chan, ACTIVE_AUCTIONS);
            for (AnAuction a : auctions) {
                // check for open auctions, announce here
                System.out.println(a.getTime());
                    String add = SINGLE_AUCTION.replaceAll("%name", a.getName());
                    add = add.replaceAll("%id", String.valueOf(a.getId()));
                    add = add.replaceAll("%time", Utils.secondsToString(a.getTime()));
                    add = add.replaceAll("%bid", String.valueOf(a.getPrice()));
                    bot.sendIRCMessage(chan, add);
                   
            }
        }
        
    }

    /**
     * This method handles the bid command.
     * 
     * @param event the Message event
     */
    private void placeBid(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        String[] msg = message.split(" ");
        
        // get id, then try to bid etc
        if (msg.length < BID_CMD_LEN) {
            bot.invalidArguments(sender, BIDFMT);
        } else {
            int auctionId = Utils.tryParse(msg[1]);
            if (auctionId == 0) {
                bot.invalidArguments(sender, BIDFMT);
            } else {
                AnAuction a = null;
                for (AnAuction anAuction : auctions) {
                    if (anAuction.getId() == auctionId) {
                        a = anAuction;
                        break;
                    }
                } 
                // if no auction, check auctions that are in the final count down
                if (a == null) {
                    for (AnAuction anAuction : finalAuctions) {
                        if (anAuction.getId() == auctionId) {
                            a = anAuction;
                            break;
                        } 
                    } 
                }
                
                if (a != null) {
                    try {
                        DB db = DB.getInstance();
                        ProfileType profile = db.getActiveProfile(sender.getNick());
                        if (a.getTime() > 0) {
                            if (!a.isValidProfile(profile)) {
                                // using play profile, don't allow them
                                String out = WRONG_PROFILE.replaceAll("%who", sender.getNick());
                                String profiles = Utils.listToString(a.getProfiles());
                                out = out.replaceAll("%profile", profiles);
                                bot.sendIRCNotice(sender, out);
                            } else {
                                if (a.bid(event)) {
                                    db.updateAuction(a.getName(), a.getPrice(), 
                                            sender.getNick());
                                    String out = AUC_BID.replaceAll("%who", sender.getNick());
                                    out = out.replaceAll("%newamount", 
                                                            Utils.chipsToString(a.getPrice()));
                                    out = out.replaceAll("%itemname", a.getName());
                                    out = out.replaceAll("%id", String.valueOf(a.getId()));
                                    bot.sendIRCMessage(chan, out);
                                } else {
                                   bot.noChips(sender, AnAuction.BID_PRICE, profile);
                                }
                            }
                        } else {
                            String out = NO_AUC_ID.replaceAll("%who", sender.getNick());
                            bot.sendIRCMessage(chan, out);  
                        }
                    } catch (SQLException e) {
                        EventLog.log(e, "Auctions", "placeBid");
                     }
                } else {
                    String out = NO_AUC_ID.replaceAll("%who", sender.getNick());
                    bot.sendIRCMessage(chan, out);
                }
            }
        }
    }

    /**
     * This method handles the add item command.
     * 
     * @param event the Message event
     */
    private void addItem(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        String[] msg = message.split(" ");
        
        if (msg.length < ADD_CMD_LEN) {
            bot.invalidArguments(sender, ADDFMT);
        } else {
            double price = Utils.tryParseDbl(msg[1 + 1 + 1]);
            int time = Utils.tryParse(msg[1 + 1]);
            
            String item = msg[1 + 1 + 1 + 1 + 1];
            double chips = 0.0;
            
            // if we are adding chips 
            if (msg[1].equalsIgnoreCase(ADD_CHIPS_VAR)) {
                chips = Utils.tryParseDbl(item);
            } else {
                // it must be an item?
                for (int i = 1 + 1 + 1 + 1 + 1 + 1; i < msg.length; i++) {
                    item += " " + msg[i];
                }
            }
            
            // get profiles
            ArrayList<ProfileType> pr = new ArrayList<ProfileType>();
            String[] profiles = msg[1 + 1 + 1 + 1].split(",");
            for (String s : profiles) {
                ProfileType p = ProfileType.fromString(s);
                if (p != null) {
                    pr.add(p);
                } else {
                    // if encounter a non valid profile, cry
                    pr = null;
                    break;
                }
            }
            
            if (price <= 0.0 || time <= 0.0 || pr == null) {
                bot.invalidArguments(sender, ADDFMT); 
            } else {
                AnAuction a  = null;
                if (chips != 0.0) {
                    a = new AnAuction(price, chips, time, pr);
                } else {
                    a = new AnAuction(price, item, time, pr);
                }
                
                auctions.add(a);
               
                String out = AUC_ADDED.replaceAll("%who", sender.getNick());
                out = out.replaceAll("%id", String.valueOf(a.getId()));
                bot.sendIRCNotice(sender, out);
 
            }
        }
    }
    
    /**
     * Auction timer to announce open auctions.
     * @author cjc
     *
     */
    public class AuctionTimer extends TimerTask {
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
        public AuctionTimer(final IrcBot bot,
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
            if (auctions.size() > 0) {
                irc.sendIRCMessage(chan, ACTIVE_AUCTIONS);
                for (AnAuction a : auctions) {
                    // check for open auctions, announce here

                    String add = SINGLE_AUCTION.replaceAll("%name", a.getName());
                    add = add.replaceAll("%id", String.valueOf(a.getId()));
                    add = add.replaceAll("%time", Utils.secondsToString(a.getTime()));
                    add = add.replaceAll("%bid", String.valueOf(a.getPrice()));
                    add = add.replaceAll("%profile", Utils.listToString(a.getProfiles()));
                    irc.sendIRCMessage(chan, add);
                    a.tick();
                        
                    if (a.getTime() <= 1 * Utils.MIN_IN_HOUR) {
                        finalAuctions.add(a);
                        String name = "";
                        if (a.getWinner() == null) {
                            name = NO_WINNER;
                        } else {
                            name = a.getWinner().getNick();
                        }
                        String out = FINAL_MINUTE.replaceAll("%name", a.getName());
                        out = out.replaceAll("%id", String.valueOf(a.getId()));
                        out = out.replaceAll("%bid", Utils.chipsToString(a.getPrice()));
                        out = out.replaceAll("%winner", name);
                        a.finalMinute(irc, chan);
                        irc.sendIRCMessage(chan, out);
                    }
                }
                
            }
            
            // remove all members of final auctions from auctions
            for (AnAuction a : finalAuctions) {
                if (auctions.contains(a)) {
                    auctions.remove(a);
                }
            }
        }
    }
}
