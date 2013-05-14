/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils;

import java.util.List;

/**
 * A utility class that provides useful functions to the entire project
 * 
 * @author Jamie Reid
 */
public class Utils {
	
	/*
	 * A method that will handle parsing of integers without throwing an exception
	 * 
	 * @param text The input string
	 * 
	 * @return The resulting integer
	 */
	public static Integer tryParse(String text) {
		try {
			return new Integer(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	public static <E> String ListToString(List<E> list)
	        throws IllegalArgumentException {
        String out = "";
        int i = 0;
        for (E item: list) {
            if (item == null) out += "NULL";
            else out += item.toString();
            
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
}
