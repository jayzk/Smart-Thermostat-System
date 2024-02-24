package Backend;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ProxyServer {

    public void startProxyServer(int port) {
        try (ServerSocket proxyServerSocket = new ServerSocket(port)) {
            System.out.println("Load balancer server started on port " + port);
            while (true) {
                Socket clientSocket = proxyServerSocket.accept();
                System.out.println("Routing request to backend server 1");

                // Read client request
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String request = "";
                while (reader.readLine() != null) {
                    request += reader.readLine();
                    System.out.println("Received request: " + request);
                }

                

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
                    out.println(request);
                    // out.println(); // Empty line to indicate end of request headers
                
                    // Close the client socket
                    clientSocket.close(); // <-- Add this line
                
                    // Create input stream to receive response
                    InputStream inputStream = socket.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                    out.close();
                    in.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ProxyServer loadBalancerServer = new ProxyServer();

        // Start the load balancer server on port 8080
        loadBalancerServer.startProxyServer(8080);
    }
}
