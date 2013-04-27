package org.smokinmils.casino;

import java.util.HashMap;
import java.util.List;

import org.pircbotx.User;
import org.smokinmils.bot.IrcBot;

/**
 * Interface to represent an irc game that will be called from the BOT different
 * games can all be run independently :)
 * 
 * @author cjc
 */
public interface IRCGame {

	/**
	 * Checks if a string is a valid command for the game we are implementing
	 * 
	 * @param command
	 *            the command to check :)
	 * @return True is valid, False if not
	 */
	public boolean isValidCommand(String command);

	/**
	 * Process the command, and return the string to be sent back to channel
	 * 
	 * @param command
	 *            The command to process in the form of a list of strings (0 is
	 *            command etc)
	 * @param username
	 *            The username of the person
	 * @return The strings to send back to the channel
	 */
	public List<String> processCommand(String[] command, User user,
			int userlevel, IrcBot bot);

	/**
	 * Get's the information for the game to be sent back to the user
	 * 
	 * @return String of instructions for the game.
	 */
	public String getInfo();

	/**
	 * Performs a timed task represented internally as an integer
	 * 
	 * @param taskId
	 *            The task which to perform
	 */
	public List<String> timerTask(int taskId);

	/**
	 * Returns an HashMap<Integer,Integer> that will denote all tasks that this
	 * has to do
	 * 
	 * @return All integer timed tasks
	 */
	public HashMap<Integer, Integer> getTimedTasks();

	/**
	 * Gets the channel we are working in
	 * 
	 * @return a string of channel
	 */
	public String getChannel();
}
