//package com.javapapers.java;
// COMPILE AND RUN
// javac -classpath twitter4j-core-4.0.4.jar Server.java
// java -classpath "twitter4j-core-4.0.4.jar:sqlite-jdbc-3.21.0.jar:." Server


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
import java.util.concurrent.TimeUnit;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;


/*
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
*/
public class Server {
  public static String consumerKeyStr = "KKWp9ibbPVPP0qZz2LYviqsDw";
  public static String consumerSecretStr = "YoIxvqjqJiayYPbvkn5RqsnDh8EGhZWl6dWpohFAFf5Etqs2Bc";
  public static String accessTokenStr = "998976358306013186-QydrWDtyl4rOSGOxfw07xjCJHexYEEr";
  public static String accessTokenSecretStr = "ICpKH8Oj41m43Oc0a2XBxbmJ2URhZzcf6Yc8xVyrLulJy";
  public static Connection conn;
  public static Calendar cal; //to insert status information;

  public static void main(String args[]) throws Exception {
    try {
    // db parameters
      Class.forName("org.sqlite.JDBC");
      //String url = "jdbc:sqlite:/home/CrisBarbosa/aulas/se/database.db";
      //String url = "jdbc:sqlite:/home/pi/database.db";
      String url = "jdbc:sqlite:/mnt/c/Users/Cristiano Barbosa/Desktop/SE 1718/se/database.db";
      //String url = "jdbc:sqlite:/net/areas/homes/up201402990/database.db";
      conn = DriverManager.getConnection(url);
      System.out.println("Connection to SQLite has been established.");

    }catch (SQLException e) {
      System.out.println("entered sqlException: " + e.getMessage());
    }
    //writeToTwitter("");
    HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
    server.createContext("/plant/info", new PlantInfo());
    server.createContext("/plant/th", new ChangePlantThreshold());
    server.createContext("/plant/act", new Actuate());
    server.createContext("/ard/sensor", new SensorData());
    server.createContext("/ard/act", new Actuated());
    server.setExecutor(null); // creates a default executor
    server.start();
    System.out.println("http server has been started.");
    System.out.println("---------------------------------------------------------");
    /*
	127.0.0.1:8000/plant/info?plant_id=x  //perguntar os ultimos valores desta planta, ordenados pelo mais recente
	Resposta: 
	--> Se o server nao conhecia a planta, cria a sua entrada na bd e retorna: {}
	--> Se a planta existe na bd mas nao tem informação sobre ela retorna: {}
	--> Se a planta existe e existe valores sobre ela retorna:
		{{plant_id=x, date=yyyyy, temperature=z, humidity=w, luminosity=l}, {plant_id=x, date=yyyyy, temperature=z, humidity=w, luminosity=l}}
    
	127.0.0.1/plant/th?plant_id=x&temperatureT=y&humidityT=z&

    */


  }

  static void writeToTwitter(String str){
	try {
		Twitter twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(consumerKeyStr, consumerSecretStr);
		AccessToken accessToken = new AccessToken(accessTokenStr, accessTokenSecretStr);
		twitter.setOAuthAccessToken(accessToken);
		twitter.updateStatus(Calendar.getInstance().getTime().toString() + "\n"+str);
		System.out.println("Successfully updated the status in Twitter.");
	} catch (TwitterException te) {
		te.printStackTrace();
	}
  }

  static void sendToArduino(String sentence) throws Exception {
	    Socket clientSocket = new Socket("192.168.10.51", 8001);
	    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
	    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	    outToServer.writeBytes(sentence + '\n');
	    String x = inFromServer.readLine();
	    clientSocket.close();
  }

  static class PlantInfo implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
        String response ="";
        String query = t.getRequestURI().getQuery();
        System.out.println(t.getRequestMethod() + " Request Received... plant/info");
        //read request body or query
        String line;
        if(query == null || query.equals("")){
          InputStream is=t.getRequestBody();
          BufferedReader reader = new BufferedReader(new InputStreamReader(is));
          line = reader.readLine();
        }
        else
          line=query;

        //split by '&'
        String par[]= new String[10];
        par=line.split("&");
        //split by '='
        String wpar[];
        wpar = par[0].split("=");
        String plant_id = wpar[1];
        System.out.println("plant_id= " + plant_id);
        //GET DATA FROM DB AND REPLY TO ANDROID
        try{
          String sql = "SELECT * FROM plants WHERE plant_id=?";
          PreparedStatement pstmt  = conn.prepareStatement(sql);
          pstmt.setInt(1,Integer.parseInt(plant_id));
          ResultSet rs = pstmt.executeQuery();
          if(!rs.isClosed()){  //if the plant exists
          	//AQUI
            sql = "select plant_id, name, temperatureT, humidityT, luminosityT, temperature, humidity, luminosity from plants natural join status where plant_id=? order by date DESC limit 1";
            pstmt  = conn.prepareStatement(sql);
            pstmt.setInt(1,Integer.parseInt(plant_id));
            ResultSet irs = pstmt.executeQuery();
            if(!irs.isClosed()){ //the plant has status information
              while(irs.next()){
              	response=response + "[{plant_id="+irs.getInt("plant_id")+", name="+irs.getString("name") + ", th_temperature="+ irs.getDouble("temperatureT")+ ", th_humidity="+ irs.getDouble("humidityT")+ ", th_luminosity="+ irs.getDouble("luminosityT") + ", pl_temperature=" +irs.getDouble("temperature") + ", pl_humidity="+irs.getDouble("humidity") + ", pl_luminosity="+ irs.getDouble("luminosity") + "}]\n";
              }
            }
            else{ // the plant has not status information
            	response="{}\n";
            }
          }
          else{ //plant doesn't exist --> insert into DB
          	try{
          		sql = "INSERT INTO plants (plant_id, name) VALUES (?, ?)";
            	pstmt  = conn.prepareStatement(sql);
            	pstmt.setInt(1,Integer.parseInt(plant_id));
            	pstmt.setString(2,"newplant!");
            	pstmt.executeUpdate();
            	response = "{}\n";
              //INFORM ARDUINO OF NEW PLANT

        	   } catch(SQLException e){
        		     System.out.println("insert error: " + e.getMessage());
      	      }
          }

        } catch(SQLException e){
            System.out.println("query exception: " + e.getMessage());
        }

        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        System.out.println("Response sent.");
        System.out.println("---------------------------------------------------------");
        os.close();
    }
  }

  static class ChangePlantThreshold implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String response="";
      String query = t.getRequestURI().getQuery();
      System.out.println(t.getRequestMethod() + " Request Received... plant/th");
      String line;
      //read request body or query
      if(query == null || query.equals("")){
        InputStream is=t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        line = reader.readLine();
      }
      else
        line=query;

      //split by '&'
      String par[]= new String[10];
      par=line.split("&");
      //System.out.println("par length: " + par.length +" "+line);
      //split by '='
      String plant_id="";
      Double thresholds[] = new Double[3];
      for(int i=0; i< par.length; i++){
        String wpar[];
        wpar = par[i].split("=");
        if(wpar[0].equals("plant_id"))
          plant_id = wpar[1];
        else if(wpar[0].equals("temperatureT"))
          thresholds[0]= Double.parseDouble(wpar[1]);
        else if(wpar[0].equals("humidityT"))
          thresholds[1]= Double.parseDouble(wpar[1]);
        else if(wpar[0].equals("luminosityT"))
          thresholds[2]= Double.parseDouble(wpar[1]);
      }
      if(par.length!=4 || plant_id.equals("") || thresholds.length!=3){
        t.sendResponseHeaders(400, 0);
        OutputStream os = t.getResponseBody();
        System.out.println("400: client side error.");
        System.out.println("---------------------------------------------------------");
        os.close();
      }
      else{
        System.out.println("plant_id=" + plant_id + " temperatureT=" +thresholds[0] + " humidityT="+thresholds[1] + " luminosityT="+thresholds[2]);
        //UPDATE DATA INTO DB
        String sql = "UPDATE plants SET temperatureT = ? , humidityT = ? , luminosityT= ? WHERE plant_id = ?";
        try{
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setDouble(1, thresholds[0]);
          pstmt.setDouble(2, thresholds[1]);
          pstmt.setDouble(3, thresholds[2]);
          pstmt.setInt(4, Integer.parseInt(plant_id));
          pstmt.executeUpdate();
        } catch(SQLException e){
          System.out.println("update thresholds values error: "+e.getMessage());
        }

        //INFORM ARDUINO
       	String s ="t " + thresholds[0] + " " + thresholds[1] + " " + thresholds[2] + " ";
       	System.out.println(s);
       	try{
       		sendToArduino(s);
       	}catch(Exception e){
       		System.out.println("couldn't send message via tcp: " + e.getMessage());
       	}

        t.sendResponseHeaders(200, 0);
        OutputStream os = t.getResponseBody();
        System.out.println("Data processed, message sent to Arduino.");
        System.out.println("---------------------------------------------------------");
        os.close();
      }
    }
  }

  static class Actuate implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String response="";
      String query = t.getRequestURI().getQuery();
      System.out.println(t.getRequestMethod() + " Request Received... plant/act");
      String line;
      //read request body or query
      if(query == null || query.equals("")){
        InputStream is=t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        line = reader.readLine();
      }
      else
        line=query;

      //split by '&'
      String par[]= new String[10];
      par=line.split("&");
      //System.out.println("par length: " + par.length +" "+line);
      //split by '='
      String plant_id="";
      String actuators[] = new String[3];
      for(int i=0; i< par.length; i++){
        String wpar[];
        wpar = par[i].split("=");
        if(wpar[0].equals("plant_id"))
          plant_id = wpar[1];
        else if(wpar[0].equals("act_temp"))
          actuators[0]= wpar[1];
        else if(wpar[0].equals("act_hum"))
          actuators[1]= wpar[1];
        else if(wpar[0].equals("act_lum"))
          actuators[2]= wpar[1];
      }
      if(par.length==1 && !plant_id.equals("")){ //asking for actuators status
      	System.out.println("plant_id=" + plant_id);
      	String sql = "SELECT plant_id, act_temp, act_hum, act_lum FROM plants WHERE plant_id = ?";
        try{
        	PreparedStatement pstmt = conn.prepareStatement(sql);
          	pstmt.setInt(1, Integer.parseInt(plant_id));
          	ResultSet irs = pstmt.executeQuery();
        	if(!irs.isClosed()){ //request results
            	while(irs.next()){
              		response=response + "[{plant_id="+irs.getInt("plant_id")+", act_temp="+irs.getString("act_temp") + ", act_hum="+ irs.getString("act_hum")+ ", act_lum="+ irs.getString("act_lum")+ "}]\n";
              	}
            }
            else{
            	response="{}\n";
            }
        } catch(SQLException e){
          System.out.println("asking actuators status error: "+e.getMessage());
        }
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        System.out.println("Response sent.");
        System.out.println("---------------------------------------------------------");
        os.close();

      }
      else if(par.length==4 && !plant_id.equals("") && actuators.length==3){ //activate actuators
        System.out.println("plant_id=" + plant_id + " act_temp=" +actuators[0] + " act_hum="+actuators[1] + " act_lum="+actuators[2]);
        //UPDATE DATA INTO DB
        String sql = "UPDATE plants SET act_temp = ? , act_hum = ? , act_lum= ? WHERE plant_id = ?";
        try{
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, actuators[0]);
          pstmt.setString(2, actuators[1]);
          pstmt.setString(3, actuators[2]);
          pstmt.setInt(4, Integer.parseInt(plant_id));
          pstmt.executeUpdate();
        } catch(SQLException e){
          System.out.println("update actuators status error: "+e.getMessage());
        }
        //INFORM ARDUINO --> ACTUATE!
        int i=0,j=0,k=0;
        if(actuators[0].equals("true"))
        	i=1;
        if(actuators[1].equals("true"))
        	j=1;
        if(actuators[2].equals("true"))
        	k=1;
        String s ="a " + i + " " + j + " " + k + " ";
       	System.out.println(s);
       	try{
       		sendToArduino(s);
       	}catch(Exception e){
       		System.out.println("couldn't send message via tcp: " + e.getMessage());
       	}

        t.sendResponseHeaders(200, 0);
        OutputStream os = t.getResponseBody();
        System.out.println("Data processed, message sent to Arduino.");
        System.out.println("---------------------------------------------------------");
        os.close();
      }
      else{
        t.sendResponseHeaders(400, 0);
        OutputStream os = t.getResponseBody();
        System.out.println("400: client side error.");
        System.out.println("---------------------------------------------------------");
        os.close();
      }
    }
  }

  static class SensorData implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String response="";
      String query = t.getRequestURI().getQuery();
      System.out.println(t.getRequestMethod() + " Request Received... ard/sensor");
      String line;
      //read request body or query
      if(query == null || query.equals("")){
        InputStream is=t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        line = reader.readLine();
      }
      else
        line=query;

      //split by '&'
      String par[]= new String[10];
      par=line.split("&");
      //split by '='
      String plant_id="";
      Double sensors[] = new Double[3];
      for(int i=0; i< par.length; i++){
        String wpar[];
        wpar = par[i].split("=");
        if(wpar[0].equals("plant_id"))
          plant_id = wpar[1];
        else if(wpar[0].equals("tempSensor"))
          sensors[0]= Double.parseDouble(wpar[1]);
        else if(wpar[0].equals("humSensor"))
          sensors[1]= Double.parseDouble(wpar[1]);
        else if(wpar[0].equals("lumSensor"))
          sensors[2]= Double.parseDouble(wpar[1]);
      }
      if(par.length!=4 || plant_id.equals("") || sensors.length!=3){
        t.sendResponseHeaders(400, 0);
        OutputStream os = t.getResponseBody();
        System.out.println("400: client side error.");
        System.out.println("---------------------------------------------------------");
        os.close();
      }
      else{
        System.out.println("plant_id=" + plant_id + " tempSensor=" +sensors[0] + " humSensor="+sensors[1] + " lumSensor="+sensors[2]);
        //UPDATE DATA INTO DB
        String sql = "INSERT INTO status VALUES(?, ?, ?, ?, ?)";
        try{
          cal=Calendar.getInstance();
          Long d = new Long(cal.getTime().getTime());
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setInt(1, Integer.parseInt(plant_id));
          pstmt.setInt(2, d.intValue());
          pstmt.setDouble(3, sensors[0]);
          pstmt.setDouble(4, sensors[1]);
          pstmt.setDouble(5, sensors[2]);
          pstmt.executeUpdate();
        } catch(SQLException e){
          System.out.println("insert new status values error: "+e.getMessage());
        }
        //writeToTwitter("Welcome to PLASMA plants monitoring!! Start making plants happy know!");
        t.sendResponseHeaders(200, 0);
        OutputStream os = t.getResponseBody();
        System.out.println("Data processed.");
        System.out.println("---------------------------------------------------------");
        os.close();
      }
    }
  }

  static class Actuated implements HttpHandler {
    @Override
    public void handle(HttpExchange t) throws IOException {
      String response="";
      String query = t.getRequestURI().getQuery();
      System.out.println(t.getRequestMethod() + " Request Received... ard/act");
      String line;
      //read request body or query
      if(query == null || query.equals("")){
        InputStream is=t.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        line = reader.readLine();
      }
      else
        line=query;

      //split by '&'
      String par[]= new String[10];
      par=line.split("&");
      //split by '='
      String plant_id="";
      String actuators[] = new String[3];
      for(int i=0; i< par.length; i++){
        String wpar[];
        wpar = par[i].split("=");
        if(wpar[0].equals("plant_id"))
          plant_id = wpar[1];
        else if(wpar[0].equals("tempAct"))
          actuators[0]= wpar[1];
        else if(wpar[0].equals("humAct"))
          actuators[1]= wpar[1];
        else if(wpar[0].equals("lumAct"))
          actuators[2]= wpar[1];
      }
      if(par.length!=4 || plant_id.equals("") || actuators.length!=3){
        t.sendResponseHeaders(400, 0);
        OutputStream os = t.getResponseBody();
        System.out.println("400: client side error.");
        System.out.println("---------------------------------------------------------");
        os.close();
      }
      else{ 
        System.out.println("plant_id=" + plant_id + " tempAct=" +actuators[0] + " humAct="+actuators[1] + " lumAct="+actuators[2]);
        //UPDATE DATA INTO DB
        String sql = "UPDATE plants SET act_temp = ? , act_hum = ? , act_lum= ? WHERE plant_id = ?";
        try{
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, actuators[0]);
          pstmt.setString(2, actuators[1]);
          pstmt.setString(3, actuators[2]);
          pstmt.setInt(4, Integer.parseInt(plant_id));
          pstmt.executeUpdate();
        } catch(SQLException e){
          System.out.println("insert new actuators values error: "+e.getMessage());
        }
        String name="";
        sql = "SELECT name FROM plants WHERE plant_id = ?";
        try{
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setInt(1, Integer.parseInt(plant_id));
          ResultSet irs = pstmt.executeQuery();
    	  if(!irs.isClosed()){ //request results
        	while(irs.next()){
          		name=irs.getString("name");
          	}
          }
        } catch(SQLException e){
          System.out.println("select name from plants error: "+e.getMessage());
        }
        name = name + ":\n\" Ufa!! So happy PLASMA is taking care of me!\n";
        if(actuators[0].equals("true")){
        	name = name + "Was so cold here!\n";
        }
        if(actuators[1].equals("true")){
        	name = name + "God i was thirsty!\n";
        }
        if(actuators[2].equals("true")){
        	name = name + "I am now enlightened!\n";
        }
        name = name + "Thank you so much :) \" ";
        //System.out.println(name);
        writeToTwitter(name);
        t.sendResponseHeaders(200, 0);
        OutputStream os = t.getResponseBody();
        System.out.println("Data processed.");
        System.out.println("---------------------------------------------------------");
        os.close();
      }
    }
  }

}
