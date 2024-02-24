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

//    @PostMapping("/endpoint")
//    @CrossOrigin(origins = "http://localhost:8081")
//    public ResponseEntity<String> handleRoomPost(@RequestBody Map<String, Object> requestBody) {
//        // Print the received JSON data
//        System.out.println("Received JSON data:");
//        for (Map.Entry<String, Object> entry : requestBody.entrySet()) {
//            System.out.println(entry.getKey() + ": " + entry.getValue());
//        }
//
//        // Return a response
//        return new ResponseEntity<>("Request processed successfully", HttpStatus.OK);
//    }

    @PostMapping("/endpoint")
    @CrossOrigin(origins = "http://localhost:8081")
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
                // out.println(); // Empty line to indicate end of request headers

                // Close the client socket
                //clientSocket.close(); // <-- Add this line

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

    public void startProxyServer(int port) {
        try (ServerSocket proxyServerSocket = new ServerSocket(port)) {
            System.out.println("Load balancer server started on port " + port);
            while (true) {
                Socket clientSocket = proxyServerSocket.accept();
                System.out.println("Routing request to backend server 1");

                // Read client request
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String request = "";
               // while (reader.readLine() != null) {
                    request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                request += reader.readLine();
                    System.out.println("Received request: " + request);
                //}



                String centralServerAddress = "127.0.0.1";
                // Central server's port number
                int centralServerPort = 10000;

//                try {
//                    // Create a socket connection to the central server
//                    Socket socket = new Socket(centralServerAddress, centralServerPort);
//
//                    // Create output stream to send request
//                    OutputStream outputStream = socket.getOutputStream();
//                    PrintWriter out = new PrintWriter(outputStream, true);
//
//                    // Send request to the server
//                    out.println(request);
//                    // out.println(); // Empty line to indicate end of request headers
//
//                    // Close the client socket
//                    clientSocket.close(); // <-- Add this line
//
//                    // Create input stream to receive response
//                    InputStream inputStream = socket.getInputStream();
//                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
//                    out.close();
//                    in.close();
//                    socket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//        ProxyServer loadBalancerServer = new ProxyServer();
//
//        // Start the load balancer server on port 8080
//        loadBalancerServer.startProxyServer(8080);
//    }
}
