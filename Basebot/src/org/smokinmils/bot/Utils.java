/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.bot;

import java.text.DecimalFormat;
import java.util.List;

/**
 * A utility class that provides useful functions to the entire project.
 * 
 * @author Jamie Reid
 */
public final class Utils {
    /**
     * Hiding the default constructor.
     */
    private Utils() { }
    
	/**
	 * A method that will handle parsing of integers without throwing
	 * an exception.
	 * 
	 * @param text The input string
	 * 
	 * @return The resulting integer
	 */
	public static Integer tryParse(final String text) {
		try {
			return new Integer(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	/**
     * A method that will handle parsing of doubles without throwing
     * an exception.
     * 
     * @param text The input string
     * 
     * @return The resulting double
     */
    public static Double tryParseDbl(final String text) {
        try {
            return new Double(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
	
    /**
     * Converts a list of objects to a string included ',' and 'and' correctly.
     * 
     * @param <E>  The object type.
     * @param list The list of objects
     * 
     * @return     The output string
     */
	public static <E> String listToString(final List<E> list) {
        String out = "";
        int i = 0;
        for (E item: list) {
            if (item == null) {
                out += "NULL";
            } else {
                out += item.toString();
            }
            
            if (list.size() > 1) {
                if (i == (list.size() - 2)) {
                    out += " and ";
                } else if (i < (list.size() - 2)) {
                    out += ", ";
                }
            }
            i++;                    
        }
        return out;
	}
	
	/**
	 * Formats chips as a String with the correct number of decimal points.
	 * 
	 * @param chips The amount of chips to format
	 * 
	 * @return A string representing the chips
	 */
	public static String chipsToString(final double chips) {
	    DecimalFormat df = new DecimalFormat("#.#");
	    return df.format(chips);
	}
	
	/**
	 * Checks if the first word of a string matches the provided word.
	 * 
	 * Note: this is case insensitive
	 * 
	 * @param message  The entire message
	 * @param start    The word the first word should match
	 * 
	 * @return true if the word matches, false otherwise.
	 */
	public static boolean startsWith(final String message, final String start) {
	    String[] msg = message.split(" ");
	    boolean ret = false;
	    if (msg.length >= 1 && msg[0].equalsIgnoreCase(start)) {
            ret = true;
        }
	    return ret;
	}
}
