package org.smokinmils.bot;

import java.io.FileInputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.smokinmils.BaseBot;
import org.smokinmils.cashier.ManagerSystem;
import org.smokinmils.cashier.commands.Coins;
import org.smokinmils.cashier.commands.UserCommands;
import org.smokinmils.cashier.rake.Rake;
import org.smokinmils.cashier.tasks.BetDetails;
import org.smokinmils.cashier.tasks.Competition;
import org.smokinmils.cashier.tasks.JackpotAnnounce;
import org.smokinmils.cashier.tasks.ManagerAnnounce;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.games.casino.DiceDuel;
import org.smokinmils.games.casino.OverUnder;
import org.smokinmils.games.casino.Roulette;
import org.smokinmils.games.casino.blackjack.BJGame;
import org.smokinmils.games.casino.poker.Client;
import org.smokinmils.games.rockpaperscissors.RPSGame;
import org.smokinmils.games.timedrollcomp.CreateTimedRoll;
import org.smokinmils.games.timedrollcomp.TimedRollComp;
import org.smokinmils.help.Help;
import org.smokinmils.logging.EventLog;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Deals with loading a configuration from XML *headache*.
 * @author cjc
 *
 */
public final class XMLLoader {

        /** create the singleton. */
        private static final XMLLoader INSTANCE = new XMLLoader();

        /** because 25 is a magic number / time to wait for bot to connect. */
        private static final long CONNECT_SLEEP_TIME = 25;
        
        /** the parsed document we can navigate. */
        private Document xmlDocument;
        /** 
         * Get's the instance. 
         * @return the instance...*/
        public static XMLLoader getInstance() {
            return INSTANCE;
        }
        
        /** something. */
        private DocumentBuilder builder;
        
        /** private constructor. */
        private XMLLoader() { }
        
        /**
         * Configures basebot based on XML data.
         * @throws UnknownHostException 
         * @throws InterruptedException 
         * @return the basebot instance with the named configuration loaded
         */
        public BaseBot loadBotSettings() throws UnknownHostException, 
                                      InterruptedException {
            
            BaseBot basebot = BaseBot.getInstance();
            
            DocumentBuilderFactory builderFactory =
                    DocumentBuilderFactory.newInstance();
            builder = null;
            String serveraddr = null;
            xmlDocument = null;
            IrcBot bot = null;
            
            try {
                builder = builderFactory.newDocumentBuilder();
               
                xmlDocument = builder.parse(
                        new FileInputStream("settings.xml"));
                
                serveraddr = initBaseBot();
               
                bot = basebot.getBot(serveraddr);
                
                Thread.sleep(CONNECT_SLEEP_TIME); // wait for some time to allow bot to connect.

                joinChannels(serveraddr);
                
                loadListeners(serveraddr, bot);
                
                loadTimers(serveraddr, bot);
                
            } catch (Exception e) {
                e.printStackTrace();  
            }
            //Client poker = new Client(swift_irc, poker_lobby_swift);
            //poker.initialise();
            //basebot.addListener(swift_irc, poker);

            
            return basebot;
        }
        
        /**
         * Gets a DB setting.
         * @param setting    the setting we want
         * @return the value from the xml file
         */
        public String getDBSetting(final String setting) {
            String ret = "";
            try {

                XPath xPath =  XPathFactory.newInstance().newXPath();
 
                String expression = "/bot/*";
                //read a nodelist using xpath
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, 
                                                            XPathConstants.NODESET);
                  
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String n = nodeList.item(i).getNodeName();
                    Element el = (Element) nodeList.item(i); 
                    if (n.equals("db")) {
                        if (el.getAttribute("type").equals(setting)) {
                            ret = el.getTextContent();
                            break;
                        }
                    }
                }
                
            } catch (Exception e) {
                EventLog.log(e, "XMLLoader", "joinChannels");
            }
            return ret;
        }
        
        /**
         * Joins all channels in the xml file for the specific bot.
         * @param xmldocument   the prepared xml document
         * @param serveraddr    the server we are doing this on
         */
        private void joinChannels(final String serveraddr) {
            try {
                
                BaseBot basebot = BaseBot.getInstance();
                XPath xPath =  XPathFactory.newInstance().newXPath();
 
                String expression = "/bot/listener/*";
                //read a nodelist using xpath
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, 
                                                            XPathConstants.NODESET);
                  
                ArrayList<String> channels = new ArrayList<String>();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    String n = nodeList.item(i).getNodeName();
                
                    if (n.equals("channel")) {
                        
                        channels.add("#" + nodeList.item(i).getTextContent());                       
                    }
                }
                
                // join unique channels
                HashSet<String> uniqueChannels = new HashSet<>(channels);
                for (String chan : uniqueChannels) {
                    basebot.addChannel(serveraddr, chan);
                }
                
            } catch (Exception e) {
                EventLog.log(e, "XMLLoader", "joinChannels");
            }
       
        }

        /**
         * Goes through and loads all the listeners and options in the xml file.
         * @param server    the server we are on
         * @param bot   the bot itself
         */
        private void loadListeners(final String server,
                                   final IrcBot bot) {
            try {
                
                BaseBot basebot = BaseBot.getInstance();
                XPath xPath =  XPathFactory.newInstance().newXPath();
 
                String expression = "/bot/*";
                //read a nodelist using xpath
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, 
                                                            XPathConstants.NODESET);
                  
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element el = (Element) nodeList.item(i);
                    String n = el.getNodeName();
                   
                    if (n.equals("listener")) {
                       HashMap<String, String> options = new HashMap<String, String>();
                       ArrayList<String> channels = new ArrayList<String>();
                        //get child nodes of channel and options
                        if (el.hasChildNodes()) {
                            // if has id use that so we can distinguish between multiple listeners
                            // of the same type
                            if (el.hasAttribute("id")) {
                                //System.out.println(el.getNodeName() + "::" + el.getAttribute("id"));
                                expression = "/bot/listener[@type='"
                                        + el.getAttribute("type") + "' and @id='"
                                        + el.getAttribute("id") + "']/*";
                            } else {
                                expression = "/bot/listener[@type='"
                                                             + el.getAttribute("type") + "']/*";
                            }
                            //read a nodelist using xpath
                            NodeList cNodes = (NodeList) xPath.compile(expression)
                                    .evaluate(xmlDocument, XPathConstants.NODESET);
                            //NodeList cNodes = el.getChildNodes();
                            for (int j = 0; j < cNodes.getLength(); j++) {
                                Element subel = (Element) cNodes.item(j);
                                String n2 = subel.getNodeName();
                                if (n2.equals("channel")) {
                                    // if we have a channel, create the game
                                    String channel = "#" + subel.getTextContent();
                                    channels.add(channel);
                                } else if (n2.equals("option")) {
                                    options.put(subel.getAttribute("type"), subel.getTextContent());
                                }
                            }
                        }
                        String[] chanarr = channels.toArray(new String[channels.size()]);
                        String type = el.getAttribute("type");
                        if (type.equals("blackjack")) {
                            basebot.addListener(server, new BJGame(bot), chanarr);
                        } else if (type.equals("help")) {
                            basebot.addListener(server, new Help(), chanarr);
                        } else if (type.equals("coins")) {
                            basebot.addListener(server, new Coins(), chanarr);
                        } else if (type.equals("usercommands")) {
                            basebot.addListener(server, new UserCommands(), chanarr);
                        } else if (type.equals("roulette")) {
                            basebot.addListener(server, new Roulette(
                                    Integer.parseInt(options.get("time")), chanarr[0], bot));
                        } else if (type.equals("overunder")) {
                            basebot.addListener(server, new OverUnder(), chanarr);
                        } else if (type.equals("createtimedroll")) {
                            basebot.addListener(server, new CreateTimedRoll(), chanarr);
                        } else if (type.equals("diceduel")) {
                          basebot.addListener(
                                  server, new DiceDuel(bot, chanarr[0]),
                                   chanarr);
                        } else if (type.equals("managersystem")) {
                          basebot.addListener(server,
                                    new ManagerSystem("#" + options.get("managementchan"), 
                                            "#" + options.get("activechan"),/*FIXME*/"", bot),
                                   chanarr);
                        }  else if (type.equals("rps")) {
                          RPSGame rpsevent = new RPSGame();
                            rpsevent.addValidChan(chanarr);
                            rpsevent.addAnnounce("#" + options.get("announce"), bot);
                            basebot.addListener(server, rpsevent);
                        } else if (type.equals("poker")) {
                            Client poker = new Client(server, chanarr[0]);
                            poker.initialise();
                            basebot.addListener(server, poker);
                        }
                       
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Goes through and loads all the timers and options in the xml file.
         * @param name the name of the bot
         * @param xmldocument the prepared xml doc
         * @param server    the server we are on
         * @param bot   the bot itself
         */
        private void loadTimers(final String server,
                                   final IrcBot bot) {
            try {
                
                BaseBot basebot = BaseBot.getInstance();
                XPath xPath =  XPathFactory.newInstance().newXPath();
 
                String expression = "/bot/*";
                //read a nodelist using xpath
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, 
                                                            XPathConstants.NODESET);
                  
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element el = (Element) nodeList.item(i);
                    String n = el.getNodeName();
                   
                    if (n.equals("timer")) {
                       HashMap<String, String> options = new HashMap<String, String>();
                       ArrayList<String> channels = new ArrayList<String>();
                        //get child nodes of channel and options
                        if (el.hasChildNodes()) {
                            // if has id use that so we can distinguish between multiple listeners
                            // of the same type
                            if (el.hasAttribute("id")) {
                                //System.out.println(el.getNodeName() + "::" + el.getAttribute("id"));
                                expression = "/bot/timer[@type='"
                                        + el.getAttribute("type") + "' and @id='"
                                        + el.getAttribute("id") + "']/*";
                            } else {
                                expression = "/bot/timer[@type='"
                                                             + el.getAttribute("type") + "']/*";
                            }
                            //read a nodelist using xpath
                            NodeList cNodes = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
                            //NodeList cNodes = el.getChildNodes();
                            for (int j = 0; j < cNodes.getLength(); j++) {
                                Element subel = (Element) cNodes.item(j);
                                String n2 = subel.getNodeName();
                                if (n2.equals("channel")) {
                                    // if we have a channel, create the game
                                    String channel = "#" + subel.getTextContent();
                                    channels.add(channel);
                                } else if (n2.equals("option")) {
                                    options.put(subel.getAttribute("type"), subel.getTextContent());
                                }
                            }
                        }
                        String[] chanarr = channels.toArray(new String[channels.size()]);
                        String type = el.getAttribute("type");
                        if (type.equals("trc")) {
                            @SuppressWarnings("unused")
                            TimedRollComp trcevent = new TimedRollComp(bot,
                                    chanarr[0], ProfileType.EOC,
                                    Integer.parseInt(options.get("prize")), 
                                    Integer.parseInt(options.get("mins")), 
                                    Integer.parseInt(options.get("rounds")), 
                                    null);
                        } else if (type.equals("managerannounce")) {
                            ManagerAnnounce mgrano = new ManagerAnnounce(
                                    basebot.getBot(server), chanarr[0]);
                            mgrano.begin(0);
                        } else if (type.equals("bettimer")) {
                            Timer bettimer = new Timer(true);
                            bettimer.scheduleAtFixedRate(new BetDetails(basebot.getBot(server),
                                    chanarr[0]), 
                                    Utils.tryParse(options.get("delay")) * Utils.MS_IN_MIN, 
                                    Utils.tryParse(options.get("period")) * Utils.MS_IN_MIN);
                        } else if (type.equals("comptimer")) {
                            Timer comptimer = new Timer(true);
                            comptimer.scheduleAtFixedRate(new Competition(basebot.getBot(server),
                                    chanarr[0]), 
                                    Utils.tryParse(options.get("delay")) * Utils.MS_IN_MIN, 
                                    Utils.tryParse(options.get("period")) * Utils.MS_IN_MIN);
                        } else if (type.equals("jackpottimer")) {
                            Timer jkpttimer = new Timer(true);
                            jkpttimer.scheduleAtFixedRate(new JackpotAnnounce(basebot.getBot(server),
                                    chanarr[0]), 
                                    Utils.tryParse(options.get("delay")) * Utils.MS_IN_MIN, 
                                    Utils.tryParse(options.get("period")) * Utils.MS_IN_MIN);
                        }
                       
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Initialises basebot and returns the server name.
         * @param xmldocument   the xmldocument that contains the settings
         * @return  the server address in string format
         */
        private String initBaseBot() {
            BaseBot basebot = BaseBot.getInstance();
            String nick = null;
            String botname = null;
            String pass = null;
            String server = null;
            int port = 0;
            
           
            try {
                XPath xPath =  XPathFactory.newInstance().newXPath();
                
               String expression = "/bot/*";
                //read a nodelist using xpath
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, 
                                                            XPathConstants.NODESET);
                  
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element el = (Element) nodeList.item(i);
                    String n = el.getNodeName();
                    String content = el.getTextContent();
                    if (n.equals("nick")) {
                        nick = content;                
                    } else if (n.equals("name")) {
                        botname = content;               
                    } else if (n.equals("ircpw")) { 
                        pass = content;                
                    } else if (n.equals("server")) {
                        server = content; 
                        //servername = el.getAttribute("name");
                    } else if (n.equals("port")) {
                        port = Integer.parseInt(content);                
                    } else if (n.equals("ip")) {
                        // FIXME What to do with the IP / host
                        System.out.println("IP :: " + content);                
                    } else if (n.equals("rake")) {
                        // Set up jackpot chan
                        Rake.init("#" + content);
                    }
                }
                if (nick != null && botname != null && pass != null 
                        && server != null && port != 0) {
                    basebot.initialise(nick, pass, botname, true, false, true);
                    basebot.addServer(server, server, port);
                } else {
                    EventLog.fatal(null, "XMLLoader", "initBaseBot");
                    System.exit(-1);
                }
            } catch (Exception e) {
                EventLog.fatal(e, "XMLLoader", "initBaseBot");
            }
           
            return server;
        }
    }