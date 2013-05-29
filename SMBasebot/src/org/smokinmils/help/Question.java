/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.help;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.smokinmils.logging.EventLog;

public class Question {
	private static Map<Integer, Question> _allQuestions = new TreeMap<Integer, Question>();
	private String topic;
	private String question;
	private String answer;
	
	/**
	 * Constructor
	 * @param arg	 The text used to call this question
	 * @param q	 	The question text
	 * @param a	 	The answer for this question
	 */
    private Question(String t, String q, String a, int i) {
        topic = t;
        question = q;
        answer = a;
    }
    
    public String getTopic() { return topic; } 
    public String getQuestion() { return question; }    
    public String getAnswer() { return answer; }
    public static Map<Integer, Question> values() { return _allQuestions; }
    
    public static Question fromString(String text) {
    	Question ret = null;
    	for (Question q: _allQuestions.values() ) {
    		if ( q.getTopic().equalsIgnoreCase(text) ) {
    			ret = q;
    			break;
    		}
    	}
    		
    	return ret;
    }
    
    public static void load(String filename) throws InvalidFileFormatException,
    								 				FileNotFoundException,
    								 				IOException {
		Ini ini = new Ini( new FileReader( filename ) );
    	for (String name: ini.keySet()) {
    		Section section = ini.get(name);
    		String q = section.get("question");
    		String a = section.get("answer");
    		Integer i = section.get("i", Integer.class);
    		if (i == null) i = _allQuestions.size();
    		if (q == null) {
    			EventLog.log(name + " has no question", "Question", "load");
    		} else if (a == null) {
    			EventLog.log(name + " has no answer", "Question", "load");
    		} else {
    			_allQuestions.put( i, new Question(name, q, a, i) );
    		}
    	}
    }
}
