package org.smokinmils.testbots;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class BotRestartTool {
    /** The Server name of the database. */
    public static final String SERVER  = "199.101.50.187";

    /** The port number of the database (MySQL is typically 3306). */
    public static final int    PORT    = 3306;

    /** The database name of the database. */
    public static final String DB_NAME = "test";

    /** The username of the database. */
    public static final String DB_USER = "smbot";

    /** The password of the database. */
    public static final String DB_PASS = "SM_bot_2013$";
    
    /** The database URL. */
    private final static String url         = "jdbc:mysql://"
                                            + SERVER + ":"
                                            + PORT + "/"
                                            + DB_NAME
                                            + "?autoReconnect=true&"
                                            + "user=" + DB_USER + "&"
                                            + "password=" + DB_PASS;
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        while (true) {
            try {
                if (checkRestart()) {
                    System.out.println("Restart table was true. Restarting the test bot...");
                    restartBot();
                }
                Thread.sleep(30000);
            } catch (Exception e) {
                System.out.println("Something went wrong while restarting!");
                e.printStackTrace();
            }
        }
    }

    private static boolean checkRestart() throws Exception {
            Connection connect = null;
            Statement statement = null;
            ResultSet resultSet = null;
            boolean ret = false;
    
       try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            
            // Setup the connection with the DB
            connect = DriverManager.getConnection(url);
            statement = connect.createStatement();
            
            // Result set get the result of the SQL query
            resultSet = statement.executeQuery("SELECT restart FROM restart LIMIT 1");
            if (resultSet.next()) {
                ret = resultSet.getBoolean(1);
            }
        } catch (Exception e) {
          throw e;
        } finally {
            if (resultSet != null) resultSet.close();            
            if (statement != null) statement.close();
            if (connect != null) connect.close();
        }
        return ret;
    }
    
    private static void restartBot() throws Exception {
        // Read PID
        File file = new File("TESTSM_BOT.pid");
        if (file.exists()) {
            Scanner scanner = new Scanner(file);
            
            if(scanner.hasNextInt()) {
                int pid = scanner.nextInt();
                
                // Kill
                System.out.println("Killing PID: " + Integer.toString(pid));
                Runtime.getRuntime().exec("taskkill /F /PID " + Integer.toString(pid));
    
            }
            scanner.close();
        }
        
        Thread.sleep(2000);
        
        // Start
        System.out.println("Starting SMGamerTest.exe");
        Runtime.getRuntime().exec("cmd /c start SMGamerTest.exe");
        
        Connection connect = null;
        Statement statement = null;
        ResultSet resultSet = null;
        
        try {
            // Setup the connection with the DB
            connect = DriverManager.getConnection(url);
            statement = connect.createStatement();
        
            // Update the table to stop restarts
            statement.executeUpdate("UPDATE restart SET restart = '0'");
        } catch (Exception e) {
            throw e;
        } finally {
            if (resultSet != null) resultSet.close();            
            if (statement != null) statement.close();
            if (connect != null) connect.close();
        }
    }
}
