import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.io.*;


public class Server {

    static Connection mySQLconnection;

    public static void main(String args[]) throws IOException {



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





            responseToClient = responseToClient + '\n';
            outToClient.writeBytes(responseToClient);
        }




    }


}
