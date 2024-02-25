package com.example.thermostatSystem;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ProxyServer {


    @PostMapping("/endpoint")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081"})
    public String handlePostRequest(@RequestBody String requestBody) {
        // Process the request body
        System.out.println("Received POST request with body: " + requestBody);

        String centralServerAddress = "127.0.0.1";
        // Central server's port number
        int centralServerPort = 10000;

            try {
                // Create a socket connection to the central server
                Socket socket = new Socket(centralServerAddress, centralServerPort);

                // Create output stream to send request
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);

                // Send request to the server
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

    

    @GetMapping("/currentTemp")
    //change temp request
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8081"})
    public ResponseEntity<String> handleCurrentTempRequest(@RequestParam String roomNum) {
        // Process the request body

        //open socket to central server
        System.out.println("Received get request with room number: " + roomNum);

        String centralServerAddress = "127.0.0.1";
        // Central server's port number
        int centralServerPort = 10000;

            try {
                // Create a socket connection to the central server
                Socket socket = new Socket(centralServerAddress, centralServerPort);

                // Create output stream to send request
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);

                //For checking temperature -
                //send to central server this:
                /**
                 * {
                 *      type: 0,
                 *      room: 5
                 * }
                */
                String data= "{ \"type\": 0, \"room\":" + roomNum + "}";
                out.println(data);


                // Create input stream to receive response
                InputStream inputStream = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        //wait for central server to send the current temp (keep socket open)
        //central server sends the current temp
        //obtain the current temp
        String currentTemp = "";
        try (ServerSocket proxyServerSocket = new ServerSocket(10000)) {
            System.out.println("Load balancer server started on port " + 10000);
            while (true) {
                Socket clientSocket = proxyServerSocket.accept();
                System.out.println("Get respond request to frontend from central server.");

                // Read client request
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                

                currentTemp += reader.readLine();

                System.out.println("Received respond: " + currentTemp);
                //clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //send back to client using http request
        //return new ResponseEntity<>(
        //      "Current temp is " + currentTemp,
        //      HttpStatus.OK);
        return new ResponseEntity<String>(currentTemp, HttpStatus.OK);

    }

}
