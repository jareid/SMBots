/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.help;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.smokinmils.logging.EventLog;

/**
 * Represents a single question from the FAQ/Help system.
 * 
 * @author Jamie
 */
public final class Question {
    /** A map of all questions with the order they should be displayed in. */
	private static Map<Integer, Question> allQuestions
	                                        = new TreeMap<Integer, Question>();
	
	/** The topic of this question. */
	private final String topic;
	
	/** The question. */
	private final String question;
	
	/** The answer. */
	private final String answer;
	
	/**
	 * Constructor.
	 * 
	 * @param t    	The text used to call this question
	 * @param q	 	The question text
	 * @param a	 	The answer for this question
	 * @param i    The order of this question
	 */
    private Question(final String t,
                     final String q,
                     final String a,
                     final int i) {
        topic = t;
        question = q;
        answer = a;
    }

    /**
     * @return the topic
     */
    public String getTopic() { return topic; }
    
    /**
     * @return the question
     */
    public String getQuestion() { return question; }
    
    /**
     * @return the answer
     */
    public String getAnswer() { return answer; }
    
    /**
     * @return all the questions
     */
    public static Map<Integer, Question> values() { return allQuestions; }
    
    /**
     * Convert a topic into a Question object.
     * 
     * @param text the topic
     * 
     * @return the question or null if one doesn't exist
     */
    public static Question fromString(final String text) {
    	Question ret = null;
    	for (Question q: allQuestions.values()) {
    		if (q.getTopic().equalsIgnoreCase(text)) {
    			ret = q;
    			break;
    		}
    	}
    		
    	return ret;
    }
    
    /**
     * Load all the questions from the specified file.
     * 
     * @param filename the file to read from
     * @param dirname the file to read from
     * 
     * @throws IOException if the file is not an ini or 
     *                     if the file doesn't exist or
     *                     if we fail to read the file
     */
    public static void load(final String dirname, final String filename)
            throws IOException {
        File dir = new File(dirname);
        File afile = new File(dir, filename);
		Ini ini = new Ini(new FileReader(afile));
    	for (String name: ini.keySet()) {
    		Section section = ini.get(name);
    		String q = section.get("question");
    		String a = section.get("answer");
    		Integer i = section.get("i", Integer.class);
    		if (i == null) {
                i = allQuestions.size();
            }
    		if (q == null) {
    			EventLog.log(name + " has no question", "Question", "load");
    		} else if (a == null) {
    			EventLog.log(name + " has no answer", "Question", "load");
    		} else {
    			allQuestions.put(i, new Question(name, q, a, i));
    		}
    	}
    }
}
