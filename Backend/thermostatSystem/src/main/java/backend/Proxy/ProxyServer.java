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


@RestController
public class ProxyServer {

    private String centralServerAddress;
    private ArrayList<Integer> serverPorts;
    private int loadIndex;

    private final Logger log;


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

    public void sendPostRequest(String requestBody){
        // Get avalible server ports
        String checkMessage = "{ \"type\": 2 }";
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
            // log.info("Respond: " + respond);
            if(respond.equals("Changing success")){
                log.info("Request: " + requestBody + " success.");
            }else{
                out.close();
                in.close();
                socket.close();
                sendPostRequest(requestBody);
            }  
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            sendPostRequest(requestBody);
        }
    }

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
                // log.info("Respond: " + respond);
                if(respond.equals("Alive")){
                    avalibleServerPorts.add(serverPorts.get(i));
                    log.info("Port " + serverPorts.get(i) + " is alive.");
                }               
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                log.info("Port " + serverPorts.get(i) + " is not alive.");
            }
        }
        return avalibleServerPorts;
    }



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

    public String sendCurrentTempRequest (String roomNum){
        // Get avalible server ports
        String checkMessage= "{ \"type\": 2 }";
        // log.info("Checking message: " + checkMessage);
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
            currentTemp = sendCurrentTempRequest (roomNum);
        }
        return currentTemp;
    }

}


