/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.game.rooms;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.pircbotx.Channel;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.poker.Client;
import org.smokinmils.games.casino.poker.enums.CommandType;
import org.smokinmils.games.casino.poker.enums.RoomType;
import org.smokinmils.logging.EventLog;
import org.smokinmils.settings.PokerStrs;
import org.smokinmils.settings.PokerVars;

/**
 * A class to handle the lobby of the poker game.
 * 
 * @author Jamie
 */
public class Lobby extends Room {
    /** A timer to announce poker to the channel. */
    private final Timer announceTimer;

    /** A task for the announcements. */
    class AnnounceTask extends TimerTask {
        @Override
        public void run() {
            getIrcClient().getBot().sendIRCMessage(
                    getIrcChannel(), PokerStrs.PokerAnnounce);
            announceTimer.schedule(
                    new AnnounceTask(),
                    PokerVars.ANNOUNCEMINS * Utils.MS_IN_MIN);
        }
    }

    /**
     * Constructor.
     * 
     * @param channel The channel to announce on.
     * @param irc The client to announce with.
     */
    public Lobby(final Channel channel, final Client irc) {
        super(channel, irc, RoomType.LOBBY);
        setRoomTopic(PokerStrs.LobbyTopic);
        announceTimer = new Timer(true);
        announceTimer.schedule(new AnnounceTask(), Utils.MS_IN_MIN);
    }

    /**
     * This method is called whenever a message is sent to this channel.
     * 
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param msg The actual message sent to the channel.
     */
    @Override
    protected final void onMessage(final String sender,
                                   final String login,
                                   final String hostname,
                                   final String msg) {
        String message = msg;
        int fSpace = message.indexOf(" ");
        if (fSpace == -1) {
            fSpace = message.length();
        }
        String firstWord = message.substring(0, fSpace).toLowerCase();

        if ((fSpace + 1) <= message.length()) {
            fSpace++;
        }
        message = message.substring(fSpace, message.length());

        CommandType cmd = CommandType.fromString(firstWord);
        if (cmd != null) {
            switch (cmd) {
            case INFO:
                onInfo(sender, login, hostname, message);
                break;
            case NEWTABLE:
                onNewTable(sender, login, hostname, message);
                break;
            case WATCHTBL:
                onWatchTable(sender, login, hostname, message);
                break;
            case TABLES:
                onTables(sender, login, hostname, message);
                break;
            case JOIN:
                onJoinTable(sender, login, hostname, message);
                break;
            default:
                // no action
                break;
            }
        }
    }

    /**
     * This method is called whenever we receive a notice to this channel.
     * 
     * @param sourceNick The nick of the user that sent the notice.
     * @param sourceLogin The login of the user that sent the notice.
     * @param sourceHostname The hostname of the user that sent the notice.
     * @param notice The notice message.
     */
    @Override
    protected void onNotice(final String sourceNick,
                            final String sourceLogin,
                            final String sourceHostname,
                            final String notice) {

    }

    /**
     * This method is called whenever someone joins a channel which we are on.
     * 
     * @param sender The nick of the user who joined the channel.
     * @param login The login of the user who joined the channel.
     * @param hostname The hostname of the user who joined the channel.
     */
    @Override
    protected void onJoin(final String sender,
                          final String login,
                          final String hostname) {

    }

    /**
     * This method is called whenever someone parts this channel which we are
     * on. This is also the handler for whenever someone quits from the channel
     * 
     * @param sender The nick of the user who parted the channel.
     * @param login The login of the user who parted from the channel.
     * @param hostname The hostname of the user who parted from the channel.
     */
    @Override
    protected void onPart(final String sender,
                          final String login,
                          final String hostname) {

    }

    /**
     * This method is called whenever someone changes nick on this channel.
     * 
     * @param oldNick The old nick.
     * @param login The login of the user.
     * @param hostname The hostname of the user.
     * @param newNick The new nick.
     */
    @Override
    protected void onNickChange(final String oldNick,
                                final String login,
                                final String hostname,
                                final String newNick) {

    }

    /**
     * This method handles the info command.
     * 
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private void onInfo(final String sender,
                        final String login,
                        final String hostname,
                        final String message) {
        String[] msg = message.split(" ");
        if (msg.length == 0 || msg[0].compareTo("") == 0) {
            for (String line : PokerStrs.InfoMessage.split("\n")) {
                getIrcClient().sendIRCNotice(sender, line);
            }
        } else if (msg.length == 1) {
            CommandType infocmd = CommandType.fromString(PokerStrs.CommandChar
                    + msg[0]);
            if (infocmd != null) {
                sendFullCommand(sender, infocmd);
            } else if (msg[0].compareToIgnoreCase("table") == 0) {
                CommandType[] tcmds = { CommandType.CHECK,
                        CommandType.RAISE, CommandType.FOLD,
                        CommandType.TBLCHIPS, CommandType.REBUY,
                        CommandType.LEAVE, CommandType.SITDOWN,
                        CommandType.SITOUT };
                for (CommandType item : tcmds) {
                    sendFullCommand(sender, item);
                }
            } else if (msg[0].compareToIgnoreCase("lobby") == 0) {
                CommandType[] lcmds = { CommandType.INFO,
                        CommandType.TABLES, CommandType.NEWTABLE,
                        CommandType.WATCHTBL,
                        CommandType.JOIN };
                for (CommandType item : lcmds) {
                    sendFullCommand(sender, item);
                    getIrcClient().sendIRCNotice(sender, "%b%c15-----");
                }
            } else {
                getIrcClient().sendIRCNotice(sender, PokerStrs.InvalidInfoArgs
                        .replaceAll("%invalid", msg[0]));
            }
        } else {
            invalidArguments(sender, CommandType.INFO.getFormat());
        }
    }

    /**
     * This method handles the new table command.
     * 
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private void onTables(final String sender,
                          final String login,
                          final String hostname,
                          final String message) {
        String[] msg = message.split(" ");
        Map<Integer, Integer> tables = new HashMap<Integer, Integer>(
                Table.getTables());
        if (msg.length == 0 || msg[0].compareTo("") == 0) {
            getIrcClient().sendIRCNotice(
                    sender,
                    PokerStrs.AllTablesMsg.replaceAll(
                            "%count", Integer.toString(tables.size())));
            for (Entry<Integer, Integer> table : tables.entrySet()) {
                Table tbl = getIrcClient().getTable(table.getKey());
                getIrcClient().sendIRCNotice(
                        sender, tbl.formatTableInfo(PokerStrs.TableInfoMsg));
            }
        } else if (msg.length == 1) {
            Vector<Integer> tids = new Vector<Integer>();
            Integer stake = Utils.tryParse(msg[0]);

            Integer tblstke = null;
            for (Entry<Integer, Integer> table : tables.entrySet()) {
                tblstke = table.getValue();
                if (tblstke.compareTo(stake) == 0) {
                    tids.add(table.getKey());
                }
            }

            if (tids.size() == 0) {
                getIrcClient().sendIRCNotice(sender,
                        PokerStrs.NoTablesMsg.replaceAll("%bb", msg[0]));
            } else {
                String out = PokerStrs.FoundTablesMsg.replaceAll("%bb", msg[0]);
                out = out.replaceAll(
                        "%count", Integer.toString(tids.size()));
                out = out.replaceAll("%tables", tids.toString());

                getIrcClient().sendIRCNotice(sender, out);

                for (Integer id : tids) {
                    Table tbl = getIrcClient().getTable(id);
                    getIrcClient().sendIRCNotice(sender,
                            tbl.formatTableInfo(PokerStrs.TableInfoMsg));
                }
            }
        } else {
            invalidArguments(sender, CommandType.TABLES.getFormat());
        }
    }

    /**
     * This method handles the info command.
     * 
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private void onNewTable(final String sender,
                            final String login,
                            final String hostname,
                            final String message) {
        String[] msg = message.split(" ");
        if (getIrcClient().getBot().userIsHost(
                getIrcClient().getBot().getUser(sender),
                getIrcChannel().getName())) {
            int cmdlen = 1 + 1 + 1 + 1;
            if (msg.length == cmdlen - 1 || msg.length == cmdlen) {
                ProfileType profile = null;
                if (msg.length == cmdlen) {
                    profile = ProfileType.fromString(msg[cmdlen - 1]);
                } else {
                    try {
                        profile = DB.getInstance().getActiveProfile(sender);
                    } catch (Exception e) {
                        EventLog.log(e, "Lobby", "onNewTable");
                    }
                }

                Integer stake = Utils.tryParse(msg[0]);
                Integer buyin = Utils.tryParse(msg[1]);
                Integer maxplyrs = Utils.tryParse(msg[2]);
                if (stake != null && buyin != null && maxplyrs != null) {
                    // Verify stake is between correct buy-in levels
                    int maxbuy = (stake * PokerVars.MAXBUYIN);
                    int minbuy = (stake * PokerVars.MINBUYIN);

                    if (profile == null) {
                        getIrcClient().getBot().sendIRCNotice(
                                sender, IrcBot.VALID_PROFILES);
                    } else if (!validPlayers(maxplyrs)) {
                        String out = PokerStrs.InvalidTableSizeMsg.replaceAll(
                                "%size", Integer.toString(maxplyrs));
                        out = out.replaceAll(
                                "%allowed",
                                Arrays.toString(PokerVars.ALLOWEDTBLSIZES));
                        getIrcClient().getBot().sendIRCNotice(sender, out);
                    } else if (!validStake(stake)) {
                        String out = PokerStrs.InvalidTableBBMsg.replaceAll(
                                "%bb", Integer.toString(stake));
                        out = out.replaceAll(
                                "%allowed",
                                Arrays.toString(PokerVars.ALLOWEDBB));
                        getIrcClient().getBot().sendIRCNotice(sender, out);
                    } else if (buyin < minbuy || buyin > maxbuy) {
                        String out = PokerStrs.IncorrectBuyInMsg.replaceAll(
                                "%buyin", Integer.toString(buyin));
                        out = out.replaceAll(
                                "%maxbuy", Integer.toString(maxbuy));
                        out = out.replaceAll(
                                "%minbuy", Integer.toString(minbuy));
                        out = out.replaceAll(
                                "%maxBB", Integer.toString(PokerVars.MAXBUYIN));
                        out = out.replaceAll(
                                "%minBB", Integer.toString(PokerVars.MINBUYIN));
                        getIrcClient().getBot().sendIRCNotice(sender, out);
                    } else if (!getIrcClient().userHasCredits(
                            sender, buyin, profile)) {
                        String out = PokerStrs.NoChipsMsg.replaceAll(
                                "%chips", Integer.toString(buyin));
                        out = out.replaceAll("%profile", profile.toString());
                        getIrcClient().getBot().sendIRCNotice(sender, out);
                    } else {
                        getIrcClient().newTable(
                                stake, maxplyrs, profile, true);
                    }
                } else {
                    invalidArguments(sender, CommandType.NEWTABLE.getFormat());
                }
            } else {
                invalidArguments(sender, CommandType.NEWTABLE.getFormat());
            }
        }
    }

    /**
     * This method handles the watch command.
     * 
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private void onWatchTable(final String sender,
                              final String login,
                              final String hostname,
                              final String message) {
        String[] msg = message.split(" ");
        if (msg.length == 1 && msg[0].compareTo("") != 0) {
            Integer id = Utils.tryParse(msg[0]);
            if (Table.getTables().containsKey(id)) {
                Table table = getIrcClient().getTable(id);
                if (table != null && table.canWatch(sender)) {
                    getIrcClient().newObserver(sender, id);
                } else {
                    getIrcClient().sendIRCNotice(
                            sender, PokerStrs.AlreadyWatchingMsg.replaceAll(
                                    "%id", Integer.toString(id)));
                }
            } else {
                getIrcClient().getBot()
                        .sendIRCMessage(
                                getIrcChannel(),
                                PokerStrs.NoTableIDMsg.replaceAll(
                                        "%id", id.toString()));
            }
        } else {
            invalidArguments(sender, CommandType.WATCHTBL.getFormat());
        }
    }

    /**
     * This method handles the join command.
     * 
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     */
    private void onJoinTable(final String sender,
                             final String login,
                             final String hostname,
                             final String message) {
        String[] msg = message.split(" ");
        Integer tableid = null;
        Integer buyin = null;
        if ((msg.length == 1 && msg[0].compareTo("") != 0) || msg.length == 2) {
            if (msg.length == 2) {
                tableid = Utils.tryParse(msg[0]);
                buyin = Utils.tryParse(msg[1]);
            } else {
                tableid = Utils.tryParse(msg[0]);
                if (tableid != null) {
                    Integer stake = Table.getTables().get(tableid);
                    if (stake != null) {
                        buyin = stake * PokerVars.MINBUYIN;
                    } else {
                        buyin = null;
                    }
                }
            }

            if (tableid == null || buyin == null) {
                invalidArguments(sender, CommandType.JOIN.getFormat());
            } else if (!Table.getTables().containsKey(tableid)) {
                getIrcClient().getBot().sendIRCMessage(
                        getIrcChannel(),
                        PokerStrs.NoTableIDMsg.replaceAll(
                                "%id", tableid.toString()));
            } else {
                Table table = getIrcClient().getTable(tableid);
                ProfileType profile = table.getProfile();
                int stake = Table.getTables().get(tableid);
                int maxbuy = (stake * PokerVars.MAXBUYIN);
                int minbuy = (stake * PokerVars.MINBUYIN);

                if (buyin < minbuy || buyin > maxbuy) {
                    String out = PokerStrs.IncorrectBuyInMsg.replaceAll(
                            "%buyin", Integer.toString(buyin));
                    out = out.replaceAll("%maxbuy", Integer.toString(maxbuy));
                    out = out.replaceAll("%minbuy", Integer.toString(minbuy));
                    out = out.replaceAll(
                            "%maxBB", Integer.toString(PokerVars.MAXBUYIN));
                    out = out.replaceAll(
                            "%minBB", Integer.toString(PokerVars.MINBUYIN));
                    getIrcClient().sendIRCNotice(sender, out);
                } else if (getIrcClient().tableIsFull(tableid)) {
                    getIrcClient().sendIRCNotice(
                            sender,
                            PokerStrs.TableFullMsg.replaceAll(
                                    "%id", Integer.toString(tableid)));
                } else if (!getIrcClient().userHasCredits(
                        sender, buyin, profile)) {
                    String out = PokerStrs.NoChipsMsg.replaceAll(
                            "%chips", Integer.toString(buyin));
                    out = out.replaceAll("%profile", profile.toString());
                    getIrcClient().sendIRCNotice(sender, out);
                } else {
                    if (table != null && table.canPlay(sender)) {
                        getIrcClient().newPlayer(sender, tableid, buyin);
                    } else {
                        getIrcClient().sendIRCNotice(
                                sender, PokerStrs.AlreadyPlayingMsg.replaceAll(
                                        "%id", Integer.toString(tableid)));
                    }
                }
            }
        } else {
            invalidArguments(sender, CommandType.JOIN.getFormat());
        }
    }

    /**
     * This method is used to check the supplied stake is an allowed big blind.
     * 
     * @param stake The big blind to check
     * 
     * @return true if the supplied value is allowed
     */
    private boolean validStake(final int stake) {
        return Arrays.asList(PokerVars.ALLOWEDBB).contains(stake);
    }

    /**
     * This method is used to check the supplied stake is an allowed table size.
     * 
     * @param players The number of players to check
     * 
     * @return true if the supplied value is allowed
     */
    private boolean validPlayers(final int players) {
        return Arrays.asList(PokerVars.ALLOWEDTBLSIZES).contains(players);
    }
}
