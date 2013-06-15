package org.smokinmils.settings;

/**
 * Variables used throughout the poker game.
 * 
 * @author Jamie Reid
 */
public final class PokerVars {
    /**
     * Hiding the default constructor.
     */
    private PokerVars() {

    }

    /** The string used to create table channels. */
    public static final String    TABLECHAN         = "#SM_Poker";

    /** The minimum number of players. */
    public static final int       MINPLAYERS        = 2;

    /** The minimum buy in in BB. */
    public static final int       MINBUYIN          = 35;

    /** The maximum buy in in BB. */
    public static final int       MAXBUYIN          = 100;

    /** The allowed big blinds. */
    public static final Integer[] ALLOWEDBB         = { 2, 4, 6, 8, 10 };

    /** The table sizes. */
    public static final Integer[] ALLOWEDTBLSIZES   = { 2, 8 };

    /** Number of seconds to wait for actions. */
    public static final int       ACTIONSECS        = 40;

    /** Time before a warning is given for actions. */
    public static final int       ACTIONWARNINGSECS = 10;

    /** Minutes you can sit out for. */
    public static final int       SITOUTMINS        = 5;

    /** Minutes for an announce. */
    public static final int       ANNOUNCEMINS      = 5;

    /** Number of times to wait for players before closing table. */
    public static final int       WAITTIMES         = 15;

    /** Time between player waiting message. */
    public static final int       PLAYERWAITSECS    = 30;

    /** Time before starting the game. */
    public static final int       GAMESTARTSECS     = 10;

    /** Time to show cards. */
    public static final int       SHOWCARDSECS      = 10;

    /** Minimum rake taken. */
    public static final int       MINRAKE           = 1;

    /** Percentage of rake taken. */
    public static final double    RAKEPERCENT       = 0.05;

    /** Minimum pot to take rake. */
    public static final int       MINPOTFORRAKE     = 4;
}
