package org.smokinmils.external;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.smokinmils.bot.XMLLoader;
import org.smokinmils.database.types.ProfileType;
import org.smokinmils.logging.EventLog;

/**
 * Class to post data via HTTP.
 * @author cjc
 *
 */
public class HTTPPoster {
    
    /** JSON Mime type. */
    private static final String JSONMIME = "application/json";
    
    /** Conversion between coins and EOC. */
    private static final int COIN2_EOC = 100000;
    
    /** Conversion between coins and 07. */
    private static final int COIN_2_07 = 10000;
    
    /** Amount in a million. */
    private static final int ONEMILLION = 1000000;
    
    /** The TOKEN for submitting data. */
    private static final String TOKEN = XMLLoader.getInstance().getHTTPSetting("token");
    //"9c8fcb2a-0bc2-4288-8880-0e2f3f42598d";
    
    /** URL To post to. */
    private static final String URL = XMLLoader.getInstance().getHTTPSetting("url");
    //"http://22.smokin-goldshop.appspot.com/stock"; 
    //"http://smokin-goldshop.appspot.com/stock";
    
    /** JSON to send a gold transaction. */
    private static final String SUBMIT_GOLD = "{\"token\": \"%token\", \"agent\": \"%agent\", "
                                    + "\"amount\": \"%amountm\", \"comment\": \"\", "
                                    + "\"goldtype\": \"%goldtype\"}";
    
    /**
     * Sends the amount of gold sold / bought to the stock page.
     * @param agent the agent who initiated the change
     * @param amount the amount that has changed in coins
     * @param prof the profile that has been sold/bought
     * @throws IOException 
     */
    public final void sendGoldChange(final String agent,
                               final double amount,
                               final ProfileType prof) throws IOException {
        HttpClient c = new DefaultHttpClient();
        HttpPost p = new HttpPost(URL);
        
        String out = SUBMIT_GOLD.replaceAll("%token", TOKEN);
        out = out.replaceAll("%agent", agent);
        out = out.replaceAll("%goldtype", prof.getText());
        
        // convert to millions
        double sendamount = 0.0;
        if (prof.equals(ProfileType.EOC)) {
            sendamount = (amount * COIN2_EOC) / ONEMILLION;
        } else {
            sendamount = (amount * COIN_2_07) / ONEMILLION;
        }
        out = out.replaceAll("%amount", Double.toString(sendamount));
        
        p.setEntity(new StringEntity(out, ContentType.create(JSONMIME)));

        c.execute(p);

        EventLog.log("Posted to internets", "HTTPPost", "sendGoldChange");
        
    }
}