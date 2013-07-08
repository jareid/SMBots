package org.smokinmils.games;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.smokinmils.BaseBot;
import org.smokinmils.bot.Event;
import org.smokinmils.bot.IrcBot;
import org.smokinmils.bot.Utils;
import org.smokinmils.bot.events.Message;
import org.smokinmils.cashier.commands.UserCommands;
import org.smokinmils.games.casino.DiceDuel;
import org.smokinmils.games.casino.OverUnder;
import org.smokinmils.games.casino.Roulette;
import org.smokinmils.games.casino.blackjack.BJGame;
import org.smokinmils.games.rockpaperscissors.RPSGame;

/**
 * Allows managers to invite the bot into a channel with a certain game.
 * @author cjc
 *
 */
public class Invite extends Event {

    /** the command to invite. */
    private static final String INVITE_CMD = "!invite";
    
    /** string representation for roulette . */
    private static final String ROULETTE = "roulette";
    
    /** string representation for dice duel. */
    private static final String DD = "dd";
    
    /** string representation for overunder. */
    private static final String OU = "ou";
    
    /** string representation for rockpaperscissors. */
    private static final String RPS = "rps";
    
    /** string representation for blackjack. */
    private static final String BJ = "bj";

    /** length of the roulette command. */
    private static final int ROULETTE_CMD_LENGTH = 4;
    
    /** length of the normal command (ie not roulette). */
    private static final int CMD_LENGTH = 3;

    /** Instructions for roulette. */
    private static final String INVALID_COMMAND_ROULETTE = "%b%c04%who%c12: To invite a roulette " 
    + "game you must use !invite #chan roulette x, where x is the delay for the roulette rounds";

    /** Instructions for other games. */
    private static final String INVALID_COMMAND = "%b%c04%who%c12: To invite a " 
                    + "game you must use !invite #chan <game>";
    
    /* (non-Javadoc)
     * @see org.smokinmils.bot.Event#message(org.smokinmils.bot.events.Message)
     */
    @Override
    public final void message(final Message event) {
        IrcBot bot = event.getBot();
        String message = event.getMessage();
        User sender = event.getUser();
        Channel chan = event.getChannel();
        String[] msg = message.split(" ");
        
        if (isValidChannel(chan.getName())
                && bot.userIsIdentified(sender)) {
            if (Utils.startsWith(message, INVITE_CMD)) {
                String newchannel = msg[1];
                BaseBot bb = BaseBot.getInstance();
                String game = msg[2];
                
                if (game.equals(ROULETTE)) {
                    if (msg.length < ROULETTE_CMD_LENGTH) {
                        bot.sendIRCMessage(chan, INVALID_COMMAND_ROULETTE);
                    } else {
                        // start roulette in that channel
                        
                        int rdelay = Utils.tryParse(msg[1 + 1 + 1]);
                        
                        if (rdelay > 0) {
                            bb.addChannel(bot.getServer(), newchannel);
                            Roulette roulette = new Roulette(rdelay, newchannel, bot);
                            roulette.addValidChan(newchannel);
                            bb.addListener(bot.getServer(), roulette);
                        } else {
                            String out = INVALID_COMMAND_ROULETTE.replaceAll("%who", 
                                                                            sender.getNick());
                            bot.sendIRCMessage(chan, out); 
                        }
                    }
                } else {
                   if (msg.length < CMD_LENGTH) {
                       String out = INVALID_COMMAND.replaceAll("%who", sender.getNick());
                       bot.sendIRCMessage(chan, out);
                   } else {
                       // valid length 
                       Boolean adding = false;
                       if (game.equals(DD)) {
                           adding = true;
                           DiceDuel dd = new DiceDuel(bot, newchannel);
                           dd.addValidChan(newchannel);
                           bb.addListener(bot.getServer(), dd);
                       } else if (game.equals(OU)) {
                           adding = true;
                           OverUnder ou = new OverUnder();
                           ou.addValidChan(newchannel);
                           bb.addListener(bot.getServer(), ou);
                       } else if (game.equals(RPS)) {
                           adding = true;
                           RPSGame rpsevent = new RPSGame();
                           rpsevent.addValidChan(newchannel);
                           rpsevent.addAnnounce(newchannel, bot);
                           bb.addListener(bot.getServer(), rpsevent);
                       } else if (game.equals(BJ)) {
                           adding = true;
                           BJGame bj = new BJGame(bot);
                           bj.addValidChan(newchannel);
                           bb.addListener(bot.getServer(), bj);
                       } else {
                           String out = INVALID_COMMAND.replaceAll("%who", sender.getNick());
                           bot.sendIRCMessage(chan, out); 
                       }
                       
                       
                       if (adding) {
                           bb.addChannel(bot.getServer(), newchannel); 
                           //add in harmless commands.
                           UserCommands uc = new UserCommands();
                           uc.addValidChan(newchannel);
                           bb.addListener(bot.getServer(), uc);
                       }
                   }
                } 
            } 
        }
    }
}
