import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.io.*;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public class Server {

    public static Connection conn;

    public static void main(String args[]) throws IOException, ClassNotFoundException {

        try {
            // db parameters
            String url = "jdbc:sqlite:C:\\Users\\Cristiano\\IdeaProjects\\wateringfromtwitter\\src\\database.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");

            /*
            //create some tables
            String sql = "CREATE TABLE IF NOT EXISTS plants (\n"
                    + "	id integer PRIMARY KEY,\n"
                    + "	name text NOT NULL,\n"
                    + "	temperatureT real\n"
                    + " humidityT real\n"
                    + " luminosityT real\n"
                    + ");";
            Statement stmt = conn.createStatement();
            stmt.execute(sql);
            sql = "CREATE TABLE IF NOT EXISTS status (\n"
                    + "	plant_id integer,\n"
                    + "	date text NOT NULL,\n"
                    + "	temperature real\n"
                    + " humidity real\n"
                    + " luminosity real\n"
                    + ");";
            stmt = conn.createStatement();
            stmt.execute(sql);

            http://www.sqlitetutorial.net/sqlite-java/insert/  <-- insert values
            */

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        //Start TCP/IP Server
        String clientSentence;
        String responseToClient;
        ServerSocket welcomeSocket = new ServerSocket(6789);

        while (true) {
            Socket connectionSocket = welcomeSocket.accept();
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            clientSentence = inFromClient.readLine();
            //System.out.println("Received: " + clientSentence);
            responseToClient = "";

            //Deal Requests and Answer



            responseToClient = responseToClient + '\n';
            outToClient.writeBytes(responseToClient);
        }





    }




}
