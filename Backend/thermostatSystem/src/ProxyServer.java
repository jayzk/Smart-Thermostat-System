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
                String request = reader.readLine();

                System.out.println("Received request: " + request);
                // if (request.startsWith("OPTIONS")) {
                //     // Handle OPTIONS request
                //     OutputStream output = clientSocket.getOutputStream();
                //     PrintWriter writer = new PrintWriter(output, true);

                //     // Set CORS headers
                //     writer.println("HTTP/1.1 200 OK");
                //     writer.println("Access-Control-Allow-Origin: *");
                //     writer.println("Access-Control-Allow-Methods: POST, GET, OPTIONS");
                //     writer.println("Access-Control-Allow-Headers: Content-Type");
                //     writer.println("Content-Length: 0");
                //     writer.println();
                //     writer.flush();

                //     // Close the socket connection
                //     clientSocket.close();
                // }
                // Forward the client connection to the selected central server
                // Central server's IP address
                String centralServerAddress = "127.0.0.1"; 
                // Central server's port number
                int centralServerPort = 8080; 

                try {
                    // Create a socket connection to the central server
                    Socket socket = new Socket(centralServerAddress, centralServerPort);

                    // Create output stream to send request
                    OutputStream outputStream = socket.getOutputStream();
                    PrintWriter out = new PrintWriter(outputStream, true);

                    // Send request to the server
                    out.println(request);
                    out.println(); // Empty line to indicate end of request headers

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
