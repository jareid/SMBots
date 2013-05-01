/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokingmils.help;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.smokinmils.logging.EventLog;

public class Question {
	private static Map<String, Question> _allQuestions = new HashMap<String, Question>();
	private String topic;
	private String question;
	private String answer;
	
	/**
	 * Constructor
	 * @param arg	 The text used to call this question
	 * @param q	 	The question text
	 * @param a	 	The answer for this question
	 */
    private Question(String t, String q, String a) {
        topic = t;
        question = q;
        answer = a;
        
        _allQuestions.put(t.toLowerCase(), this);
    }
    
    public String getTopic() { return topic; } 
    public String getQuestion() { return question; }    
    public String getAnswer() { return answer; }
    public static Map<String, Question> values() { return _allQuestions; }
    
    
    public static Question fromString(String text) {
        return _allQuestions.get(text.toLowerCase());
    }
    
    public static void load(String filename) throws InvalidFileFormatException,
    								 				FileNotFoundException,
    								 				IOException {
		Ini ini = new Ini( new FileReader( filename ) );
    	for (String name: ini.keySet()) {
    		Section section = ini.get(name);
    		String q = section.get("question");
    		String a = section.get("answer");
    		if (q == null) {
    			EventLog.log(name + " has no question", "Question", "load");
    		} else if (a == null) {
    			EventLog.log(name + " has no answer", "Question", "load");
    		} else {
    			_allQuestions.put( name, new Question(name, q, a) );
    		}
    	}
    }
}
