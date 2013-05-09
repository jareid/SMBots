package org.smokinmils.pokerbot.settings;

/**
 * Variables used throughout the poker bot
 * 
 * @author Jamie Reid
 */
public class Variables {
	public static final String 	Nick = 				"SM_Poker";
	public static final String 	Login = 			"smokinmils";
	public static final String 	NickServPassword =	"smokinmilsdev";
	
	public static final String 	FingerMsg = 		"";
	 
	public static final String 	LobbyChan = 		"#smokin_dice";
	public static final String 	TableChan = 		"#SM_Poker";
	
	public static final String 	Server = 			"irc.swiftirc.net";
	public static final int 	Port =				6667;
	
	public static final int 	MessageDelayMS = 	0;
	public static final int		ReconnectMS	=		2500;
	
	public static final int MinPlayers = 2;
	public static final int MaxPlayers = 2;
	
	public static final int MinBuyIn = 35;
	public static final int MaxBuyIn = 100;
	
	public static final Integer[] AllowedBigBlinds = {2, 4, 6, 8, 10};
	
	public static final Integer[] AllowedTableSizes = {2, 8};
	
	public static final int ActionTimeSecs = 40;
	public static final int ActionWarningTimeSecs = 10;
	
	public static final int MaxSitOutMins = 5;
	
	public static final int AnnounceMins = 5;
	
	public static final int MaxWaitCount = 15;
	
	public static final int WaitingForPlayersSecs = 30;
	
	public static final int GameStartSecs = 10;
	
	public static final int ShowCardSecs = 10;
	
	public static final int MinimumRake = 1;
	public static final int MaximumRake = 100;
	public static final int RakePercentage = 5;
	public static final int MinimumPotForRake = 4;
	
	public static final int JackpotChance = 10000;
	public static final int JackpotRakePercentage = 34;
	
	/**
	 * NickServ's STATUS command can produce the following results:
	 * 0 - no such user online or nickname not registered
	 * 1 - user not recognized as nickname's owner
	 * 2 - user recognized as owner via access list only
	 * 3 - user recognized as owner via password identification
	 * This number restricts the user from using commands until they have the specified status.
	 */
	public static final int RequiredStatus = 2;	
}
