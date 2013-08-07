/**
 * This file is part of a commercial IRC bot that 
 * allows users to play online IRC games.
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.help;

import java.io.IOException;
import java.util.Map;

import org.pircbotx.User;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to check a user's chips.
 * 
 * @author Jamie
 */
public class Help extends Event {
    /** The text used to use the info command. */
	public static final String COMMAND = "!info";
	
    /** A description of the info command. */
	public static final String DESCRIPTION = "%b%c12Lists the available info "
	                                       + "topics";
	
	/** The format used for the info command. */
	public static final String FORMAT = "%b%c12" + COMMAND + " ?topic?";
	
	/** The message used when we receive an invalid topic. */
	public static final String INVALID_TOPIC = "%b%c12Sorry, %c04%topic%c12 is "
	 + "not a valid topic. Please use %c04%cmd%c12 for valid questions/topics!";
	
    /**The directory where we store the questions. */
    private static final String         DIRNAME         = "settings";
	
	/** The file where we store the questions. */
	public static final String FILENAME    = "faq.ini";
	
	/**
	 * Constructor.
	 */
	public Help() {
		try {
			Question.load(DIRNAME, FILENAME);
		} catch (IOException e) {
			EventLog.fatal(e, "Help", "Help");
			System.exit(0);
		}
	}

	/**
	 * This method handles the chips command.
	 * 
	 * @param event The Message event
	 */
	@Override
    public final void message(final Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		User sender = event.getUser();
		String chan = event.getChannel().getName();
		
		if (isValidChannel(chan) && Utils.startsWith(message, COMMAND)) {
			String[] msg = message.split(" ");
			if (msg.length == 1) {
				// list all questions
				Map<Integer, Question> topics = Question.values();
				// for every question
				for (Question q: topics.values()) {
					bot.sendIRCNotice(sender, "%b%c12" + q.getQuestion()
					                          + "%c12 - Use %c04" + COMMAND
					                          + " " + q.getTopic());
				}
			} else if (msg.length == 2) {
				Question q = Question.fromString(msg[1]);
				if (q == null) {
					String out = INVALID_TOPIC.replaceAll("%topic", msg[1]);
					out = out.replaceAll("%cmd", COMMAND);
					bot.sendIRCNotice(sender, out);
				} else {
					bot.sendIRCNotice(sender, "%b%c12" + q.getQuestion());
					bot.sendIRCNotice(sender, "%b%c12" + q.getAnswer());
				}
			} else {
				bot.invalidArguments(sender, FORMAT);
			}
		}
	}
	
	/*private List<String> splitToLines(String in) {
		int start = 0;
		int end = start + MaxCharacters;
		int length = in.length();
		List<String> out = new ArrayList<String>();
		char is_space = ' ';
		String line = "";
		
		while (end < length) {
			// only end on a space
			do {
				is_space = in.charAt(end);
				line = in.substring(start, end);
				end--;
			} while (is_space != ' ');
			out.add( line + "\n" );
			
			/* move to next line, we add 2 as startIndex is inclusive and
			 * we want to skip the space too./
			start = end + 2;
			end = start + MaxCharacters;
		}
		
		out.add( in.substring(start, length) );
		return out;
	}*/
}
