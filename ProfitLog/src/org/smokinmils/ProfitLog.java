package org.smokinmils;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.smokinmils.bot.Utils;
import org.smokinmils.database.types.GamesType;
import org.smokinmils.database.types.ProfileType;

public class ProfitLog {
	
	public static void main(String[] args) {
		Map<ProfileType, Double> profilesTurnover = new HashMap<ProfileType, Double>();
		Map<ProfileType, Double> profileVolume = new HashMap<ProfileType, Double>();
		
		DB db = DB.getInstance();
		
		// Get yesterday's date.
		Calendar cal = Calendar.getInstance();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		cal.add(Calendar.DATE, -1);
		String date = df.format(cal.getTime());
		
		try {
			PrintWriter out = new PrintWriter(new FileWriter(date + ".log"));
					
			for (ProfileType p : ProfileType.values()) {
				if(!p.equals(ProfileType.PLAY)) {
					for(GamesType g : GamesType.values()) {
						if (!g.equals(GamesType.ADMIN)) {
								double turnover = db.turnoverForGame(g, date, p);
								out.println(String.format("%-11s %-30s %-5s %20s",
										date, g.toString(), p.toString(), Utils.chipsToString(turnover) + " profit"));
								
								if(profilesTurnover.containsKey(p)) {
									profilesTurnover.put(p, profilesTurnover.get(p) + turnover);
								} else {
									profilesTurnover.put(p, turnover);
								}
								
								double volume = db.volumeForGame(g, date, p);
								out.println(String.format("%-11s %-30s %-5s %20s",
										date, g.toString(), p.toString(), Utils.chipsToString(volume) + " volume"));
								
								if(profileVolume.containsKey(p)) {
									profileVolume.put(p, profileVolume.get(p) + volume);
								} else {
									profileVolume.put(p, volume);
								}
								
								
						}
					}
				}
			}
			
			/* No longer used since the trade system
			for (ProfileType p : ProfileType.values()) {
				if(!p.equals(ProfileType.PLAY)) {
					out.println(String.format("%-11s %-30s %-5s %13s",
					date, "Chips sold ", p.toString(), Utils.chipsToString(db.chipsSoldForProfile(date, p))));
				}
			}
			for (ProfileType p : ProfileType.values()) {
				if(!p.equals(ProfileType.PLAY)) {
					out.println(String.format("%-11s %-30s %-5s %13s",
							date, "Chips cashed out ", p.toString(), Utils.chipsToString(db.chipsPaidoutForProfile(date, p))));
				}
			}*/
			
			for (ProfileType p : ProfileType.values()) {
				if(!p.equals(ProfileType.PLAY)) {
					out.println(String.format("%-11s %-30s %-5s %13s",
							date, "Bet volume ", p.toString(), Utils.chipsToString(profileVolume.get(p))));
				}
			}
			
			for (Map.Entry<ProfileType, Double> entry : profilesTurnover.entrySet()) {
			    String key = entry.getKey().toString();
			    double value = entry.getValue();
			    out.println(String.format("%-11s %-30s %-5s %13s",
			    date, "TOTAL PROFIT ", key, Utils.chipsToString(value)));
			}
			
			System.out.println(date + " report saved to " + date + ".log");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
} 
