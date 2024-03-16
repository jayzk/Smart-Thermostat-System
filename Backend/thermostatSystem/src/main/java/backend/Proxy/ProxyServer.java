package backend.Proxy;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;


@RestController
public class ProxyServer {

    private String centralServerAddress;
    private ArrayList<Integer> serverPorts;
    private int loadIndex;

    public ProxyServer() {
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


        // Process the request body
        System.out.println("Received POST request with body: " + requestBody);

        String[] arrOfStr = requestBody.split(",");
        String checkMessage = "{ \"type\": 2," + arrOfStr[1] + "}";
        // System.out.println("Checking message: " + checkMessage);
        
        // Get avalible server ports
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
            

            try {
                // Create a socket connection to the central server
                Socket socket = new Socket(centralServerAddress, centralServerPort);

                // Create output stream to send request
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);

                // Send request to the server
                System.out.println("Send changing temperature request to port: " + centralServerPort);
                out.println(requestBody);


                // Create input stream to receive response
                InputStream inputStream = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        // You can return a response if needed
        return "ACK from proxy server";
    }

    public ArrayList<Integer> checkAvalible(String checkMessage){
        ArrayList<Integer> avalibleServerPorts = new ArrayList<Integer>();
        // Check avaliable ports
        for(int i = 0; i < serverPorts.size(); i++){
            try {
                String respond = "";
                Socket socket = new Socket(centralServerAddress, serverPorts.get(i));
                System.out.println("Check replica Alive: replica port " + serverPorts.get(i));

                // Create output stream to send request
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);

                // Send request to the server
                out.println(checkMessage);
                
                // Create input stream to receive response
                InputStream inputStream = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                respond = in.readLine();
                // System.out.println("Respond: " + respond);
                if(respond.equals("Alive")){
                    avalibleServerPorts.add(serverPorts.get(i));
                    System.out.println("Port " + serverPorts.get(i) + " is alive.");
                }               
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Port " + serverPorts.get(i) + " is not alive.");
            }
        }
        return avalibleServerPorts;
    }



    @GetMapping("/currentTemp")
    //change temp request
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081"})
    public ResponseEntity<String> handleCurrentTempRequest(@RequestParam String roomNum) {
        // Process the request body

        //open socket to central server
        System.out.println("Received get request with room number: " + roomNum);

        // Get avalible server ports
        String checkMessage= "{ \"type\": 2, \"room\":" + roomNum + "}";
        // System.out.println("Checking message: " + checkMessage);
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

            System.out.println("Send checking temperature request to port: " + centralServerPort);
            String data= "{ \"type\": 0, \"room\":" + roomNum + "}";
            out.println(data);


            // Create input stream to receive response
            InputStream inputStream = socket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            currentTemp = in.readLine();
            out.close();
            in.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ResponseEntity<String>(currentTemp, HttpStatus.OK);

    }

}


