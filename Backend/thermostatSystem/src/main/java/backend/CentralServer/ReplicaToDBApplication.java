package backend.CentralServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import org.springframework.web.bind.annotation.RequestBody;

public class ReplicaToDBApplication{

    ReplicaToDBApplication(){}
    
    // Send request to database and update the data 
    public void updateData(int roomID, int temp) {

        String centralServerAddress = "127.0.0.1";
        System.out.println("Update roomID: " + roomID + " temp: " + temp);

        for(int port = 12000; port < 12004; port++){
            try {
                // Create a socket connection to the central server
                Socket socket = new Socket(centralServerAddress, port);

                // Create output stream to send request
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);

                // Send request to the JAVA DB
                System.out.println("Send update request to port: " + port);
                String updateMassage= "{ \"type\": 1, \"room\":" + roomID + ", \"temperature\":" + temp + "}";
                out.println(updateMassage);


                // Create input stream to receive response
                InputStream inputStream = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Update data port " + port + " is not avalible.");
            }
        }
    }

    // Get tempurature from data base
    public int getTemp(int roomID) {
        int intValue = 0 ;
        
        String centralServerAddress = "127.0.0.1";
        System.out.println("Get temperature from roomID: " + roomID);

        for(int port = 12000; port < 12001; port++){
            try {
                // Create a socket connection to the central server
                Socket socket = new Socket(centralServerAddress, port);

                // Create output stream to send request
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);

                // Send request to the JAVA DB
                System.out.println("Send get temp request to port: " + port);
                String getTempMassage= "{ \"type\": 0, \"room\":" + roomID + "}";
                out.println(getTempMassage);


                // Create input stream to receive response
                InputStream inputStream = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                String respond = in.readLine();
                System.out.println("Get tempurature Respond: " + respond);       
                out.close();
                in.close();
                socket.close();
                intValue = Integer.parseInt(respond);
                // return intValue;
            
            } catch (IOException e) {
                System.out.println("Get temp request data port " + port + " is not avalible.");
            }
        }
        // intValue = Integer.parseInt(respond);
        return intValue;
    }
}
