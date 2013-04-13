/**
 * This file is part of the 'pokerbot' a commercial IRC bot that 
 * allows users to play Texas Hold'em poker from IRC
 * 
 * The project was commissioned by Julian Clark
 * 
 * Copyright (C) 2013 Jamie Reid
 */ 
package org.smokinmils.pokerbot.settings;

/**
 * Strings used throughout the poker bot
 * 
 * All string specified in this file are allowed to use the following variable:
 * 
 * %c - Adds the IRC colour character, should be followed with # or #,#
 * %b - Adds the IRC bold character
 * %u - Adds the IRC underline character
 * %i - Adds the IRC italic character
 * %n - Removes all the IRC formatting specified prior to this
 * 
 * Each string may also use it's own specific variables
 * 
 * @author Jamie Reid
 */
public final class Strings {
	/** The letter than prefixes all command */
	public static final char 	CommandChar =		'!';
	
	/** 
	 * This defines the output of a card
	 * 
	 * %suitC - The colour for this suit
	 * %rank - The card rank
	 * %suit - The card suit
	 */	
	public static final String CardText = " %suitC %rank  %suit %n";
	
	/**
	 * This defines the text for suits
	 */
	public static final String CardText_Diamonds = "dia";
	public static final String CardText_Hearts = "hea";
	public static final String CardText_Clubs = "clu";
	public static final String CardText_Spades = "spa";	
	
	/**
	 * This defines the colour for suits
	 */
	public static final String CardColours_Diamonds = "00,05";
	public static final String CardColours_Hearts = "00,04";
	public static final String CardColours_Clubs = "00,01";
	public static final String CardColours_Spades = "00,14";
	
	/**
	 * This string is output when the user does not meet the above status with NickServ
	 */
	public static final String NotIdentifiedMsg = "%b%c12You must be identified with %c04NickServ%c12 to use the bot commands";
	
	/**
	 * The following list of settings are the commands for the lobby in the following order
	 * Info command
	 * Chips command
	 * Tables command
	 * Join command
	 * Promotions command
	 */
	public static final String InfoCommand = CommandChar + "info";
	public static final String InfoCommand_Desc = "%b%c12Receive help on the available commands";
	public static final String InfoCommand_Format = "%b%c12" + InfoCommand + " ?command|lobby|table?";
	
	public static final String ChipsCommand = CommandChar + "check";
	public static final String ChipsCommand_Desc = "%b%c12Query the bot about how many chips you or someone else has";
	public static final String ChipsCommand_Format = "%b%c12" + ChipsCommand + " ?user?";
	
	public static final String GiveCommand = CommandChar + "chips";
	public static final String GiveCommand_Desc = "%b%c12Give a user a number of chips to a certain game profile";
	public static final String GiveCommand_Format = "%b%c12" + GiveCommand + " <user> <amount> <profile>";
	
	public static final String ProfileCommand = CommandChar + "profile";
	public static final String ProfileCommand_Desc = "%b%c12Changes the active profile for you";
	public static final String ProfileCommand_Format = ProfileCommand + "<profile>";
	
	public static final String ProfilesCommand = CommandChar + "profiles";
	public static final String ProfilesCommand_Desc = "%b%c12Lists the available profiles";
	public static final String ProfilesCommand_Format = "%b%c12" + ProfilesCommand + "";
	
	public static final String NewTablCommand = CommandChar + "start";
	public static final String NewTablCommand_Desc = "%b%c12Creates a new table with big_blind as the Big blind.\n"
													 + "%b%c12Big blind will always be made even.\n"
													 + "%b%c12If you specify profile it creates a table for that profile, if you don't it uses your active profile";
	public static final String NewTablCommand_Format = "%b%c12" + NewTablCommand + " <big blind> <buy in> <max players> ?profile?";
	
	public static final String WatchTlCommand = CommandChar + "watch";
	public static final String WatchTlCommand_Desc = "%b%c12Adds you to the observer list for the table";;
	public static final String WatchTlCommand_Format = "%b%c12" + WatchTlCommand + " <table id>";
	
	public static final String TablesCommand = CommandChar + "tables";
	public static final String TablesCommand_Desc = "%b%c12Provides a list of the currently running tables\n"
												  + "%b%c12If a stake is provided, only displays tables with that size big blind";
	public static final String TablesCommand_Format = "%b%c12" + TablesCommand + " ?big blind?";
	
	public static final String JoinCommand = CommandChar + "join";
	public static final String JoinCommand_Desc = "%b%c12Joins a table with the selected ID\n"
												+ "%b%c12Will buy in for the specified amount.\n"
												+ "%b%c12Buy in must be between the table minimum and maximum"
												+ "%b%c12If no buy in is specified, you will be seated with the minimum chips";
	public static final String JoinCommand_Format = "%b%c12" + JoinCommand + " <table id> ?buy in?";
	
	public static final String PromosCommand = CommandChar + "promos";
	public static final String PromosCommand_Desc = "%b%c12Lists all the currently running promotions and the prizes you can win";
	public static final String PromosCommand_Format = "%b%c12" + PromosCommand + "";
	
	/**
	 * The following list of settings are the commands for tables in the following order
	 * Check command
	 * Description of Check command
	 * Bet command
	 * Fold command
	 * Rebuy command
	 * Leave command
	 */
	public static final String CheckCommand = CommandChar + "c";
	public static final String CheckCommand_Desc = "%b%c12Check on the current round" +
												   "%b%c12Aliases: !c / !check / !call all work for this command";
	public static final String CheckCommand_Format = CheckCommand + "";
	public static final String[] CheckCommand_Alternatives = {"!check", "!call"};
	
	public static final String BetCommand = CommandChar + "r";
	public static final String BetCommand_Desc = "%b%c12Place a bet/Raise for the current round.\n " +
												 "%b%c12Aliases: !bet / !raise / !b / !r all work for this command";
	public static final String BetCommand_Format = "%b%c12" + BetCommand + "<amount>";
	public static final String[] BetCommand_Alternatives = {"!b", "!bet", "!raise"};
	
	public static final String FoldCommand = CommandChar + "f";
	public static final String FoldCommand_Desc = "%b%c12Fold your current hand\n"
												+ "%b%c12If it is free to play/check, you will be checked." +
												   "%b%c12Aliases: !f / !fold all work for this command";
	public static final String FoldCommand_Format = "%b%c12" + FoldCommand + "";
	public static final String[] FoldCommand_Alternatives = {"!fold"};
	
	public static final String RebuyCommand = CommandChar + "rebuy";
	public static final String RebuyCommand_Desc = "%b%c12Rebuys chips for this table\n"
												 + "%b%c12You are only allowed to re-purchase chips if you have less than the table maximum"
												 + "%b%c12Chips can be bought to make your total equal the maxium";
	public static final String RebuyCommand_Format = "%b%c12" + RebuyCommand + "<amount>";
	
	public static final String SitDownCommand = CommandChar + "sitdown";
	public static final String SitDownCommand_Desc = "%b%c12Sit back down and start playing";
	public static final String SitDownCommand_Format = "%b%c12" + SitDownCommand + "";
	
	public static final String SitOutCommand = CommandChar + "sitout";
	public static final String SitOutCommand_Desc = "%b%c12Sit out and take a break";
	public static final String SitOutCommand_Format = "%b%c12" + SitOutCommand + "";
	
	public static final String LeaveCommand = CommandChar + "leave";
	public static final String LeaveCommand_Desc = "%b%c12Sit out and take a break"
												 + "%b%c12If you try this while still in play, the chips from that hand will be forfeited";
	public static final String LeaveCommand_Format = "%b%c12" + LeaveCommand + "";

	public static final String InvalidArgs = "%b%c12You provided invalid arguments for the command. The format is:";
	
	/**
	 * This string is used for the topic in every lobby
	 */
	public static final String LobbyTopic = "%b%c12Welcome to the %c04Smokin Mils Poker Lobby%c12 - Type %c04" 
											+ InfoCommand + "%c12 for help. Ready for testing, ask a host for play chips, ensure you do %c04!profile play%c12 before playing ";
	
	/**
	 * This string is used for the topic in each poker room
	 * 
	 * %id - The tableID
	 * %sb - The table's small blind
	 * %bb - The table's big blind
	 * %min - The minimum buy in
	 * %max - The maximum buy in
	 * %Pcur - The number of people sat down
	 * %Pmin - The minimum number of people for play to commence
	 * %Pmax - The maximum number of people for play to commence
	 * %seats - The number of available seats
	 * %watching - The number of people watching
	 * %hID - The id of the currently playing hand
	 */
	public static final String TableTopic = "%b%c12Welcome to the SM Poker Table %c04%id%c12 - SB: %c04%sb%c12 / BB: %c04%bb%c12 (Buy In: %c04%min%c12-%c04%max%c12) - Players: %c04%Pcur%c12 of %c04%Pmax%c12 - HandID: %c04%hID%c12";
	
	
	public static final String InfoMessage = "%b%c12For more information, please supply " + InfoCommand + " with one of the following options:\n" +
											 "%b%c12Lobby Commands: %c04info chips tables promos start join watch profile profiles\n" +
											 "%b%c12Table Commands: %c04c r f rebuy leave sitdown sitout\n" +
										 	 "%b%c12All commands for room type: table lobby";
	
	public static final String Promotions = "%b%c12There are currently no promtions running";
	
	public static final String ValidProfiles = "%b%c12Valid profiles are: %c04%profiles";
	
	public static final String ProfileChanged = "%b%c04%user %c12is now using the %c04%profile%c12 game profile";
	public static final String ProfileChangeFail = "%b%c04%user %c12tried to change to the %c04%profile%c12 game profile and it failed. Please try again!";
	
	public static final String GiveChips = "%b%c04%sender:%c12 Added %c04%amount%c12 chips to the %c04%profile%c12 account of %c04%who%c12";
	public static final String GiveChipsPM = "%b%c12You have had %c04%amount%c12 chips deposited into your account by %c04%sender%c12";
	
	/**
	 * This string is used to specify the message sent when a user checks their credit in the system
	 * 
	 * %sender - the person who sent the command
	 * %user - the person who the credit check is for
	 * %creds - the amount of credits
	 * %active - the active profile name
	 */
	public static final String CheckCreditMsg =  "%b%c04%sender%c12: %user %c12currently has %c04%creds%c12 chips on the active profile(%c04%active%c12)";
	public static final String CheckCreditSelfMsg =  "%b%c04%sender%c12: You %c12currently have %c04%creds%c12 chips on the active profile(%c04%active%c12)";
	public static final String CreditsOtherProfiles = "%c04%name%c12(%c04%amount%c12)";
	public static final String NoCredits = "%b%c04%sender%c12: %user %c12currently has %c04%creds%c12%sender: %user %c12currently has %c04no%c12 available chips.";
	public static final String NoCreditsSelf = "%b%c04%sender%c12: You %c12currently have %c04%creds%c12%sender: %user %c12currently has %c04no%c12 available chips.";
	
	/**
	 * This string is used when a user asks for all tables
	 * 
	 * %count - The big blind the user searched for
	 */
	public static final String AllTablesMsg = "%b%c12There are currently %c04%count%c12 tables. They are:";

	/**
	 * This string is used for the output of each individual table
	 * 
	 * %id - The table ID
	 * %bb - The blind blind for this table
	 * %minP - The minimum players for play to commence on this table
	 * %maxP - The maximum players for play to commence on this table
	 * %curP - The current number of players seated
	 * %profile - The name of the profile this table's chips are for.
	 */
	public static final String TableInfoMsg = "%b%c12TableID: %c04%id%c12 | Big Blind: %c04%bb%c12 | Seated: %c04%curP%c12 (Min: %c04%minP%c12 / Max: %c04%maxP%c12) | Profile: %c04%profile";
	
	/**
	 * This string is used to specify the message sent when a user searches for tables and:
	 * - Finds none
	 * 
	 * %bb - The big blind the user searched for
	 */
	public static final String NoTablesMsg = "%b%c12There are current no tables with a big blind of %c04%bb\n"
											 + "%b%c12Please start one with the command %c04" + NewTablCommand;
	
	/**
	 * This string is used to specify the message sent when a user searches for tables and:
	 * - Finds some
	 * 
	 * %bb - The big blind the user searched for
	 * %count - the number of tables found
	 * %tables - the list of table ids
	 */
	public static final String FoundTablesMsg = "%b%c12There are current %c04%count tables with a big blind of %c04%bb\n"
											 	 + "%b%c12The table IDs are: %c04%tables";
	
	
	/**
	 * This string is used when a user tries to create a table with a invalid size
	 * 
	 * %size - The size of table the user tried to create
	 * %allowed - The allowed tables list
	 */
	public static final String InvalidTableSizeMsg = "%b%c12Sorry, %c04%size%c12 is not an allowed table size\n" 
													+ "%b%c12Valid Table Sizes are: %c04%allowed";
	
	/**
	 * This string is used when a user tries to create a table with a invalid size
	 * 
	 * %bb - The size of table the user tried to create
	 * %allowed - The allowed tables list
	 */
	public static final String InvalidTableBBMsg = "%b%c12Sorry, %c04%bb%c12 is not an allowed table big blind\n" 
													+ "%b%c12Valid Big Blinds are: %c04%allowed";
	
	/**
	 * This string is used when a user attempts to join a poker table with an incorrect ID
	 * 
	 * %id - The big blind the user searched for
	 */
	public static final String NoTableIDMsg = "%b%c12No poker table with ID %c04%id%c12 exists";
	
	/**
	 * This string is used when a user attempts to join a table with:
	 * - less than the minimum buy in
	 * - more than the maximum buy in
	 * 
	 * %buyin - The big blind the user searched for
	 * %minbuy - The minimum amount of chips
	 * %maxbuy - The maximum amount of chips
	 * %minBB - The minimum number of big blinds
	 * %maxBB -  The maximum number of big blinds
	 */
	public static final String IncorrectBuyInMsg = "%b%c12Sorry, your buyin must be between %c04%minBB%c12x and %c04%maxBB%c12x "
													+ "the table Big Blind (%c04%minbuy%c12 <= %c04%buyin%c12 <= %c04%maxbuy%c12)";
	
	/**
	 * This string is used when a suer attempts to join a full table
	 * 
	 * %id - The table id
	 */
	public static final String TableFullMsg = "%b%c12Sorry you can not join table %c04%id%c12 as it is currently full";	
	
	/**
	 * This string is used when a user doesn't have enough chips
	 * 
	 * %chips - The amount the user tried to spend
	 */
	public static final String NoChipsMsg = "%b%c12Sorry, you do not have %c04%chips%c12 chips available for the %c04%profile%c12 profile.";
	
	/**
	 * This string is used to specify the message sent when a user enters an invalid option
	 * for the info command
	 * 
	 * %invalid - the invalid arguments supplied
	 */
	public static final String InvalidInfoArgs =  "%b%c04%invalid%c12 is invalid. Valid choices: "
	  		  									+ "info chips tables promos start join watch profile profiles c r f rebuy leave sitdown sitout"; 
	
	/**
	 * This string is used to specify the message sent when a user attempts to watch a table they are already watching
	 * 
	 * %id - the table ID
	 */
	public static final String AlreadyWatchingMsg =  "%b%c12You are already watching table %c04%id";
	
	/**
	 * This string is used to specify the message sent when a user attempts to join a table they are already playing at
	 * 
	 * %id - the table ID
	 */
	public static final String AlreadyPlayingMsg =  "%b%c12You are already playing at table %c04%id";
	
	/**
	 * These settings set the amount of time between announcing the message and 
	 * message we announce when we need players
	 * 
	 * Variables:
	 * %needP - number of players required before play starts
	 * %minP - minimum number of players on the table
	 * %maxP - maximum number of players on the table
	 * %seatedP - number of players currently sat down
	 */
	public static final int WaitingForPlayersSecs = 15;
	public static final String WaitingForPlayersMsg = "%b%c12We are currently waiting for %need more players (%c04%seated%c12 of %c04%max%c12 seated) [Min: %c04%min%c12]";
	
	/**
	 * These setting set the amount of time between announcing the message and 
	 * message we announce when we need players
	 * 
	 * Variables
	 * %secs - Seconds until play begins
	 * %bb - Big Blind
	 * %sb - Small blind
	 * %seatedP - number of players currently sat down
	 */
	public static final int GameStartSecs = 10;
	public static final String GameStartMsg = "%b%c12We now have %c04%seatedP%c12 players and the game (SB: %c04%sb%c12 / BB: %c04%bb%c12) will begin momentarily";
	
	
	/**
	 * This setting is used for the message when a new hand commences
	 * 
	 * %hiD - the id of the hand
	 * %dealer - the name of the user in dealer position
	 * %sb - The name of the user in small blind position
	 * $bb - The name of the user in the bib blind position
	 */
	public static final String NewHandMessage = "%b%c12Hand ID#%c04%hID%c12 started (Dealer %c04%dealer%c12 | SB: %c04%sb%c12 | BB %c04%bb%c12)";
	
	/**
	 * This setting is used for the message when a big blind is posted
	 * 
	 * %player - the name of the player
	 * $bb - The name of the user in the bib blind position
	 */
	public static final String BigBlindPosted = "%b%c12Big blind (%c04%bb%c12) posted by %c04%player";
	
	/**
	 * This setting is used for the message when a big blind is posted
	 * 
	 * %player - the name of the player
	 * %sb - The name of the user in the bib blind position
	 */
	public static final String SmallBlindPosted = "%b%c12Small blind (%c04%sb%c12) posted by %c04%player";
	
	/**
	 * This setting is used for the message when the player's hole cards have been dealt
	 * 
	 * %id - the tableID
	 * %hID - the hand ID
	 * %card1 - The first hole card
	 * %card2 - The second hole card
	 */
	public static final String HoleCardsDealtPlayer = "%b%c12[Table %c04%id%c12] (%c04#%hID%c12) Your hole cards are:\n"
													+ "%b%c12[Table %c04%id%c12] (%c04#%hID%c12) %card1 %card2\n"
													+ "%b%c12[Table %c04%id%c12] (%c04#%hID%c12) %card1 %card2\n"
													+ "%b%c12[Table %c04%id%c12] (%c04#%hID%c12) %card1 %card2";
	
	/**
	 * This setting is used for the message when the player's hole cards have been dealt
	 * 
	 * %hID - the hand ID
	 */
	public static final String HoleCardsDealt = "%b%c12The hole cards for this hand (%c04#%hID%c12) have been dealt...";
	
	/**
	 * This setting is used for the message when the player's hole cards have been dealt
	 * 
	 * %hID - the hand ID
	 * %round - the round name
	 * %card - the cards
	 */
	public static final String CommunityDealt = "%b(%c04#%hID%c12) %c04%round%c12 has been dealt:\n"
											  + "%b(%c04#%hID%c12) %cards\n"
											  + "%b(%c04#%hID%c12) %cards\n"
											  + "%b(%c04#%hID%c12) %cards";
	
	/**
	 * This setting is used for the message when the player's hole cards have been dealt
	 * 
	 * %id - table ID
	 * %hID - the hand ID
	 * %round - the round name
	 * %cards - The cards the player currently has
	 */
	public static final String CommunityDealtPlayer = "%b%c12[Table %c04%id%c12] (%c04#%hID%c12) Your hand is now: \n"
													+ "%b%c12[Table %c04%id%c12] (%c04#%hID%c12)%cards\n"
													+ "%b%c12[Table %c04%id%c12] (%c04#%hID%c12)%cards\n"
													+ "%b%c12[Table %c04%id%c12] (%c04#%hID%c12)%cards";
	
	/**
	 * This setting is used for the message when a player acts out of turn
	 * 
	 * %hID - the handID
	 * %user - The user who tried to act
	 * %actor - The user due to act
	 */
	public static final String InvalidActor = "%b%c12(%c04#%hID%c12) %c04%user%c12 acted out of turn, %c04%actor%c12 to act...";
	
	/**
	 * This setting is used for the message when a player acts out of turn
	 * 
	 * %hID - the handID
	 * %invalid - The invalid actions
	 * %valid - The valid actions
	 */
	public static final String InvalidAction = "%b%c12(%c04#%hID%c12) %c04%invalid%c12 is not valid. Only %c04%valid";
	
	/**
	 * This setting is used for the message when a player acts out of turn
	 * 
	 * %hID - the handID
	 * %valid - The valid actions
	 * %actor
	 */
	public static final String GetAction = "%b%c12(%c04#%hID%c12) %c04%actor%c12 to act. Valid actions: %c04%valid";
	
	/**
	 * Table action
	 * 
	 * %hID - the handID
	 * %action - The action
	 * %actor - The person who acted
	 * %pot - The size of the pot
	 * %chips - Remaining chips left for actor
	 */
	public static final String TableAction = "%b%c12(%c04#%hID%c12) %c04%actor %action %amount %c12(Pot: %c04%pot%c12) [%c04%actor%c12 has %c04%chips%c12 chips now]";
	
	/**
	 * A message for when someone goes all in
	 * 
	 * %hID - the handID
	 * %actor - The person who acted
	 */
	public static final String PlayerAllIn = "%b%c12(%c04#%hID%c12) %c04%actor%c12 is all in.";
	
	/**
	 * A message for when someone is running out of time to act
	 * 
	 * %hID - the handID
	 * %actor - The person who failed to act
	 * %secs - The number of seconds they have left to act
	 */
	public static final String NoActionWarning = "%b%c12(%c04#%hID%c12)has %c04%secs%c12 seconds to act.";
	
	/**
	 * A message for when someone fails to act
	 * 
	 * %hID - the handID
	 * %actor - The person who failed to act
	 */
	public static final String NoAction = "%b%c12(%c04#%hID%c12) No action received within the time. %c04%actor%c12 has folded";
	
	/**
	 * Message when someone makes an invalid bet
	 * 
	 * %hID - The hand ID
	 * %pChips - Amount of chips the player has
	 * %min - The minimum valid bet
	 */
	public static final String InvalidBet = "%b%c12You can not bet less than the minimum %c04(%min%c12). You have %c04%pChips";
	
	
	/**
	 * Message when a new table is created
	 * 
	 * %tID - the table ID
	 * %bb - The big blind
	 * %sb - The small blind
	 * %Pmin - Minimum Players
	 * %Pmax - Maximum Players
	 * %min - Minimum BuyIn
	 * %max - Maximum BuyIn
	 */
	public static final String NewTable = "%b%c12Table %c04%id%c12 has been created for %c04%Pmin%c12 to %c04%Pmax%c12 players with a BB of %c04%bb%c12 (SB: %c04%sb%c12) [Buy In: %c04%min%c12 to %c04%max%c12]";
	
	/**
	 * Message when a table is closed
	 * 
	 * %tID - the table ID
	 */
	public static final String TableClosed = "%b%c12Table %c04%id%c12 has been shut down due to lack of players";
	
	/**
	 * Message when someone joins a table
	 * 
	 * %id - the table ID
	 * %bb - The big blind
	 * %sb - The small blind
	 * %Pmin - Minimum Players
	 * %Pmax - Maximum Players
	 * %Pcur - People playing
	 * %sears - Seats left
	 * %watchint - People Watching
	 * %min - Minimum BuyIn
	 * %max - Maximum BuyIn
	 * %hID - Current HandID
	 */
	public static final String NewPersonAtTable = "%b%c12[Table %c04%id%c12] SB: %c04%sb%c12 / BB: %c04%bb%c12 (Buy In: %c04%min%c12-%c04%max%c12) - Players: %c04%Pcur%c12 of %c04%Pmax%c12 (%c04%seats%c12 seats left)";

	/**
	 * Message when a player is joining
	 * 
	 * %id% - table id
	 * %player - player who is sitting out
	 * %chips - number of chips brought to the table
	 */
	public static final String PlayerJoins = "%b%c12[Table %c04%id%c12]: %c04%player%c12 has joined the table with %c04%chips%c12 chips.";
	
	/**
	 * Message when a player is sitting out for parting/quiting/timing out
	 * 
	 * %id% - table id
	 * %player - player who is sitting out
	 */
	public static final String PlayerSitsOut = "%b%c12[Table %c04%id%c12] %player is now sitting out.";
	public static final String OutOfChips = "%b%c12[Table %c04%id%c12] %player is now sitting out as they have no chips left";

	/**
	 * Player can't sit out private message
	 * 
	 * %id - table id
	 */
	public static final String SitOutFailed = "%b%c12[Table %c04%id%c12] You can not sit out, you are already sitting out";
	
	/**
	 * Can't sit down, rebuy first message
	 * 
	 * %id - table id
	 */
	public static final String SitDownFailed = "%b%c12[Table %c04%id%c12] You can not sit down, please rebuy first";
	
	/**
	 * Rebuy successful message
	 * 
	 * %id - table id
	 * %user - The player
	 * %new - new chips total
	 * %total - the user's total chips
	 */
	public static final String RebuySuccess = "%b%c12[Table %c04%id%c12] %user has bought %new chips. They now have %total chips.";
	
	/**
	 * Message when a player is rejoining
	 * 
	 * %id% - table id
	 * %player - player who is sitting out
	 */
	public static final String PlayerSitsDown = "%b%c12[Table %c04%id%c12] %c04%player%c12 has sat down.";
	
	/**
	 * Message when a player is leaving
	 * 
	 * %id% - table id
	 * %player - player who is sitting out
	 * %chips - number of chips taken from the table
	 */
	public static final String PlayerLeaves = "%b%c12[Table %c04%id%c12] %c04%player%c12 has taken his chips (%c04%chips%c12) and left.";
	
	/**
	 * Private message to user when they leave
	 * 
	 * %id - table id
	 */
	public static final String PlayerLeavesPM = "%b%c12You have now left table %c04%id";
	
	/**
	 * Private message when a player won a hand by means of everyone folding
	 * 
	 * %id - table id
	 * %hID - hand id
	 * %who - The player who won
	 * %amount - amount won
	 * %total - player's new chip count
	 */
	public static final String PlayerWins = "%b%c12[Table %c04%id%c12] (#%c04%hID%c12) Congratulations, %c04%who%c12 won %c04%amount%c12 (Chips: %c04%total%c12) [Rake: %c04%rake%c12]";
	
	
	/**
	 * This is used for formatting action's to string
	 * 
	 * %action
	 * %cmd
	 */
	public static final String AllowedActionString = "%c04%action%c12%cmd%n";
}
