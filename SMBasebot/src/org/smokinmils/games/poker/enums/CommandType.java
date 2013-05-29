/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */
package org.smokinmils.games.poker.enums;

import java.util.ArrayList;
import java.util.List;

import org.smokinmils.settings.poker.Strings;

/**
 * All the possible commands the bot accepts
 * 
 * @author Jamie Reid
 */
public enum CommandType {
	/** Lobby commands*/
	INFO	(Strings.InfoCommand, 	 Strings.InfoCommand_Desc, 	  Strings.InfoCommand_Format),
	NEWTABLE(Strings.NewTablCommand, Strings.NewTablCommand_Desc, Strings.NewTablCommand_Format),
	WATCHTBL(Strings.WatchTlCommand, Strings.WatchTlCommand_Desc, Strings.WatchTlCommand_Format),
	TABLES	(Strings.TablesCommand,  Strings.TablesCommand_Desc,  Strings.TablesCommand_Format),
	JOIN	(Strings.JoinCommand, 	 Strings.JoinCommand_Desc,    Strings.JoinCommand_Format),
	
	/** Table commands*/
	CHECK	(Strings.CheckCommand,   Strings.CheckCommand_Desc,   Strings.CheckCommand_Format,
	 		 Strings.CheckCommand_Alternatives),
	RAISE	(Strings.BetCommand,  	 Strings.BetCommand_Desc,	  Strings.BetCommand_Format,
	 		 Strings.BetCommand_Alternatives),
	FOLD 	(Strings.FoldCommand,    Strings.FoldCommand_Desc,    Strings.FoldCommand_Format,
			 Strings.FoldCommand_Alternatives),
	SHOW 	(Strings.ShowCommand,    Strings.ShowCommand_Desc,    Strings.ShowCommand_Format,
			 Strings.ShowCommand_Alternatives),
	TBLCHIPS(Strings.TblChipsCommand,Strings.TblChipsCommand_Desc,Strings.TblChipsCommand_Format),
	REBUY	(Strings.RebuyCommand,   Strings.RebuyCommand_Desc,   Strings.RebuyCommand_Format),
	SITDOWN (Strings.SitDownCommand, Strings.SitDownCommand_Desc, Strings.SitDownCommand_Format),
	SITOUT	(Strings.SitOutCommand,  Strings.SitOutCommand_Desc,  Strings.SitOutCommand_Format),
	LEAVE	(Strings.LeaveCommand,   Strings.LeaveCommand_Desc,   Strings.LeaveCommand_Format);
	
	private String cmdText;
	private String description;
	private String format;
	private List<String> alternatives;
	
	/**
	 * Constructor
	 * @param text	 The text used to call this command
	 * @param desc	 A description of the command
	 * @param fmt	 The format for the command
	 */
    private CommandType(String text, String desc, String fmt) {
    	this(text, desc, fmt, new String[]{});
    }
    
	/**
	 * Constructor
	 * @param text	 The text used to call this command
	 * @param desc	 A description of the command
	 * @param fmt	 The format for the command
	 * @param alts	 Alternate commands that will also work
	 */
    private CommandType(String text, String desc, String fmt, String[] alts) {
    	
        cmdText = text;
        description = desc;
        format = fmt;
        
        alternatives = new ArrayList<String>();
        alternatives.add(cmdText.toLowerCase());
        for (int i = 0; i < alts.length; i++) {
        	alternatives.add(alts[i].toLowerCase());
        }
    }
    
    public String getCommandText() { return cmdText; } 
    public String getDescription() { return description; }    
    public String getFormat() { return format; }
    public List<String> getAlternatives() { return alternatives; }
    
    public static CommandType fromString(String text) {
        if (text != null) {
        	text = text.toLowerCase();
        	for (CommandType cmd : CommandType.values()) {
        		if ( cmd.getAlternatives().contains(text) ) {
        			return cmd;
        		}
        	}
        }
        return null;
    }
}