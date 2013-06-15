/**
 * This file is part of the 'pokerbot' a commercial IRC bot that allows users to
 * play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.casino.poker.enums;

import java.util.ArrayList;
import java.util.List;

import org.smokinmils.settings.PokerStrs;

/**
 * All the possible commands the bot accepts.
 * 
 * @author Jamie Reid
 */
public enum CommandType {
    /** lobby info command. */
    INFO(PokerStrs.InfoCommand, PokerStrs.InfoCommand_Desc,
            PokerStrs.InfoCommand_Format),

    /** lobby new table command. */
    NEWTABLE(PokerStrs.NewTablCommand, PokerStrs.NewTablCommand_Desc,
            PokerStrs.NewTablCommand_Format),

    /** lobby watch table command. */
    WATCHTBL(PokerStrs.WatchTlCommand, PokerStrs.WatchTlCommand_Desc,
            PokerStrs.WatchTlCommand_Format),

    /** lobby info command. */
    TABLES(PokerStrs.TablesCommand, PokerStrs.TablesCommand_Desc,
            PokerStrs.TablesCommand_Format),
    /** lobby info command. */
    JOIN(PokerStrs.JoinCommand, PokerStrs.JoinCommand_Desc,
            PokerStrs.JoinCommand_Format),

    /** Table check command. */
    CHECK(PokerStrs.CheckCommand, PokerStrs.CheckCommand_Desc,
            PokerStrs.CheckCommand_Format,
            PokerStrs.CheckCommand_Alternatives),

    /** Table bet command. */
    RAISE(PokerStrs.BetCommand, PokerStrs.BetCommand_Desc,
            PokerStrs.BetCommand_Format,
            PokerStrs.BetCommand_Alternatives),

    /** Table fold command. */
    FOLD(PokerStrs.FoldCommand, PokerStrs.FoldCommand_Desc,
            PokerStrs.FoldCommand_Format,
            PokerStrs.FoldCommand_Alternatives),

    /** Table show command. */
    SHOW(PokerStrs.ShowCommand, PokerStrs.ShowCommand_Desc,
            PokerStrs.ShowCommand_Format,
            PokerStrs.ShowCommand_Alternatives),

    /** Table chips command. */
    TBLCHIPS(PokerStrs.TblChipsCommand, PokerStrs.TblChipsCommand_Desc,
            PokerStrs.TblChipsCommand_Format),

    /** Table rebuy command. */
    REBUY(PokerStrs.RebuyCommand, PokerStrs.RebuyCommand_Desc,
            PokerStrs.RebuyCommand_Format),

    /** Table sitdown command. */
    SITDOWN(PokerStrs.SitDownCommand, PokerStrs.SitDownCommand_Desc,
            PokerStrs.SitDownCommand_Format),

    /** Table sitout command. */
    SITOUT(PokerStrs.SitOutCommand, PokerStrs.SitOutCommand_Desc,
            PokerStrs.SitOutCommand_Format),

    /** Table leave command. */
    LEAVE(PokerStrs.LeaveCommand, PokerStrs.LeaveCommand_Desc,
            PokerStrs.LeaveCommand_Format);

    /** The command text. */
    private String       cmdText;

    /** The command description. */
    private String       description;

    /** The command format. */
    private String       format;

    /** The command alternatives. */
    private List<String> alternatives;

    /**
     * Constructor.
     * 
     * @param text The text used to call this command
     * @param desc A description of the command
     * @param fmt The format for the command
     */
    private CommandType(final String text, final String desc,
            final String fmt) {
        this(text, desc, fmt, new String[] {});
    }

    /**
     * Constructor.
     * 
     * @param text The text used to call this command
     * @param desc A description of the command
     * @param fmt The format for the command
     * @param alts Alternate commands that will also work
     */
    private CommandType(final String text, final String desc, final String fmt,
            final String[] alts) {

        cmdText = text;
        description = desc;
        format = fmt;

        alternatives = new ArrayList<String>();
        alternatives.add(cmdText.toLowerCase());
        for (int i = 0; i < alts.length; i++) {
            alternatives.add(alts[i].toLowerCase());
        }
    }

    /**
     * @return the command text.
     */
    public String getCommandText() {
        return cmdText;
    }

    /**
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the command format.
     */
    public String getFormat() {
        return format;
    }

    /**
     * @return the list of alternatives.
     */
    public List<String> getAlternatives() {
        return alternatives;
    }

    /**
     * Converts a String to a CommandType.
     * 
     * @param text The string to convert
     * @return the new commandtype or null.
     */
    public static CommandType fromString(final String text) {
        if (text != null) {
            String txt = text.toLowerCase();
            for (CommandType cmd : CommandType.values()) {
                if (cmd.getAlternatives().contains(txt)) {
                    return cmd;
                }
            }
        }
        return null;
    }
}
