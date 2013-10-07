package org.smokinmils.games.tournaments;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.pircbotx.Channel;
import org.smokinmils.BaseBot;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.database.DB;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.Bet;
import org.smokinmils.logging.EventLog;


/**
 * Creates and starts tournaments.
 * @author cjc
 *
 */
public class Tournament extends Event {

    /** The register command. */
    private static final String REGCMD = "!reg";
    
    /** The start command. */
    private static final String STARTCMD = "!start";
    
    /** The roll command. */
    private static final String ROLLCMD = "!roll";
    
    /** The de-reg command. */
    private static final String DEREGCMD = "!dereg";
            
    /** The message send with registered. */
    private static final String REGDONE = "%b%c04%who%c12: You have registered for the tournament!";
    
    /** If the user is already registered! */
    private static final String REGDENIED = "%b%c04%who%c12: You are already registered for "
            + "the tournament!";

    /** If the user tries to join after it has started. */
    private static final String STARTED = "%b%c04%who%c12: The tournament has already started, "
            + "sorry!";

    /** String showing the current round. */
    private static final String NEWROUND = "%b%c12Welcome to Round %c04%roundno%c12! Following "
            + "are the dice duels for this round! Type %c04" + ROLLCMD + "%c12 to roll when you "
                    + "have a match";

    /** letting the use rknow they are not registered yet. */
    private static final String NOTREG = "%b%c04%who%c12: You are not registered yet!";

    /** String letting the user know they are removed from the tourney! */
    private static final String REMOVED = "%b%c04%who%c12: You have left the tournament!";

    /** String to show a round timeout has happened! */
    public static final String TIMEOUT = "%b%c04Round over, any unrolled games will now "
            + "be auto rolled!";

    /** The minimum players for a tourney to start. */
    private static final int MINPLAYERS = 2;
    
    /** The Maximum players. */
    private static final int MAXPLAYERS = 64;
    
    /** String for informing not enough players. */
    private static final String NOTENOUGH = "%b%c04%who%c12: Not enough players, need at "
            + "least %c04" + String.valueOf(MINPLAYERS) + "%c12 but have only %c04%num";

    /** One minute warning per round. */
    public static final String ONEMINLEFT = "%b%c04One minute left for this round!";

    /** If they don't have enough chips to enter. */
    private static final String NOCHIPS = "Sorry %c04%who%c12, you don't have enough chips!";

    /** Start of tourney / prize announce. */
    private static final String PRIZEANNOUNCE = "%b%c12Registration is now closed, The "
            + "tournament is starting with a prize of %c04%prize %profile %c12chips!";
    
    /** Message to spam every minute when waiting to start. */
    private static final String INREG = "%b%c12Dice Duel tournament in registration, type "
            + "%c04" + REGCMD + " %c12to enter, %c04%amount %profile%c12 chips required";
    
    /** Message when a tournament is finished! */
    private static final String FINISHED = "%b%c12The tournament is over, congratulations "
            + "to %c04%winner%c12, who has won %c04%amount %profile%c12 chips!!!";

    /** Message when a tournament is full. */
    private static final String TOOMANY = "%b%c12Sorry %c04%who%c12, there are too many "
            + "people registered";
    
    /** The channel the tournament is in. */
    private static Channel channel;
    
    /** The irc bot. */
    private static IrcBot ib;
    
    /** The players in the tournament. */
    private List<String> players;
    
    /** The bets active. */
    private final List<Bet> bets;
    
    /** The current round. */
    private Round currentRound = null;
    
    /** The round number, so it looks nice. */
    private int roundno = 0;
    
    /** The time per round. */
    private int timePerRound = 0;
    
    /** The profile type for the tournament. */
    private final ProfileType profile;
    
    /** The registration cost. */
    private final double amount;
    
    /** The prize cash dollar coin chips. */
    private double prizeMoney;
    
    /** Timer for tasks. */
    private final Timer announce;
    
    /**
     * Constructor.
     * @param bot the bot 
     * @param chan the channel from which we can start tournaments.
     * @param maxtime the max time per round before auto rolls happen.
     * @param prof the profile for this tournament
     * @param amt the registration cost
     */
    public Tournament(final IrcBot bot, final Channel chan, final int maxtime,
                      final ProfileType prof, final double amt) {
        // check the channel is a valid IRC Channel name
        String chanstr = chan.getName();
        if (!chanstr.matches("([#&][^\\x07\\x2C\\s]{1,200})")) {
            throw new IllegalArgumentException(chanstr
                    + " is not a valid IRC channel name");
        } else {
            ib = bot;
            this.addValidChan(chanstr);
            
            ib.sendIRC().joinChannel(chanstr);
            BaseBot bb = BaseBot.getInstance();
            bb.addListener(bot.getServer(), this);
  
        }   
        
        // initialise variables
        players = new ArrayList<String>();
        bets = new ArrayList<Bet>();
        
        channel = chan;
        timePerRound = maxtime;
        profile = prof;
        amount = amt;
        
        prizeMoney = 0;
        
        // start the timer
        announce = new Timer(true);
        announce.scheduleAtFixedRate(
                new Announce(), Utils.MS_IN_MIN,
                Utils.MS_IN_MIN);
    } 
    
    /**
     * Handles the commands.
     * 
     * @param event the message event.
     */
    @Override
    public final void message(final Message event) {
        String message = event.getMessage();
        
        synchronized (this) {
            if (event.getBot().userIsIdentified(event.getUser())
                    && isValidChannel(event.getChannel().getName())) {
                if (Utils.startsWith(message, REGCMD)) {
                    doRegister(event);  
                } else if (Utils.startsWith(message, STARTCMD)) { 
                       // && ib.userIsOp(senderu, chan.toString())) {
                    doStart(event);
                } else if (Utils.startsWith(message, ROLLCMD)) {
                    doRoll(event);
                } else if (Utils.startsWith(message, DEREGCMD)) {
                    doDereg(event);
                }
            }
        }
    }

    /**
     * Handles the !dereg command.
     * @param event the event
     */
    private void doDereg(final Message event) {
        String user = event.getUser().getNick();
        if (currentRound == null) { // if we haven't started (can't de-reg mid game)
            
            if (players.contains(user)) {
                for (Bet bet : bets) {
                    if (bet.getUser().equalsIgnoreCase(user)) {
                        try {
                            bet.cancel();
                            break;
                        } catch (SQLException e) {
                            EventLog.log(e, "Tournament", "doDereg");
                        }
                    }
                }
                players.remove(user);
                String out = REMOVED.replaceAll("%who", user);
                ib.sendIRCMessage(channel, out);
            } else {
                String out = NOTREG.replaceAll("%who", user);
                ib.sendIRCMessage(channel, out);
            }
        } else {
            String out = STARTED.replaceAll("%who", user);
            ib.sendIRCMessage(channel, out);
        }
        
    }

    /**
     * Handles the !roll command.
     * @param event the event
     */
    private void doRoll(final Message event) {
        
        String user = event.getUser().getNick();
        // find game, if they are in a game and haven't rolled, roll
        // after each roll check if round is finished, if round is finished, process.
        Match match = null;
        
        for (Match m : currentRound.getMatches()) {
            if (m.contains(user)) {
                match = m;
                break;
            }
        }
        
        if (match != null) {
            if (!match.playerHasRolled(user)) {
                match.roll(user);
                checkRound(); // if a round is over it starts the next round
                
            }
        } 
        // no point sending them a message they ar enot in a game?
        
        
    }

    /**
     * Method to check if a round is over, and then either start a new one
     * or finish the tournament!
     */
    private void checkRound() {
        if (currentRound.isFinished()) {
         // clear out bets of losers and start a new round
            for (String player : currentRound.getLosers()) {
                for (Bet bet : bets) {
                    if (bet.getUser().equals(player)) {
                        try {
                            bet.close();
                        } catch (SQLException e) {
                            EventLog.log(e, "Tournament", "checkRound.1");
                        }
                    }
                }
            }
            players = currentRound.getWinners();
            if (players.size() == 1) { // if there is a single player left
                                       // then we have a winner!
                try {
                    String winner = players.get(0);
                    
                    for (Bet bet : bets) {
                        if (bet.getUser().equals(winner)) {
                            bet.win(prizeMoney);
                            bet.close();
                            break;
                        }
                    }
                    
                    String out = FINISHED.replaceAll("%winner", winner);
                    out = out.replaceAll("%amount", Utils.chipsToString(prizeMoney));
                    out = out.replaceAll("%profile", profile.getText());
                    ib.sendIRCMessage(channel, out);
                    
                    // cancel timer and remove self
                    announce.cancel();
                    ib.getListenerManager().removeListener(this);
                } catch (SQLException e) {
                    EventLog.log(e, "Tournament", "checkRound.2");
                }
                
                // reset for another
                players.clear();
                currentRound = null;
                roundno = 0;
                prizeMoney = 0.0;
                bets.clear();
                
            } else {
                // start a new round
                newRound();
            }
        }
        
    }

    /**
     * Method to start a new round.
     */
    private void newRound() {
        Collections.shuffle(players);
        
        roundno++;
        
        String out = NEWROUND.replaceAll("%roundno", String.valueOf(roundno));
        ib.sendIRCMessage(channel, out);
        
        int numMatches = (int) Math.floor(players.size() / 2);
        
        currentRound = new Round(timePerRound);
        
        boolean hasBye = players.size() % 2 == 1;
        
        for (int m = 0; m < numMatches; m++) {
            int startm = m * 2;
            Match match = new Match(players.get(startm), players.get(startm + 1), ib, channel);
            ib.sendIRCMessage(channel, match.toString());
            currentRound.addMatch(match);
        }
        
        // give the last person has a bye (random due to shuffle)
        if (hasBye) {
            String lastDude = players.get(players.size() - 1);
            Match match = new Match(lastDude, ib, channel);
            ib.sendIRCMessage(channel, match.toString());
            currentRound.addMatch(match);
        }
    }

    /**
     * Handles the !start command.
     * @param event the event
     */
    private void doStart(final Message event) {
        if (currentRound == null) {
            if (players.size() < MINPLAYERS) {
                String out = NOTENOUGH.replaceAll("%who", event.getUser().getNick());
                out = out.replaceAll("%num", String.valueOf(players.size()));
                ib.sendIRCMessage(channel, out);
            } else {
                double rake = Rake.getRake(players.get(0), prizeMoney, profile);
                prizeMoney = prizeMoney - rake;
                
                String out = PRIZEANNOUNCE.replaceAll("%prize", Utils.chipsToString(prizeMoney));
                out = out.replaceAll("%profile", profile.toString());
                ib.sendIRCMessage(channel, out);
                newRound();
            }
        }
    }

    /**
     * Registers a user in the tournament.
     * @param event the event to get the info from
     */
    private void doRegister(final Message event) {
        String user = event.getUser().getNick();
        if (currentRound == null) {
            
            if (players.contains(user)) {
                String out = REGDENIED.replaceAll("%who", user);
                ib.sendIRCMessage(channel, out);
            } else {
                try {
                    if (players.size() >= MAXPLAYERS) {
                        String out = TOOMANY.replaceAll("%who", user);
                        ib.sendIRCMessage(channel, out);
                    } else {
                        // check if they have chips, and then enter if they do
                        DB db = DB.getInstance();
                        
                        if (db.checkCredits(user, profile) >= amount) {
                            players.add(user);
                            prizeMoney += amount;
                            
                            Bet bet = new Bet(user, profile, GamesType.DICE_DUEL, amount, null);
                            bets.add(bet);
                            
                            String out = REGDONE.replaceAll("%who", user);
                            ib.sendIRCMessage(channel, out);
                        } else {
                            String out = NOCHIPS.replaceAll("%who", user);
                            out = out.replaceAll("%amount", Utils.chipsToString(amount));
                            out = out.replaceAll("%prof", profile.getText());
                            ib.sendIRCMessage(channel, out);
                        }
                    }
                } catch (SQLException e) {
                    EventLog.log(e, "Tournament", "doRegister");
                }
            }
        } else {
            String out = STARTED.replaceAll("%who", user);
            ib.sendIRCMessage(channel, out);
        }
    }
    
    /**
     * Simple extension to time task to deal with timeouts for the game.
     * 
     * @author cjc
     */
    class Announce extends TimerTask {
        @Override
        public void run() {
           if (currentRound != null) {
               int timeleft = currentRound.getTimeLeft();
               if (timeleft == 1) {
                   ib.sendIRCMessage(channel, ONEMINLEFT);
               } else if (timeleft <= 0) {
                   ib.sendIRCMessage(channel, TIMEOUT);
                   currentRound.timeOut();
                   checkRound();
               }
           } else {
               // spam the advertisement
               String out = INREG.replaceAll("%amount", Utils.chipsToString(amount));
               out = out.replaceAll("%profile", profile.toString());
               ib.sendIRCMessage(channel, out);
           }
        }
    }
}
