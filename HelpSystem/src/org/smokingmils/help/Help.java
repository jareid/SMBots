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
import java.io.IOException;
import java.util.Map;

import org.ini4j.InvalidFileFormatException;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.events.Message;
import org.smokinmils.logging.EventLog;

/**
 * Provides the functionality to check a user's chips
 * 
 * @author Jamie
 */
public class Help extends Event {
	public static final String Command = "!info";
	public static final String Description = "%b%c12Lists the available info topics";
	public static final String Format = "%b%c12" + Command + " ?topic?";
	
	public static final String InvalidTopic = "%b%c12Sorry, %c04%topic%c12 is not a valid topic. Please use %c04%cmd%c12 for valid questions/topics!";
	
	public static final String FileName = "faq.ini";
	
	/**
	 * Constructor 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws InvalidFileFormatException 
	 */
	public Help() {
		try {
			Question.load(FileName);
		} catch (IOException e) {
			EventLog.fatal(e, "Help", "Help");
			System.exit(0);
		}
	}

	/**
	 * This method handles the chips command
	 * 
	 * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
	 */
	@Override
	public void message(Message event) {
		IrcBot bot = event.getBot();
		String message = event.getMessage();
		String sender = event.getUser().getNick();
		String chan = event.getChannel().getName();
		
		if ( isValidChannel( chan ) &&	message.startsWith( Command ) ) {			
			String[] msg = message.split(" ");
			if (msg.length == 1) {
				// list all questions
				Map<Integer, Question> topics = Question.values();
				// for every question
				for (Question q: topics.values()) {
					bot.sendIRCNotice( sender, "%b%c12" + q.getQuestion() + "%c12 - Use %c04" + Command + " " + q.getTopic() );
				}
			} else if (msg.length == 2) {
				Question q = Question.fromString(msg[1]);
				if (q == null) {
					String out = InvalidTopic.replaceAll( "%topic", msg[1] );
					out = out.replaceAll( "%cmd", Command );
					bot.sendIRCNotice( sender, out );					
				} else {
					bot.sendIRCNotice( sender, "%b%c12" + q.getQuestion());
					bot.sendIRCNotice( sender, "%b%c12" + q.getAnswer());
				}
			} else {
				bot.invalidArguments( sender, Format );
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
			
			//move to next line, we add 2 as startIndex is inclusive and we want to skip the space too. 
			start = end + 2;
			end = start + MaxCharacters;
		}
		
		out.add( in.substring(start, length) );
		return out;
	}*/
}
