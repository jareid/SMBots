package org.smokinmils.casino;

// database imports
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import com.mchange.v2.c3p0.DataSources;
import javax.sql.DataSource;

import org.pircbotx.User;
import org.smokinmils.Database;
import org.smokinmils.bot.Bet;

public class Accounts {

	private HashMap<String, Integer> userIDs;

	private static Accounts instance = new Accounts();

	public static Accounts getInstance() {
		return instance;
	}

	private DataSource unpooled;
	private DataSource pooled;
	private String url = "jdbc:mysql://" + Settings.DBHOST + "/"
			+ Settings.DBNAME;

	private Accounts() {
		try {
			unpooled = DataSources.unpooledDataSource(url, Settings.DBUSER,
					Settings.DBPASS);
			pooled = DataSources.pooledDataSource(unpooled);
		} catch (Exception e) {
			System.out.println("Error pooling stuff");
		}
		userIDs = new HashMap<String, Integer>();
	}

	/**
	 * Get's a connection from the pool
	 * 
	 * @return Connection to do SQL stuffs
	 */
	private Connection getConnection() {
		try {
			return pooled.getConnection();
		} catch (SQLException e) {
			System.out.println("Unable to get pooled connection");
			return null;
		}
	}

	/**
	 * Run upon start up, refunds all active bets that were never called or
	 * rolled for since a crash / restart
	 */
	public void processRefunds() {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			// cache user ids in a HashMap for ease
			rs = stmt.executeQuery("SELECT id,username FROM users;");
			while (rs.next()) {
				userIDs.put(rs.getString("username").toLowerCase(),
						rs.getInt("id"));
			}

			for (int g = 1; g < 7; g++) {
				// refund each bet then delete
				rs = stmt
						.executeQuery("SELECT * FROM bets INNER JOIN users ON bets.userid=users.id AND bets.gameid="
								+ g + ";");
				while (rs.next()) {
					this.addChips(rs.getString("username"),
							rs.getString("profile"), rs.getInt("amount"), null);
					this.addTransaction(rs.getString("username"),
							rs.getString("profile"), 3, rs.getInt("amount"), 8); // log
																					// the
																					// refund
				}
				stmt.executeUpdate("DELETE FROM bets WHERE gameid=" + g + ";");
			}

		} catch (SQLException e) {
			System.out.println("Error initializing...");
			System.out.println(e.getMessage());
			System.exit(-1);
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(refund processing)");
			}
		}

	}

	/**
	 * Adds chips to the user on the specified profile, TODO - user/username?
	 * are both required etc
	 * 
	 * @param username
	 *            The name of the user we are adding chips to
	 * @param profile
	 *            Which profile are we adding to
	 * @param amount
	 *            How much are we adding?
	 * @param user
	 *            User object, used for adding hostmasks if they are online,
	 *            else send null
	 */
	public void addChips(String username, String profile, int amount, User user) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		username = username.toLowerCase();
		try {
			// get current chips if they have them, add amount and re insert
			// if they don't exist create a new entry with the user and amount
			// and profile
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt
					.executeQuery("SELECT COUNT(*) AS total from users WHERE username='"
							+ username + "';");
			rs.next(); // nasty, fix this up TODO
			int count = rs.getInt("total");
			if (count > 0) // user exists
			{
				// check profile exists
				rs = stmt
						.executeQuery("SELECT COUNT(*) AS total,amount,profile_type.id AS type,users.id AS user_id from users JOIN user_profiles JOIN profile_type WHERE users.username='"
								+ username
								+ "' AND user_profiles.user_id=users.id AND user_profiles.type_id=profile_type.id AND profile_type.name='"
								+ profile + "';");
				rs.next();
				if (rs.getInt("total") > 0) {
					// profile also exists,
					// update chops
					stmt.executeUpdate("UPDATE user_profiles SET amount="
							+ (rs.getInt("amount") + amount)
							+ " WHERE user_id=" + rs.getInt("user_id")
							+ " AND type_id=" + rs.getInt("type") + ";");

				} else {
					// no profile, but user, so create profile
					stmt.executeUpdate("INSERT INTO user_profiles (user_id, type_id, amount) VALUES ((SELECT id FROM users WHERE username='"
							+ username
							+ "')"
							+ ",(SELECT id FROM profile_type WHERE name='"
							+ profile + "')," + amount + ");");
					// set the active profile -- no longer as they are defaulted
					// to play
					// stmt.executeUpdate("UPDATE users SET active_profile="+this.getProfileID(profile)+" WHERE username='+username+'");
				}

			} else {
				// user doesn't exist, create them AND a profile and set active
				// profile and shizzle
				try {
					System.out.println("Adding new user.");
					stmt.executeUpdate("INSERT INTO users (username) VALUES('"
							+ username + "');");
					System.out.println("Adding new user's profile");
					// now that the user exists.. we can insert the new profile
					// to match the requirements
					stmt.executeUpdate("INSERT INTO user_profiles (user_id, type_id, amount) VALUES ((SELECT id FROM users WHERE username='"
							+ username
							+ "' LIMIT 1)"
							+ ",(SELECT id FROM profile_type WHERE name='"
							+ profile + "' LIMIT 1)," + amount + ");");

				} catch (SQLException e2) {
					System.out.println("Error in SQL query INSERTING NEW USER");
					System.out.println(e2.getMessage());

				}
			}

			// if user is not null, log the hostname
			// at this point the user should exist, or have just been created so
			// skip counting for now
			if (user != null) {
				int userID;
				if (userIDs.containsKey(username)) {
					userID = userIDs.get(username);
				} else {
					// stmt = con.createStatement();
					// user exists so we need to get the current amount, and
					// then update it
					rs = stmt
							.executeQuery("SELECT ID FROM users WHERE username='"
									+ username + "' LIMIT 1;");
					rs.next();
					userID = rs.getInt("id");
					userIDs.put(username, userID);

				}
				try {

					// check if they have logged on from this before
					rs = stmt
							.executeQuery("SELECT COUNT(*) AS total FROM hostmasks WHERE userid="
									+ userID
									+ " and host='"
									+ user.getHostmask() + "';");
					rs.next();
					if (rs.getInt("total") == 0) // ie if we haven't logged this
													// hostmask to this account
													// before
					{
						// if we haven't seen it before, add it now
						stmt.executeUpdate("INSERT INTO hostmasks (host, userid) VALUES('"
								+ user.getHostmask() + "'," + userID + ");");

					}

				} catch (SQLException ex) {
					System.out.println("Error adding in game name to DB...");
				}

				/*
				 * statement.executeUpdate(
				 * "INSERT INTO hostmasks (host, userid) VALUES('" +
				 * user.getHostmask() +"'," + userID + ");");
				 */

			}
		} catch (SQLException e) {

			System.out.println("Error in SQL query RETRIEVING USER");
			System.out.println(e.getMessage());
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(add chips)");
			}
		}

	}

	/**
	 * Removes chips from a user's profile,
	 * 
	 * @param username
	 *            The username we are working with
	 * @param profile
	 *            The profile that we want to remove from
	 * @param amount
	 *            The amount that will be removed
	 * @return True if success, false other wise
	 */
	public boolean removeChips(String username, String profile, int amount) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		boolean result = false;
		username = username.toLowerCase();
		profile = profile.toLowerCase();
		try {
			conn = getConnection();
			stmt = conn.createStatement();

			// get current chips if they have them, add amount and re insert
			// if they don't exist create a new entry with the user and amount
			int current;
			if (!profile.equalsIgnoreCase(this.getActiveProfile(username))) {
				// current profile != profile we are removing from.
				// get the hashmap to find the correct one?
				if (this.checkNonActiveChips(username).containsKey(profile)) {
					// they have chips here
					current = this.checkNonActiveChips(username).get(profile);
					if (current >= amount) {
						// enough to remove
						current = current - amount;
						stmt.executeUpdate("UPDATE user_profiles SET amount="
								+ current
								+ " WHERE user_id=(SELECT id FROM users WHERE username='"
								+ username
								+ "')"
								+ " AND type_id=(SELECT id FROM profile_type WHERE name='"
								+ profile + "');");
						result = true;
					}
				} else {
					result = false; // nope
				}

			} else {
				// active profile
				current = Database.getInstance().checkCredits(username);
				if (current >= amount) {
					// enough to remove
					current = current - amount;
					stmt.executeUpdate("UPDATE user_profiles SET amount="
							+ current
							+ " WHERE user_id=(SELECT id FROM users WHERE username='"
							+ username
							+ "')"
							+ " AND type_id=(SELECT id FROM profile_type WHERE name='"
							+ profile + "');");
					result = true;
				}
			}

		} catch (SQLException e) {
			System.out.println("Error in SQL query");
			System.out.println(e.getMessage());
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(rem chips)");
			}
		}
		return result;
	}

	/**
	 * Returns a Hashmap of all the non-active user chips
	 * 
	 * @param user
	 *            String of the username
	 * @return the hashmap...
	 */
	public HashMap<String, Integer> checkNonActiveChips(String user) {
		HashMap<String, Integer> retMap = new HashMap<String, Integer>();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		user = user.toLowerCase();
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT name,amount FROM user_profiles "
					+ "JOIN users JOIN profile_type "
					+ "WHERE users.username='" + user
					+ "' AND users.id=user_profiles.user_id "
					+ "AND profile_type.id=user_profiles.type_id "
					+ "AND profile_type.id!=users.active_profile;");
			while (rs.next()) {
				retMap.put(rs.getString("name"), rs.getInt("amount"));
			}
		} catch (SQLException ex) {
			System.out.println("Error doing non-active profile check!");
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(add ingame name)");
			}
		}

		return retMap;
	}

	/**
	 * Records a win in the database
	 * 
	 * @param username
	 *            The username who won -- Can this be done with checking
	 *            transaction log?
	 */
	public void recordWin(String username) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		// get current wins
		username = username.toLowerCase();
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT wins FROM users WHERE username='"
					+ username + "' LIMIT 1;");
			rs.next();
			int newWins = rs.getInt("wins") + 1;
			stmt.executeUpdate("UPDATE users SET wins=" + newWins
					+ " WHERE username='" + username + "';");
		} catch (SQLException se) {
			System.out.println("Error in Recording a Win");
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(rec win)");
			}
		}

	}

	// TODO do we need these any more since we have transaction logs we can use
	// for stats?

	/**
	 * Records a loss in the database
	 * 
	 * @param username
	 *            The username who loss
	 */
	public void recordLoss(String username) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		// get current wins
		username = username.toLowerCase();
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT losses FROM users WHERE username='"
					+ username + "' LIMIT 1;");
			rs.next();
			int newLosses = rs.getInt("losses") + 1;
			stmt.executeUpdate("UPDATE users SET losses=" + newLosses
					+ " WHERE username='" + username + "';");
		} catch (SQLException se) {
			System.out.println("Error in Recording a Loss");
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(rec loss)");
			}
		}

	}

	/**
	 * Records the amount bet into the database, this could probably be tidied
	 * up with transactions instead
	 * 
	 * @param username
	 *            The username who bet.
	 * @param amount
	 *            The amount they bet
	 */
	public void recordBet(String username, int amount) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		// get current wins
		username = username.toLowerCase();
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			// System.out.println("RS");
			rs = stmt
					.executeQuery("SELECT total_bets FROM users WHERE username='"
							+ username + "' LIMIT 1;");
			rs.next();
			int newBets = rs.getInt("total_bets") + amount;
			// System.out.println("UPDATE");
			stmt.executeUpdate("UPDATE users SET total_bets=" + newBets
					+ " WHERE username='" + username + "';");
		} catch (SQLException se) {
			System.out.println("Error in Recording a Bet");
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(rec loss)");
			}
		}

	}

	/**
	 * Adds a bet to the database
	 * 
	 * @param bet
	 *            The bet to add
	 * @param game
	 *            The game represented as a unique string
	 */
	public void addBet(Bet bet, int game) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		// get current wins
		conn = getConnection();

		int uid = 0;
		if (userIDs.containsKey(bet.getUser())) {
			uid = userIDs.get(bet.getUser().toLowerCase());
		} else {
			try {
				stmt = conn.createStatement();
				rs = stmt.executeQuery("SELECT id FROM users WHERE username='"
						+ bet.getUser().toLowerCase() + "' LIMIT 1;");
				rs.next();
				uid = rs.getInt("id");
				// uid = 1;
			} catch (SQLException ex) {
				System.out.println("Error retreiving UID in addBet");
			}
		}
		try {
			stmt = conn.createStatement();

			stmt.executeUpdate("INSERT INTO bets(userid, amount, choice, gameid, profile) VALUES("
					+ uid
					+ ","
					+ bet.getAmount()
					+ ",'"
					+ bet.getChoice()
					+ "'," + game + ", '" + bet.getProfile() + "');");
		} catch (SQLException e) {
			System.out.println("Unable to add bet to DB...");
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(add bet to db)");
			}
		}
	}

	/**
	 * Deletes a bet from the database
	 * 
	 * @param bet
	 *            The bet to remove
	 * @param game
	 *            The game represented as a unique string
	 */
	public void delBet(Bet bet, int game) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		conn = getConnection();
		try {
			int uid = 0;
			if (userIDs.containsKey(bet.getUser())) {
				uid = userIDs.get(bet.getUser());
			} else {
				try {

					stmt = conn.createStatement();
					rs = stmt
							.executeQuery("SELECT id FROM users WHERE username='"
									+ bet.getUser().toLowerCase()
									+ "' LIMIT 1;");
					rs.next();
					uid = rs.getInt("id");
					// uid = 1;
				} catch (SQLException ex) {
					System.out.println("Error retreiving UID in addBet");
				}
			}
			stmt = conn.createStatement();
			stmt.executeUpdate("DELETE FROM bets WHERE userid=" + uid
					+ " AND gameid=" + game + ";");
		} catch (SQLException e) {
			System.out.println("Unable to delete bet from DB...");
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(add bet to db)");
			}
		}
	}

	/**
	 * Adds a transaction to the database.
	 * 
	 * @param user
	 *            User who the transaction belongs to
	 * @param transaction_id
	 *            transaction typ
	 * @param amount
	 *            amount signed for losses
	 * @param game_id
	 *            Game id (or 0 if not applicable)
	 */
	public void addTransaction(String user, String profile, int transaction_id,
			int amount, int game_id) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		conn = getConnection();
		user = user.toLowerCase();

		try {
			int uid = 0;
			if (userIDs.containsKey(user)) {
				uid = userIDs.get(user);
			} else {
				try {

					stmt = conn.createStatement();
					rs = stmt
							.executeQuery("SELECT id FROM users WHERE username='"
									+ user + "' LIMIT 1;");
					rs.next();
					uid = rs.getInt("id");
					// uid = 1;
				} catch (SQLException ex) {
					System.out.println("Error getting uid in add transaction");
				}
			}
			stmt = conn.createStatement();
			stmt.executeUpdate("INSERT INTO transactions(user_id, game_id, type_id, amount, profile_type) VALUES("
					+ uid
					+ ","
					+ game_id
					+ ","
					+ transaction_id
					+ ","
					+ amount
					+ ", " + this.getProfileID(profile) + ");");
		} catch (SQLException e) {
			System.out.println("Unable to add Transaction...");
			e.printStackTrace();
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(adding transaction)");
			}
		}
	}

	/**
	 * Get's the user's active profile
	 * 
	 * @param user
	 *            User who the transaction belongs to
	 * @return USer's active profile as a string
	 */
	public String getActiveProfile(String username) {
		String profile = "eoc";
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		conn = getConnection();
		username = username.toLowerCase();

		try {
			stmt = conn.createStatement();
			rs = stmt
					.executeQuery("SELECT profile_type.name AS name FROM user_profiles "
							+ "JOIN profile_type JOIN users WHERE users.username='"
							+ username
							+ "' AND users.id=user_profiles.user_id "
							+ "AND user_profiles.type_id=profile_type.id "
							+ "AND user_profiles.type_id=users.active_profile;");
			if (rs.next()) {// TODO FIX THIS
				profile = rs.getString("name");
				// System.out.println("ping");
			}
			// else

		} catch (SQLException e) {
			System.out.println("Unable to find profile for user");

			e.printStackTrace();

		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(getUserProfile)");
			}
		}
		return profile;
	}

	/**
	 * Check if a profile is real.
	 * 
	 * @param profile
	 *            profile to check if real
	 * @return yay or nay on it being real
	 */
	public Boolean isValidProfile(String profile) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		conn = getConnection();
		boolean valid = false;

		try {
			stmt = conn.createStatement(); // probably could do this with count
			rs = stmt.executeQuery("SELECT name FROM profile_type WHERE name='"
					+ profile + "';");
			// TODO select count!?
			if (rs.next()) {
				// valid profile type
				valid = true;
			}
		} catch (SQLException e) {
			System.out.println("Unable to find profile for user x4");

			e.printStackTrace();

		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(getUserProfile)");
			}
		}
		return valid;
	}

	/**
	 * set's the user's active profile TODO make this a boolean?
	 * 
	 * @param profile
	 *            profile to check if real
	 */
	public void setProfile(String user, String profile) {

		profile = profile.toLowerCase();
		user = user.toLowerCase();
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		conn = getConnection();

		try {
			stmt = conn.createStatement();
			stmt.executeUpdate("UPDATE users,(SELECT id from profile_type WHERE name='"
					+ profile
					+ "') src SET users.active_profile=src.id WHERE users.username='"
					+ user + "'");
			// if they don't have a profile, create it?
			rs = stmt
					.executeQuery("SELECT COUNT(*) AS total FROM users JOIN user_profiles JOIN profile_type WHERE users.username='"
							+ user
							+ "' AND user_profiles.user_id=users.id AND user_profiles.type_id=profile_type.id AND profile_type.name='"
							+ profile + "'");
			rs.next();
			if (rs.getInt("total") == 0) {
				// the user_profiles doesn't exist
				stmt.executeUpdate("insert into user_profiles (user_id, type_id) values ((SELECT id FROM users WHERE username='"
						+ user
						+ "' LIMIT 1), (SELECT id FROM profile_type WHERE name='"
						+ profile + "' LIMIT 1));");
			}

		} catch (SQLException e) {
			System.out
					.println("Unable to find profile for user in set profile");

			e.printStackTrace();

		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(getUserProfile)");
			}
		}
	}

	/**
	 * Gets profile id from id
	 * 
	 * @param profile
	 *            in string form,
	 * @return
	 */
	public int getProfileID(String profile) {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		int id = 0;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			profile = profile.toLowerCase();

			rs = stmt.executeQuery("SELECT id FROM profile_type WHERE name='"
					+ profile + "' LIMIT 1;");
			if (rs.next())
				id = rs.getInt("id");

		} catch (SQLException e) {
			System.out.println("Error in SQL query GET PROFILE ID");
			// System.out.println(e.getErrorCode());
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				System.out.println("Error closing(get Profile ID)");
			}
		}

		return id;
	}
}
