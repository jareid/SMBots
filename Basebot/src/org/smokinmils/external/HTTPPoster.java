package org.smokinmils.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.ContentType;
import org.apache.http.HttpResponse;
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
    private final int COIN_2_EOC = 100000;
    
    /** Conversion between coins and 07. */
    private final int COIN_2_07 = 10000;
    
    /** The TOKEN for submitting data. */
    private final String TOKEN = XMLLoader.getInstance().getHTTPSetting("token");
    //"9c8fcb2a-0bc2-4288-8880-0e2f3f42598d";
    
    /** URL To post to. */
    private final String URL = XMLLoader.getInstance().getHTTPSetting("url");
    //"http://22.smokin-goldshop.appspot.com/stock"; 
    //"http://smokin-goldshop.appspot.com/stock";
    
    /** JSON to send a gold transaction. */
    private final String SUBMIT_GOLD = "{\"token\": \"%token\", \"agent\": \"%agent\", "
                                    + "\"amount\": %amount, \"comment\": \"\", "
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
        int sendamount = 0;
        if (prof.equals(ProfileType.EOC)) {
            sendamount = (int) (amount * COIN_2_EOC);
        } else {
            sendamount = (int) (amount * COIN_2_07);
        }
        out = out.replaceAll("%amount", String.valueOf(sendamount));
        
        p.setEntity(new StringEntity(out, ContentType.create(JSONMIME)));
        
        //HttpResponse r = 
                c.execute(p);
        /*
        BufferedReader rd = new BufferedReader(new InputStreamReader(r.getEntity().getContent()));
        String line = "";
        while ((line = rd.readLine()) != null) {
            System.out.println(line);
        
        }*/
        EventLog.log("Posted to internets", "HTTPPost", "sendGoldChange");
        
    }
    /* for testing 
    public static void main(String[] args) {
        HTTPPoster h = new HTTPPoster();
        try {
            h.sendGoldChange("Harold", 10, ProfileType.EOC);
            System.out.println("DUN");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}