/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokingmils.help;

import java.util.ArrayList;
import java.util.List;

import org.smokinmils.poker.enums.CommandType;
import org.smokinmils.poker.settings.Strings;

public class Command {
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
    private Command(String text, String desc, String fmt) {
    	this(text, desc, fmt, new String[]{});
    }
    
	/**
	 * Constructor
	 * @param text	 The text used to call this command
	 * @param desc	 A description of the command
	 * @param fmt	 The format for the command
	 * @param alts	 Alternate commands that will also work
	 */
    private Command(String text, String desc, String fmt, String[] alts) {
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
    
    public static Command fromString(String text) {
        if (text != null) {
        	text = text.toLowerCase();
        	for (Command cmd : Command.values()) {
        		if ( cmd.getAlternatives().contains(text) ) {
        			return cmd;
        		}
        	}
        }
        return null;
    }
}
