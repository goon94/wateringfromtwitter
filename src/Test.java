

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.sql.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.*;
import java.util.*;

public class Test {

  public static Connection conn;
  public static void main(String args[]) throws Exception {
    try {
    // db parameters
      //Class.forName("org.sqlite.JDBC");
      // /mnt/c/Users/Cristiano/Desktop/SE
      String url = "jdbc:sqlite:C:\\Users\\Cristiano\\Desktop\\SE\\database.db";
      //System.out.println(url);
      // create a connection to the database
      //Class.forName("org.sqlite.JDBC");
      conn = DriverManager.getConnection(url);
      System.out.println("Connection to SQLite has been established.");

      /*
      //create some tables
      String sql = "CREATE TABLE IF NOT EXISTS plants (\n"
      + "	id integer PRIMARY KEY,\n"
      + "	name text NOT NULL,\n"
      + "	temperatureT real,\n"
      + " humidityT real,\n"
      + " luminosityT real\n"
      + ");";
      Statement stmt = conn.createStatement();
      stmt.execute(sql);
      sql = "CREATE TABLE IF NOT EXISTS status (\n"
      + "	plant_id integer,\n"
      + "	date text NOT NULL,\n"
      + "	temperature real,\n"
      + " humidity real,\n"
      + " luminosity real\n"
      + ");";
      stmt = conn.createStatement();
      stmt.execute(sql);
      //http://www.sqlitetutorial.net/sqlite-java/insert/  <-- insert values
      */

    }catch (SQLException e) {
      System.out.println("entered sqlException: "+e.getMessage());
    }

    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    //server.createContext("/plant/new", new NewPlant());
    //server.createContext("/plant/delete", new DeletePlant());
    server.createContext("/plant/info", new PlantInfo());
    server.createContext("/plant/th", new ChangePlantThreshold());
    server.setExecutor(null); // creates a default executor
    server.start();
    //System.out.println("hey");

  }

  static class NewPlant implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
        String response = "This is the response";
        t.sendResponseHeaders(200, response.length());
        System.out.println(t.getRequestMethod());
        //read request body
        InputStream is=t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while((line = reader.readLine()) != null){
        	System.out.println(line);
        }

        //INSERT INTO plants VALUES() ...

        //INFORM ARDUINO
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
  }

  static class DeletePlant implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
        String response = "This is the response";
        t.sendResponseHeaders(200, response.length());
        System.out.println(t.getRequestMethod());
        //read request body
        InputStream is=t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while((line = reader.readLine()) != null){
        	System.out.println(line);
        }

        //DELETE ROW WHERE plant_id=id ...

        //INFORM ARDUINO
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
  }

  static class PlantInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
        String response = "This is the response";
        t.sendResponseHeaders(200, response.length());
        System.out.println(t.getRequestMethod());
        //read request body
        InputStream is=t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        line = reader.readLine();
        String par[]= new String[10];
        par=line.split("&");
        String wpar[];
        wpar = par[0].split("=");
        String plantID = wpar[1];
        //System.out.println(plantID);
        //GET DATA FROM DB AND REPLY TO ANDROID



        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
  }

  static class ChangePlantThreshold implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
        String response = "This is the response";
        t.sendResponseHeaders(200, response.length());
        System.out.println(t.getRequestMethod());
        //read request body
        InputStream is=t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while((line = reader.readLine()) != null){
        	System.out.println(line);
        }
        //UPDATE DATA INTO DB

        //INFORM ARDUINO
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
  }

}
