package mi.cjc.ircbot;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Colors;
import org.pircbotx.User;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.KickEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;


@SuppressWarnings("rawtypes")
public class Bot extends ListenerAdapter implements Listener
{
	// <Channel, Game>
	private ArrayList<IRCGame> games;
	private ArrayList<String> validUsers;
	private ArrayList<String> badUsers;
	
	private String MSG = Colors.BLUE;
	private String VAR = Colors.RED;
	private String BLD = Colors.BOLD;
	
	private ArrayList<Timer> events;
	
	private String infoText;
	
	private static Object locked = new Object();
	
	public Bot(PircBotX bot)
	{
		
		games = new ArrayList<IRCGame>();
		validUsers = new ArrayList<String>();
		badUsers = new ArrayList<String>();
		
		new ArrayList<String>();
		// Live configuration
		games.add(new Roulette(5, "#smokin_dice", bot));
		games.add(new Roulette(1, "#sm_roulette", bot));
		games.add(new OverUnder("#sm_overunder"));
		games.add(new DiceDuel("#smokin_dice"));
		
		// test config
		//games.add(new Roulette(1, "#testeroo", bot));
		//games.add(new DiceDuel("#testeroo"));
		//games.add(new OverUnder("#testeroo"));
		
		
		// initialize timing events
		
		
		events = new ArrayList<Timer>();
		for(IRCGame g : games)
		{
			// join all the channels
			bot.joinChannel(g.getChannel());
			
			// add all the timed events if we have them
			if (g.getTimedTasks() != null)
			{
				for (Map.Entry<Integer, Integer> entry : g.getTimedTasks().entrySet())
				{
					if (entry.getValue() < 0)
					{
						Timer timer = new Timer();
						timer.schedule(new gameTrigger(entry.getKey(),g, bot,g.getChannel()), (-entry.getValue() * 60 * 1000)-10000, -entry.getValue() * 60 * 1000);
						events.add(timer);
					}
					else
					{
						Timer timer = new Timer();
						timer.schedule(new gameTrigger(entry.getKey(), g, bot, g.getChannel()), entry.getValue() * 60 * 1000, entry.getValue() * 60 * 1000);
						events.add(timer);
					}
				}
			}
		}
		// read the info text TODO tidy this up
		
	}
	
	
	
	// quit, change name
	@Override
	public void onNickChange(NickChangeEvent e) throws Exception
	{
		// when some one changes nick, remove them from the valid users list if they exist
		if(validUsers.contains(e.getOldNick()))
		{
			validUsers.remove(e.getOldNick());
		}
	}
	
	@Override
	public void onKick(KickEvent e) throws Exception
	{
		
		// when some one is kicked, remove them from the valid users list if they exist
		if(validUsers.contains(e.getRecipient().getNick()))
		{
			validUsers.remove(e.getRecipient().getNick());
		}
	}
	
	@Override
	public void onQuit(QuitEvent e) throws Exception
	{
		// when some one quits, remove them from the valid users list if they exist
		if(validUsers.contains(e.getUser().getNick()))
		{
			validUsers.remove(e.getUser().getNick());
		}
	}
	
	@Override
	public void onPart(PartEvent e) throws Exception
	{
		// when some one parts, remove them from the valid users list if they exist
		if(validUsers.contains(e.getUser().getNick()))
		{
			validUsers.remove(e.getUser().getNick());
		}
	}
	
	/**
	 * Deals with messages from irc to call the current game etc
	 */
	@Override
	public synchronized void onMessage(MessageEvent e) throws Exception
	{
		synchronized(locked)
		{
			locked = true;
			// if the message starts with ! (so it is a command) and it is longer than just !
			if(e.getMessage().startsWith("!") && e.getMessage().length() > 1)
			{
								
				// Extract the command, see if it is valid with the current game
				// if the user is not verified, just ignore the command
				if(!validUsers.contains(e.getUser().getNick()))
				{
					// user is invalid, so perform a wait for and attempt to check if they
					// are authenticated
					
					// this needs to be fixed as it is pretty horrible 
					
					//System.out.println("In not valid users if statement");
	
						e.getBot().sendMessage("nickserv", "status " + e.getUser().getNick());
						NoticeEvent currentEvent = e.getBot().waitFor(NoticeEvent.class);
						
						if (currentEvent.getUser().getNick().equalsIgnoreCase("nickserv"))
						{
							//System.out.println("Got event, processing");
							String check = currentEvent.getMessage().split(" ")[2];
							String who = currentEvent.getMessage().split(" ")[1];
							if(check.equalsIgnoreCase("3") && e.getUser().getNick().equalsIgnoreCase(who))
							{
								validUsers.add(e.getUser().getNick());
								System.out.println("Validated user: " + e.getUser().getNick());
								//return;
							}
							else
							{
								// quit since they are not validated
								if(!badUsers.contains(e.getUser().getNick()))
								{
									e.getBot().sendMessage(e.getUser().getNick(), "To use our systems you must first register your name using the command: /ns register PASS PASS EMAIL EMAIL then check your email for a confirmation link, and then identify yourself using /ns id PASS");
									badUsers.add(e.getUser().getNick());
								}
								
								System.out.println("Invalid User: " + e.getUser().getNick() );
								locked = false;
								return;
							}
						}
					
				}
				
				// at this point we have a user who is regged with nickserv
				if(!Accounts.getInstance().isValidUser(e.getUser().getNick()))
				{
					// this user is registered with nickserv, but not on our systems, 
					Accounts.getInstance().addUser(e.getUser().getNick());
					// then proceed to carry on
				}
				String[] words = e.getMessage().split("!")[1].split(" ");
				// if info, do that, else check if it's an accounting command, else check game commands
				String command = words[0];
				String sender = e.getUser().getNick();
				String chan = e.getChannel().getName();
				if (command.equalsIgnoreCase("ou") && e.getChannel().getName().equalsIgnoreCase("#smokin_dice"))
				{
					e.respond(BLD + VAR + e.getUser().getNick() + MSG+ " Please join #SM_OverUnder to play Over/Under"); // todo fix this / tidy
					locked = false;
					return;
				}
				else if (command.equalsIgnoreCase("info"))
				{
					InputStream fis;
					BufferedReader br;
					String line;
	
					try
					{
						fis = new FileInputStream("info.txt");
						br = new BufferedReader(new InputStreamReader(fis));
						if((line = br.readLine()) != null) 
						{
							infoText = line;
						}
					}
					catch (Exception ex)
					{
						System.out.println("Error reading the info.txt");
						infoText = "Error with info text, please contact an Admin";
					}
					e.getBot().sendMessage(e.getUser(), infoText);
					e.getBot().sendNotice(e.getUser(), infoText);
	
				
				
					
				}
				else //if ( game.isValidCommand(command) )
				{
					// game commands
					// since replies might require multiple lines, iterate through the replies and send them
					for(IRCGame g : games)
					{
						if(g.getChannel().equalsIgnoreCase(chan) && g.isValidCommand(command))
						{
							// should check if is valid here
							for (String reply : g.processCommand(words, e.getUser(), this.getUserLevel(sender, chan, e.getUser()),e.getBot()))
								e.getBot().sendMessage(chan, reply);
						}
					}
				}
				// other wise it's not a valid command, so do nothing :)
			}
			
			locked = false;
		}
	}
	
	/**
	 * Iterate through the list of users and get the level
	 * @param username	The username in question
	 * @return			The users level, where 0 = normal, 1 = voiced, 2 = op
	 */
	private int getUserLevel(String username, String channel, User user)
	{

				for(Channel chan : user.getChannelsOpIn())
				{
					if(chan.getName().equalsIgnoreCase(channel))
						return 2;
				}
				for(Channel chan : user.getChannelsVoiceIn())
				{
					if(chan.getName().equalsIgnoreCase(channel))
						return 1;
				}
			
		
		return 0;
	}
	
	
	
	/**
	 * Sets up and runs the Bot
	 * @param args n/a
	 */
	public static void main(String[] args)
	{
		// first thing process refunds, and get the db connected
		// TODO could do this on disconnect too?
		Accounts.getInstance().processRefunds();
		
		// then connect and shit
		PircBotX bot = new PircBotX();;
		bot.setName(Settings.BOTNAME);
		bot.setLogin("Bot");
		
		//bot.setVerbose(true);
		
		bot.setAutoNickChange(true);
		
		//TODO this will spam a lot, maybe sort out admin to know it needs to run fast?
		bot.setMessageDelay(0);
		// not on my network
		
		
		try
		{
			
			bot.connect("irc.swiftirc.net");
			//bot.joinChannel("#testeroo");
			bot.joinChannel("#sm_hosts");
			bot.identify("5w807");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		//ListenerManager manager = new ListenerManager();
		
		bot.getListenerManager().addListener(new Bot(bot));
	}
	
	/**
	 * Simple extension to time task to deal with game triggers
	 * @author cjc
	 *
	 */
	class gameTrigger extends TimerTask {
		private int id;
		private PircBotX bot;
		private String channel;
		private IRCGame game;
		
		public gameTrigger(int id, IRCGame game, PircBotX bot, String channel)
		{
			this.id = id;
			this.bot = bot;
			this.channel = channel;
			this.game = game;
		}
		public void run()
		{
			//System.out.println("triggered");
			for (String reply : this.game.timerTask(id))
				bot.sendMessage(this.channel, reply);
			 //sendMessage(channel, reply);
		}
	}

}


