/**
 * This file is part of a commercial IRC bot that allows users to play online
 * IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.cashier.commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.bot.CheckIdentified;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Pair;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.XMLLoader;
import org.smokinmils.bot.events.Join;
import org.smokinmils.bot.events.Message;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.database.types.Swap;
import org.smokinmils.database.types.Trade;
import org.smokinmils.database.types.TradeResult;
import org.smokinmils.database.types.TransactionType;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to payout a user some chips.
 * 
 * @author Jamie
 */
public class Escrow extends Event {
    /** The trade command. */
    public static final String  TRADECMD       = "!trade";

    /** The trade command format. */
    public static final String  TRADEFMT      = "%b%c12" + TRADECMD + " <user> <amount> <profile>";
    
    /** The trade command. */
    public static final String  TRADECXLCMD    = "!tradecancel";

    /** The trade command format. */
    public static final String  TRADECXLFMT    = "%b%c12" + TRADECXLCMD + " <user>";
    
    /** The trade command format. */
    public static final String  MGRTRADECXLFMT = "%b%c12" + TRADECXLCMD + " <rank> <user>";
    
    /** The trade command length. */
    public static final int     TRADE_CMD_LEN  = 4;
    
    /** The trade + swap command length. */
    public static final String     SWAPCMD     = "!swap";
    
    /** The trade + swap command format. */
    public static final String  SWAPFMT      = "%b%c12" + SWAPCMD + " <amount> <profile>"
                                                                  + " <amount> <profile>";
    
    /** The trade + swap command length. */
    public static final int     SWAP_CMD_LEN     = 5;
    
    /** The trade + swap command length. */
    public static final String     SWAPCXLCMD     = "!swapcancel";
    
    /** The swap cancel command format. */
    public static final String  SWAPCXLFMT      = "%b%c12" + SWAPCXLCMD + " <id>";
    
    /** The confirm command. */
    public static final String  AGREECMD     = "!agree";

    /** The confirm command format. */
    public static final String  AGREEFMT      = "%b%c12" + AGREECMD + " <trade_id>";
    
    /** The confirm command. */
    public static final String  CONFIRMCMD     = "!confirm";

    /** The confirm command format. */
    public static final String  CONFIRMFMT      = "%b%c12" + CONFIRMCMD + " <user>";
    
    /** Message to say yyou can't trade with yourself. */
    private static final String SELFTRADE      = "%b%c04%who%c12: You can't trade with yourself!";

    /** The message to the channel on trade start. */
    private static final String SWAP   = "%b%c04[SWAP]%c12 A %c04%user%c12 has requested a swap "
                                        + "for %c04%amount %profile%c12 and they want "
                                        + "%c04%wamount %wprofile%c12. Please type %c04 "
                                        + AGREECMD + " %id";
    
    /** The message to the channel on trade start. */
    private static final String OPEN_SWAPS = "%b%c04[%c12OPEN SWAPS%c04]%c12 To swap ";
    
   /** The message to the channel on trade start. */
   private static final String OPEN_SWAP = "%c04%wamount %wprofile%c12 for %c04%amount %profile%c12"
                                         + " type %c04" + AGREECMD + " %id %c12| ";
   
   /** number of swaps per line. */
   private static final int SWAP_PER_LINE = 5;
    
    /** The message to the channel on trade start. */
    private static final String SWAPCOMP   = "%b%c04[SWAP]%c12 Swap ID %c04%id%c12 has been "
                                        + "completed. %c04%user%c12 sold %c04%amount %profile%c12 "
                                        + "to %c04%user2%c12 for %c04%wamount %wprofile%c12";
    
    /** The message to the channel on  no swap started. */
    private static final String NO_SWAP = "%b%c04[SWAP]%c12 No swap trade exists with the ID "
                                         + "%c04%id%c12. Please use %c04"
                                         + SWAPCMD + "%c12 to start one.";
    
    /** The message to the channel on  no swap started. */
    private static final String NO_CXL_SWAP = "%b%c04[SWAP]%c12 The swap trade with the ID "
                                         + "%c04%id%c12 can only be cancelled by %c04%who%c12";
    
    /** The message to the channel on  no swap started. */
    private static final String CXL_SWAP = "%b%c04[SWAP]%c12 The swap trade with the ID "
                                         + "%c04%id%c12 has been cancelled!";

    /** The message to the channel on trade start. */
    private static final String TRADE   = "%b%c04[TRADE]%c12 A %c04%profile%c12 trade between "
                                        + "%c04%rank%c12 and %c04%user%c12 has been started. "
                                        + "The %c04%chips%c12 chips have been removed from "
                                        + "%c04%rank%c12's account and are locked until "
                                        + "confirmation is received. %c04%rank%c12 when you "
                                        + "receive your gold please type %c04 " + CONFIRMCMD
                                        + " %user";
    
    /** The message to the channel on no trade started. */
    private static final String NO_TRADE = "%b%c04[TRADE]%c12 No trade exists between "
                                         + "%c04%rank%c12 and %c04%user%c12. Please use %c04"
                                         + TRADECMD + "%c12 to start one.";
    
    /** The message to the channel on trade start. */
    private static final String TRADE_EXIST = "%b%c04[TRADE]%c12 A trade already exists between "
                                            + "%c04%rank%c12 and %c04%user%c12. Please use "
                                            + CONFIRMCMD + "%c12 to confirm it.";
    
    /** The message to the channel on no trade started. */
    private static final String TRADE_CXL = "%b%c04[TRADE]%c12 between "
                                          + "%c04%rank%c12 and %c04%user%c12 has been cancelled and"
                                          + " the %c04%chips %profile%c12 chips have been returned";
    
    /** The message to the channel on trade complete. */
    private static final String TRADECONF = "%b%c04[TRADE]%c12 A %c04%profile%c12 trade between "
                  + "%c04%rank%c12 and %c04%user%c12 for %c04%chips%c12 chips has been completed. ";
    
    /** The message to the channel on trade cancel failure. */
    private static final String TRADENOCXL = "%b%c04[TRADE]%c12 A %c04%user%c12 is the only person"
                                           + " that can cancel this trade!";
    
    /** The message to the channel on trade cancel failure. */
    private static final String TRADENOCNF = "%b%c04[TRADE]%c12 A %c04%user%c12 is the only person"
                                           + " that can confirm this trade!";
    
    /** The message to the channel on trade cancel failure. */
    private static final String TRADEDENY  = "%b%c04[TRADE]%c12 %c04%user%c12: This trade is denied"
                                           + " as the conversion rate is unreasonable! Please "
                                           + "choose a better conversion rate!";
    
    
    /** Message when the user doesn't have enough chips. */
    public static final String  NOCHIPSMSG    = "%b%c12Sorry, %c04%user%c12 does not have "
                                              + "%c04%chips%c12 chips available for the "
                                              + "%c04%profile%c12 profile.";
    
    /** Message when the user doesn't have enough chips. */
    public static final String  NOPLAYMSG    = "%b%c04%user%c12: Sorry, %c04play%c12 chips are not"
                                              + " valid in a swap or trade.";
    
    /** Message when the user doesn't have enough chips. */
    public static final String  NOSAMEPROF   = "%b%c04%user%c12: Sorry, you can only trade between"
                                             + "different profiles!";
    
    /** Message when the user trys to open too many swaps. */
    public static final String  TOOMANYSWAPS    = "%b%c04%user%c12: Sorry, You can only have"
                                              + " %c04two%c12 open swaps at a time.";

    /** Message when the user tries a negative swap. */
    public static final String  NONEGATIVES    = "%b%c04%user%c12: Sorry, You can't have"
                                                 + " %c04negative%c12 swaps.";
    
    /** Message when the user doesn't have enough chips. */
    public static final String  JOINMSG    = XMLLoader.getInstance().getTradeSetting("joinmsg");
    
    /** Used to limit the maximum conversion. */
    public static final Double  MAXCONV = Double.parseDouble(XMLLoader.getInstance()
                                                                      .getTradeSetting("maxconv"));
    /** Used to limit the maximum conversion default. */
    public static final Double  MAXCONVDEFAULT = 0.3;
    
    /** Used to convert to percent. */
    public static final int PERCENT = 100;
            
    /** Minutes between announcements. */
    private final int     announceDelay;
    
    /** The channel used for manager cancels. */
    private final String  managerChan;
    
    /**
     * Constructor.
     * 
     * @param bot    The irc bot.
     * @param channel The channel the game will run on
     * @param anochan The channel the announce will run on
     * @param anodelay the number of minutes between announcements.
     * @param mgrchan The channel the managers can cancel in
     * @param delay the number of minutes between announcements.
     */
    public Escrow(final IrcBot bot, final String channel,
                  final String anochan, final int anodelay,
                  final String mgrchan, final int delay) {
       announceDelay = delay;
       managerChan = mgrchan;
       Timer announce = new Timer(true);
       announce.scheduleAtFixedRate(new Announce(bot, channel, "", ""),
                                    Utils.MS_IN_MIN,
                                    announceDelay * Utils.MS_IN_MIN);
       Timer announce2 = new Timer(true);
       announce2.scheduleAtFixedRate(new Announce(bot, anochan,
                                           "To make any of these swaps please join %c04" + channel,
                                           "type %c04 " + AGREECMD + " %id %c12|"),
                                     Utils.MS_IN_MIN,
                                     anodelay * Utils.MS_IN_MIN);
    }
    
    /**
     * This method handles the on join event.
     * 
     * @param event The Join event.
     */
    @Override
    public final void join(final Join event) {
        User user = event.getUser();
        IrcBot bot = event.getBot();
        if (!user.getNick().equalsIgnoreCase(bot.getNick())
                && isValidChannel(event.getChannel().getName())) {
            String out = JOINMSG.replaceAll("%swpformat", SWAPFMT.replaceAll("%b%c12", ""));
            bot.addJoinMsg(new Pair(user, out));
        }
    }
    
    /**
     * This method handles the escrow commands.
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
            if (Utils.startsWith(message, TRADECMD) && bot.userIsHost(sender, chan.getName())) {
                sellChips(event);
            } else if (Utils.startsWith(message, CONFIRMCMD)) {
                confirm(event);
            } else if (Utils.startsWith(message, TRADECXLCMD)) {
                cancelTrade(event);
            } else if (Utils.startsWith(message, SWAPCMD)) {
                swapChips(event);
            } else if (Utils.startsWith(message, AGREECMD)) {
                agree(event);
            } else if (Utils.startsWith(message, SWAPCXLCMD)) {
                cancelSwap(event);
            }
        } else if (chan.getName().equalsIgnoreCase(managerChan)) {
            if (Utils.startsWith(message, TRADECXLCMD)) {
                mgrCancelTrade(event);
            } else if (Utils.startsWith(message, SWAPCXLCMD)) {
                cancelSwap(event);
            }
        }
    }
    
    /**
     * This method handles the sell chips command.
     * 
     * @param event the message event.
     */
    public final void sellChips(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");

        if (msg.length == TRADE_CMD_LEN) {
            String user = msg[1];
            Double amount = Utils.tryParseDbl(msg[2]);
            Double actamnt = amount;
            ProfileType profile = ProfileType.fromString(msg[TRADE_CMD_LEN - 1]);
            
            double chips = 0;
            Trade exists = null;
            try {
                exists = DB.getInstance().getTrade(sender, user);
                chips = DB.getInstance().checkCredits(sender, profile);
            } catch (Exception e) {
                EventLog.log(e, "Escrow", "sellChips");
            }                
            
            if (exists != null) {
                String out = TRADE_EXIST.replaceAll("%user", user)
                                        .replaceAll("%rank", sender);
                bot.sendIRCMessage(chan, out);
            } else if (amount != null && chips < amount) {
                String out = NOCHIPSMSG.replaceAll("%chips", Utils.chipsToString(amount));
                out = out.replaceAll("%profile", profile.toString());
                out = out.replaceAll("%user", sender);
                
                bot.sendIRCMessage(chan, out);
            } else if (profile == ProfileType.PLAY) {
                String out = NOPLAYMSG.replaceAll("%user", sender);
                bot.sendIRCMessage(chan, out);
            } else if (amount != null) {
                DB db = DB.getInstance();
                // positive means we are selling chips, negative buying them
                String fromuser = sender;
                String touser = user;
                if (amount < 0) {
                    fromuser = user;
                    touser = sender;
                    // received a negative amount so negate it so correct user loses chips
                    amount = -amount;
                }

                if (!bot.manualStatusRequest(user)) {
                    String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", user);
                    
                    bot.sendIRCMessage(senderu, out);
                } else if (profile == null) {
                    bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                } else {
                    boolean success = false;
                    try {
                        success = db.adjustChips(fromuser, -amount, profile,
                                                 GamesType.ADMIN, TransactionType.ESCROW);

                        db.addChipTransaction(fromuser, DB.HOUSE_USER,
                                              amount, TransactionType.ESCROW, profile);
                    } catch (Exception e) {
                        EventLog.log(e, "Escrow", "sellChips");
                    }

                    if (success) {
                        try {
                            db.addTrade(sender, user, actamnt, profile);
                            
                            String out = TRADE.replaceAll("%chips",
                                                          Utils.chipsToString(amount));
                            out = out.replaceAll("%user", touser);
                            out = out.replaceAll("%rank", fromuser);
                            out = out.replaceAll("%profile", profile.toString());
                            
                            bot.sendIRCMessage(chan, out);

                        } catch (SQLException e) {
                            EventLog.log(e, "Escrow", "sellChips");
                        }
                    } else {
                        EventLog.log(sender + ":database failed on trade", "Escrow", "sellChips");
                    }
                }
            } else {
                bot.invalidArguments(senderu, TRADEFMT);
            }                
        } else {
            bot.invalidArguments(senderu, TRADEFMT);
        }
    }
    
    /**
     * This method handles the confirm command.
     * 
     * @param event the Message event
     */
    public final void confirm(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");
        
        if (msg.length == 2) {
            String user = msg[1];
            Trade exists = null;
            try {
                exists = DB.getInstance().getTrade(sender.getNick(), user);
                if (exists == null) {
                    exists = DB.getInstance().getTrade(user, sender.getNick());
                    if (exists != null) {
                        exists = new Trade(exists.getUser(), exists.getRank(),
                                           exists.getProfile(), exists.getAmount());
                    }
                }
            } catch (Exception e) {
                EventLog.log(e, "Escrow", "confirm");
            }
            
            if (!bot.manualStatusRequest(user)) {
                String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", user);
                bot.sendIRCMessage(sender, out);
            } else if (exists == null) {
                String out = NO_TRADE.replaceAll("%user", user)
                                     .replaceAll("%rank", sender.getNick());
                bot.sendIRCMessage(chan, out);
            } else {
                String confirmer = exists.getRank();
                if (exists.getAmount() < 0) {
                    confirmer = exists.getUser();
                }
                if (sender.getNick().equalsIgnoreCase(confirmer)) {
                    DB db = DB.getInstance();
                    TradeResult res = null;
                    try {
                        res = db.tradeConfirmed(exists);
                    } catch (Exception e) {
                        EventLog.log(e, "Escrow", "confirm");
                    }
        
                    if (res != null) {
                        double amnt = res.getAmount();
                        ProfileType profile = res.getProfile();
                        
                        String out = TRADECONF.replaceAll("%chips", Utils.chipsToString(amnt));
                        out = out.replaceAll("%user", user);
                        out = out.replaceAll("%rank", sender.getNick());
                        out = out.replaceAll("%profile", profile.toString());
                        
                        bot.sendIRCMessage(chan, out);                            
                    } else {
                        EventLog.log(sender.getNick() + " attempted to confirm a trade "
                                     + "the database failed", "Escrow", "confirm");
                    }
                } else {
                    String out = TRADENOCNF.replaceAll("%user", confirmer);
                    bot.sendIRCMessage(chan, out);     
                }
            }
        } else {
            bot.invalidArguments(sender, CONFIRMFMT);
        }
    }
    
    /**
     * This method handles the confirm command.
     * 
     * @param event the Message event
     */
    public final void cancelTrade(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");
        
        if (msg.length == 2) {
            String user = msg[1];
            Trade exists = null;
            try {
                exists = DB.getInstance().getTrade(sender.getNick(), user);
                if (exists == null) {
                    exists = DB.getInstance().getTrade(user, sender.getNick());
                    if (exists != null) {
                        exists = new Trade(exists.getUser(), exists.getRank(),
                                           exists.getProfile(), exists.getAmount());
                    }
                }
            } catch (Exception e) {
                EventLog.log(e, "Escrow", "confirm");
            }
            
            if (exists == null) {
                String out = NO_TRADE.replaceAll("%user", user)
                                     .replaceAll("%rank", sender.getNick());
                bot.sendIRCMessage(chan, out);
            } else {
                String cxlr = exists.getRank();
                if (exists.getAmount() < 0) {
                    cxlr = exists.getUser();
                }
                
                if (sender.getNick().equalsIgnoreCase(cxlr)) {
                DB db = DB.getInstance();
                    try {
                        db.tradeCancel(exists);
                    } catch (Exception e) {
                        EventLog.log(e, "Escrow", "cancelTrade");
                    }
    
                    double amnt = exists.getAmount();
                    ProfileType profile = exists.getProfile();
                    
                    String out = TRADE_CXL.replaceAll("%chips", Utils.chipsToString(amnt));
                    out = out.replaceAll("%user", user);
                    out = out.replaceAll("%rank", sender.getNick());
                    out = out.replaceAll("%profile", profile.toString());
                    
                    bot.sendIRCMessage(chan, out);
                } else {
                    String out = TRADENOCXL.replaceAll("%user", cxlr);
                    bot.sendIRCMessage(chan, out);     
                }
            }
        } else {
            bot.invalidArguments(sender, TRADECXLFMT);
        }
    }
    
    /**
     * This method handles the confirm command.
     * 
     * @param event the Message event
     */
    public final void mgrCancelTrade(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");
        
        if (msg.length == 1 + 1 + 1) {
            String rank = msg[1];
            String user = msg[2];
            Trade exists = null;
            try {
                exists = DB.getInstance().getTrade(rank, user);
                if (exists == null) {
                    exists = DB.getInstance().getTrade(user, rank);
                    if (exists != null) {
                        // switch rank/user to correct position.
                        exists = new Trade(exists.getUser(), exists.getRank(),
                                           exists.getProfile(), exists.getAmount());
                    }
                }
            } catch (Exception e) {
                EventLog.log(e, "Escrow", "confirm");
            }
            
            if (exists == null) {
                String out = NO_TRADE.replaceAll("%user", user)
                                     .replaceAll("%rank", sender.getNick());
                bot.sendIRCMessage(chan, out);
            } else {
                DB db = DB.getInstance();
                try {
                    db.tradeCancel(exists);
                } catch (Exception e) {
                    EventLog.log(e, "Escrow", "cancelTrade");
                }

                double amnt = exists.getAmount();
                ProfileType profile = exists.getProfile();
                
                String out = TRADE_CXL.replaceAll("%chips", Utils.chipsToString(amnt));
                out = out.replaceAll("%user", user);
                out = out.replaceAll("%rank", sender.getNick());
                out = out.replaceAll("%profile", profile.toString());
                
                bot.sendIRCMessage(chan, out);
            }
        } else {
            bot.invalidArguments(sender, MGRTRADECXLFMT);
        }
    }
    
    /**
     * This method handles the confirm command.
     * 
     * @param event the Message event
     */
    public final void agree(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");
        Integer id = Integer.parseInt(msg[1]);
        if (msg.length == 2 && id != null) {
            Swap exists = null;
            double chips = 0;
            try {
                exists = DB.getInstance().getSwap(id);
            } catch (Exception e) {
                EventLog.log(e, "Escrow", "agree");
            }
            
            if (exists != null) {
                try {
                    chips = DB.getInstance().checkCredits(sender, exists.getWantedProfile());
                } catch (Exception e) {
                    EventLog.log(e, "Escrow", "agree");
                }
            }
            
            if (!bot.manualStatusRequest(sender)) {
                String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", sender);
                bot.sendIRCMessage(senderu, out);
            } else if (exists == null) {
                String out = NO_SWAP.replaceAll("%id", Integer.toString(id));
                bot.sendIRCMessage(chan, out);

            } else if (exists.getUser().equalsIgnoreCase(sender)) {
                String out = SELFTRADE.replaceAll("%who", sender);
                bot.sendIRCMessage(chan, out);
            } else if (chips < exists.getWantedAmount()) {
                String out = NOCHIPSMSG.replaceAll("%chips",
                                                 Utils.chipsToString(exists.getWantedAmount()));
                out = out.replaceAll("%profile", exists.getWantedProfile().toString());
                out = out.replaceAll("%user", sender);
                
                bot.sendIRCMessage(chan, out);
            } else {
                try {
                    DB.getInstance().completeSwap(exists, sender);
                    
                    String out = SWAPCOMP.replaceAll("%user2", sender);
                    out = out.replaceAll("%user", exists.getUser());
                    out = out.replaceAll("%amount",
                                         Utils.chipsToString(exists.getAmount()));
                    out = out.replaceAll("%profile", exists.getProfile().toString());
                    out = out.replaceAll("%user2", sender);
                    out = out.replaceAll("%wamount",
                                         Utils.chipsToString(exists.getWantedAmount()));
                    out = out.replaceAll("%wprofile", exists.getWantedProfile().toString());
                    out = out.replaceAll("%id", Integer.toString(exists.getId()));
                    
                    bot.sendIRCMessage(chan, out);
                } catch (SQLException e) {
                    EventLog.log(e, "Escrow", "agree");
                }
            }
        } else {
            bot.invalidArguments(senderu, AGREEFMT);
        }
    }

    /**
     * This method handles the sell chips command.
     * 
     * @param event the message event.
     */
    public final void swapChips(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");

        if (msg.length == SWAP_CMD_LEN) {
            Double amount = Utils.tryParseDbl(msg[1]);
            ProfileType profile = ProfileType.fromString(msg[2]);
            Double wamount = Utils.tryParseDbl(msg[SWAP_CMD_LEN - 2]);
            ProfileType wprofile = ProfileType.fromString(msg[SWAP_CMD_LEN - 1]);
            
            if (amount != null && wamount != null) {
                // positive means we are selling chips, negative buying them        
                DB db = DB.getInstance();        
                double chips = 0;                
                int swapcount = 0;
                List<Swap> openSwaps = null;
                try {
                    openSwaps = DB.getInstance().getAllSwaps();
                    for (Swap item: openSwaps) {
                        if (item.getUser().equalsIgnoreCase(sender)) {
                            swapcount++;
                        }
                    }
                    chips = db.checkCredits(sender, profile);
                    
                } catch (Exception e) {
                    EventLog.log(e, "Escrow", "swapChips");
                }
                
                double percdiff = (wamount - amount) / amount;
                double percdiff2 = (amount - wamount) / amount;
                double maxdiff = MAXCONVDEFAULT;
                if (MAXCONV != null) {
                    maxdiff = MAXCONV;
                }
                
                if (swapcount == 2) {
                    bot.sendIRCMessage(chan, TOOMANYSWAPS.replaceAll("%user", sender));
                } else if (percdiff > maxdiff || percdiff2 > maxdiff) {
                    String out = TRADEDENY.replaceAll("%user", sender);
                    out = out.replaceAll("%percent", Utils.chipsToString(PERCENT * maxdiff));
                    
                    bot.sendIRCMessage(chan, out);
                } else if (!bot.manualStatusRequest(sender)) {
                    String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", sender);
                    
                    bot.sendIRCMessage(senderu, out);
                } else if (profile == null || wprofile == null) {
                    bot.sendIRCMessage(chan, IrcBot.VALID_PROFILES);
                } else if (chips < amount) {
                    String out = NOCHIPSMSG.replaceAll("%chips", Utils.chipsToString(amount));
                    out = out.replaceAll("%profile", profile.toString());
                    out = out.replaceAll("%user", sender);
                    
                    bot.sendIRCMessage(chan, out);
                } else if (profile == ProfileType.PLAY) {
                    String out = NOPLAYMSG.replaceAll("%user", sender);
                    bot.sendIRCMessage(chan, out);
                } else if (wprofile == ProfileType.PLAY) {
                    String out = NOPLAYMSG.replaceAll("%user", sender);
                    bot.sendIRCMessage(chan, out);
                } else if (profile == wprofile) {
                    String out = NOSAMEPROF.replaceAll("%user", sender);
                    bot.sendIRCMessage(chan, out);
                } else if (amount < 0) {
                    String out = NONEGATIVES.replaceAll("%user", sender);
                    bot.sendIRCMessage(chan, out);
                } else if (wamount < 0) {
                    String out = NONEGATIVES.replaceAll("%user", sender);
                    bot.sendIRCMessage(chan, out);
                } else {
                    boolean success = false;
                    try {
                        success = db.adjustChips(sender, -amount, profile,
                                                 GamesType.ADMIN, TransactionType.SWAP);

                        db.adjustChips(DB.HOUSE_USER, amount, profile,
                                       GamesType.ADMIN, TransactionType.SWAP);
                        
                        db.addChipTransaction(sender, DB.HOUSE_USER,
                                              -amount, TransactionType.SWAP, profile);
                        
                        db.addChipTransaction(DB.HOUSE_USER, sender,
                                              amount, TransactionType.SWAP, profile);
                    } catch (Exception e) {
                        EventLog.log(e, "Escrow", "swapChips");
                    }

                    if (success) {
                        try {
                            int id = db.addSwap(sender, amount, profile, wamount, wprofile);
                            
                            String out = SWAP.replaceAll("%id", Integer.toString(id));
                            out = out.replaceAll("%user", sender);
                            out = out.replaceAll("%amount", Utils.chipsToString(amount));
                            out = out.replaceAll("%profile", profile.toString());
                            out = out.replaceAll("%wamount", Utils.chipsToString(wamount));
                            out = out.replaceAll("%wprofile", wprofile.toString());
                            
                            bot.sendIRCMessage(chan, out);
                        } catch (SQLException e) {
                            EventLog.log(e, "Escrow", "swapChips");
                        }
                    } else {
                        EventLog.log(sender + ":database failed on swap", "Escrow", "swapChips");
                    }
                }
            } else {
                bot.invalidArguments(senderu, SWAPFMT);
            }                
        } else {
            bot.invalidArguments(senderu, SWAPFMT);
        }
    }
    
    /**
     * This method handles the cancel swap command.
     * 
     * @param event the Message event
     */
    public final void cancelSwap(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User senderu = event.getUser();
        String sender = senderu.getNick();
        Channel chan = event.getChannel();

        String[] msg = message.split(" ");
        Integer id = Integer.parseInt(msg[1]);
        if (msg.length == 2 && id != null) {
            Swap exists = null;
            
            try {
                exists = DB.getInstance().getSwap(id);
            } catch (Exception e) {
                EventLog.log(e, "Escrow", "agree");
            }
            
            if (!bot.manualStatusRequest(sender)) {
                String out = CheckIdentified.NOT_IDENTIFIED.replaceAll("%user", sender);
                bot.sendIRCMessage(senderu, out);
            } else if (exists == null) {
                String out = NO_SWAP.replaceAll("%id", Integer.toString(id));
                bot.sendIRCMessage(chan, out);
            } else if (!chan.getName().equals(managerChan)
                       && !exists.getUser().equalsIgnoreCase(sender)) {
                String out = NO_CXL_SWAP.replaceAll("%who", exists.getUser())
                                        .replaceAll("%id", Integer.toString(exists.getId()));
                bot.sendIRCMessage(chan, out);
            } else {
                try {
                    DB.getInstance().cancelSwap(exists, sender);
                    
                    String out = CXL_SWAP.replaceAll("%id", Integer.toString(exists.getId()));
                    
                    bot.sendIRCMessage(chan, out);
                } catch (SQLException e) {
                    EventLog.log(e, "Escrow", "cancelSwap");
                }
            }
        } else {
            bot.invalidArguments(senderu, SWAPCXLFMT);
        }
    }
    
    /**
     * Simple extension to time task to deal with game triggers.
     * 
     * @author jamie
     */
    class Announce extends TimerTask {
        /** The IRC bot. */
        private final IrcBot irc;
        
        /** The IRC channel. */
        private final String channel;
        
        /** Extra message. */
        private final String extra;
        
        /** Remove message. */
        private final String remove;

        /**
         * Constructor.
         * @param ib   The irc bot/server.
         * @param chan The channel.
         * @param ext  Extra text to add.
         * @param rem Text to remove.
         */
        public Announce(final IrcBot ib, final String chan, final String ext, final String rem) {
            irc = ib;
            channel = chan;
            extra = ext;
            remove = rem;
        }

        @Override
        public void run() {
            List<Swap> openSwaps = null;
            List<Swap> cxlSwaps = new ArrayList<Swap>();
            try {
                openSwaps = DB.getInstance().getAllSwaps();
            } catch (SQLException e) {
                EventLog.log(e, "Escrow", "Announce->run");                
            }
            
            if (openSwaps != null && openSwaps.size() > 0) {
                Channel chan = irc.getUserChannelDao().getChannel(channel);
                String swapstr = "";
                int swapcount = 0;

                for (Swap swp: openSwaps) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(swp.getTimestamp());
                    cal.add(Calendar.DATE, 1);
                    Date date = cal.getTime();
                    if (date.before(new Date())) {
                        cxlSwaps.add(swp);
                    }
                }
                
                for (Swap swp: cxlSwaps) {
                    openSwaps.remove(swp);
                }
                
                for (Swap swp: openSwaps) {
                    if (swapcount == 0) {
                        swapstr += OPEN_SWAPS + OPEN_SWAP;
                    } else if (swapcount < SWAP_PER_LINE) {
                        swapstr += OPEN_SWAP;
                    }
                    if (!remove.equals("")) {
                        swapstr = swapstr.replaceAll(remove, "");
                    }
                                        
                    swapstr = swapstr.replaceAll("%id", Integer.toString(swp.getId()));
                    swapstr = swapstr.replaceAll("%user", swp.getUser());
                    swapstr = swapstr.replaceAll("%amount", Utils.chipsToString(swp.getAmount()));
                    swapstr = swapstr.replaceAll("%profile", swp.getProfile().toString());
                    swapstr = swapstr.replaceAll("%wamount",
                                                 Utils.chipsToString(swp.getWantedAmount()));
                    swapstr = swapstr.replaceAll("%wprofile", swp.getWantedProfile().toString());

                    swapcount++;
                    if (swapcount == SWAP_PER_LINE) {
                        swapstr = swapstr + extra + "\n";
                        swapcount = 0;
                    }
                }
                irc.sendIRCMessage(chan, swapstr);
                
                for (Swap swp: cxlSwaps) {
                    try {
                        DB.getInstance().cancelSwap(swp, swp.getUser());
                    } catch (SQLException e) {
                        EventLog.log(e, "Escrow", "Announce->run");                
                    }
                    
                    String out = CXL_SWAP.replaceAll("%id", Integer.toString(swp.getId()));
                    
                    irc.sendIRCMessage(chan, out);
                }
            }
        }
    }
}
