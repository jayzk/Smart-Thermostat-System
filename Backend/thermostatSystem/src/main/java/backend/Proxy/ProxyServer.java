package backend.Proxy;

import backend.CentralServer.ServerApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Proxy sever responsible for handling incoming http requests and send them to available server replicas
 * Using round-robin algorithm to send to server replicas
 * Handle current temperature request and set temperature request
 */

@RestController
public class ProxyServer {

    private String centralServerAddress;
    private ArrayList<Integer> serverPorts;
    private int loadIndex;

    private final Logger log;

    /**
     * Constructor for the ProxyServer class
     * Initializes the central server address, server ports, and load index(for round-robin algorithm)
     */
    public ProxyServer() {
        log = Logger.getLogger(ServerApplication.class.getName() + "-port");
        this.centralServerAddress = "127.0.0.1";
        this.serverPorts = new ArrayList<Integer>();
        this.serverPorts.add(10000);
        this.serverPorts.add(10001);
        this.serverPorts.add(10002);
        this.serverPorts.add(10003);
        this.loadIndex = 0;

    }

    /**
     * Handles incoming POST requests(set temperature request)
     * 
     * @param requestBody The request body received in http request from frontend
     */
    @PostMapping("/endpoint")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081"})
    public String handlePostRequest(@RequestBody String requestBody) {

        log.info("======================================");

        // Process the request body
        log.info("Received POST request with body: " + requestBody);
        
        sendPostRequest(requestBody);

        // You can return a response if needed
        log.info("======================================");

        return "ACK from proxy server";
    }

    /**
     * Sends a POST request to the appropriate server replica
     * Handle fail to send request
     * 
     * @param requestBody the string to be sent in the POST request
     */
    public void sendPostRequest(String requestBody){

        // Get available server ports
        String checkMessage = "{ \"type\": 2 }";
        ArrayList<Integer> avalibleServerPorts = checkAvalible(checkMessage);

        // Find the server port post request (round-robin)
        int centralServerPort;
        while (true) {
            centralServerPort = serverPorts.get(loadIndex);
            loadIndex = (loadIndex + 1) % serverPorts.size();
            if(avalibleServerPorts.contains(centralServerPort)){
                break;
            }
        }
            
        String respond = "";
        try {
            // Create a socket connection to the central server
            Socket socket = new Socket(centralServerAddress, centralServerPort);

            // Create output stream to send request
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter out = new PrintWriter(outputStream, true);

            // Send request to the server
            log.info("Send changing temperature request to port: " + centralServerPort);
            out.println(requestBody);


            // Create input stream to receive response           
            InputStream inputStream = socket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            respond = in.readLine();

            // Send successful
            if(respond.equals("Changing success")){
                log.info("Request: " + requestBody + " success.");
            }
            // Fail to send, resend the request to next available server
            else{
                out.close();
                in.close();
                socket.close();
                sendPostRequest(requestBody);
            }  
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            // Fail to connect to server, resend the request to next available server
            sendPostRequest(requestBody);
        }
    }

    /**
     * Checks available server ports using socket and return a list of available server ports
     * 
     * @param checkMessage The message used to check server availability 
     * @return A list of available server ports
     */
    public ArrayList<Integer> checkAvalible(String checkMessage){
        ArrayList<Integer> avalibleServerPorts = new ArrayList<Integer>();
        // Check avaliable ports
        for(int i = 0; i < serverPorts.size(); i++){
            try {
                String respond = "";
                Socket socket = new Socket(centralServerAddress, serverPorts.get(i));
                log.info("Check replica Alive: replica port " + serverPorts.get(i));

                // Create output stream to send request
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);

                // Send request to the server
                out.println(checkMessage);
                
                // Create input stream to receive response
                InputStream inputStream = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                respond = in.readLine();
                // If set response alive, add to avaliable array
                if(respond.equals("Alive")){
                    avalibleServerPorts.add(serverPorts.get(i));
                    log.info("Port " + serverPorts.get(i) + " is alive.");
                }               
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                // Fail to connect socket, so the server replica is dead
                log.info("Port " + serverPorts.get(i) + " is not alive.");
            }
        }
        return avalibleServerPorts;
    }

    /**
     * Handles incoming GET http requests from frontend for checking current temperature
     * 
     * @param roomNum The room number for which the current temperature is requested
     * @return A ResponseEntity containing the current temperature
     */
    @GetMapping("/currentTemp")
    //change temp request
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081"})
    public ResponseEntity<String> handleCurrentTempRequest(@RequestParam String roomNum) {
        log.info("======================================");
        // Process the request body

        //open socket to central server
        log.info("Received get request with room number: " + roomNum);

        String currentTemp = sendCurrentTempRequest(roomNum);

        log.info("======================================");


        return new ResponseEntity<String>(currentTemp, HttpStatus.OK);

    }

    /**
     * Sends a request to appropriate server to retrieve current temperature for a room
     * Handle fail to send request
     * 
     * @param roomNum The room number for which the temperature is requested to check
     * @return The current temperature of the room
     */
    public String sendCurrentTempRequest (String roomNum){
        // Get available server ports
        String checkMessage= "{ \"type\": 2 }";
        ArrayList<Integer> avalibleServerPorts = checkAvalible(checkMessage);

        // Find the port post request
        int centralServerPort;
        while (true) {
            centralServerPort = serverPorts.get(loadIndex);
            loadIndex = (loadIndex + 1) % serverPorts.size();
            if(avalibleServerPorts.contains(centralServerPort)){
                break;
            }
        }

        String currentTemp = "";
        try {
            // Create a socket connection to the central server
            Socket socket = new Socket(centralServerAddress, centralServerPort);

            // Create output stream to send request
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter out = new PrintWriter(outputStream, true);

            log.info("Send checking temperature request to port: " + centralServerPort);
            String data= "{ \"type\": 0, \"room\":" + roomNum + "}";
            out.println(data);


            // Create input stream to receive response
            InputStream inputStream = socket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            currentTemp = in.readLine();
            // If not getting current temperature, resend the request to next available server
            if(currentTemp == null){
                out.close();
                in.close();
                socket.close();
                currentTemp = sendCurrentTempRequest (roomNum);
            }
            log.info("Current temp: " + currentTemp);

            out.close();
            in.close();
            socket.close();

        } catch (IOException e) {
            // Fail to connect to server, resend the request to next available server
            currentTemp = sendCurrentTempRequest (roomNum);
        }
        return currentTemp;
    }

}


