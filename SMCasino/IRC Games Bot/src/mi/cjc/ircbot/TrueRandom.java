package mi.cjc.ircbot;
import java.security.SecureRandom;

public class TrueRandom {
	/** The random number generator */
	private static SecureRandom secureRand;

	/**
	 * Returns an integer upto the maximum
	 * 
	 * @param max maximum value
	 * @return
	 */
	public static Integer nextInt(Integer max) {
		secureRand = new SecureRandom();
		return secureRand.nextInt(max);
	}
}