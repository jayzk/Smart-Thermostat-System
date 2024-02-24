package Backend;


import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



public class CentralServer {
    private static final int NUM_SERVERS = 4; // Number of server instances
    private static final String SERVER_HOST = "localhost";
    private static int[] SERVER_PORT = new int[NUM_SERVERS]; // Array to hold server ports
    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;

    public static void main(String[] args) {
        if (args.length != NUM_SERVERS) {
            System.out.println("Please provide exactly " + NUM_SERVERS + " port numbers.");
            return;
        }

        // Parse command line arguments to get server ports
        for (int i = 0; i < NUM_SERVERS; i++) {
            SERVER_PORT[i] = Integer.parseInt(args[i]);
        }

        executor = Executors.newFixedThreadPool(NUM_SERVERS);
        serverSockets = new ArrayList<>();

        // Add shutdown hook to close server sockets
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            executor.shutdown(); // Shutdown the executor service
            try {
                // Close all server sockets
                for (ServerSocket serverSocket : serverSockets) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Shutdown complete.");
        }));

        try {
            for (int i = 0; i < NUM_SERVERS; i++) {
                final int port = SERVER_PORT[i];
                executor.submit(() -> {
                    try (ServerSocket serverSocket = new ServerSocket(port)) {
                        serverSockets.add(serverSocket); // Add created ServerSocket to the list
                        System.out.println("Server listening on port " + port);
                        while (true) {
                            // Accept client connections and handle them
                            Socket clientSocket = serverSocket.accept();
                            System.out.println("Client connected to port " + port);
                            // Handle the client connection (e.g., create a new thread to handle it)
                            handleClientConnection(clientSocket);
                        }
                    } catch (Exception e) {
                        System.out.println("Exception occurred on port " + port + ": " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("Some Exception has occurred " + e);
        }
    }

    private static void handleClientConnection(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                System.out.println("Message from client: " + inputLine);// send it to the queues eventually
            }
        } catch (IOException e) {
            System.out.println("Error handling client connection: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class SharedMemory {
    private String instructions;
    private final Lock mutex;

    public SharedMemory() {
        this.instructions = "";
        this.mutex = new ReentrantLock();
    }

    public void writeInstructions(String instructions) {
        mutex.lock();
        try {
            this.instructions += instructions;
        } finally {
            mutex.unlock();
        }
    }

    public String readInstructions() {
        mutex.lock();
        try {
            return this.instructions;
        } finally {
            mutex.unlock();
        }
    }
}

class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final SharedMemory sharedMemory;

    public ClientHandler(Socket socket, SharedMemory sharedMemory) {
        this.clientSocket = socket;
        this.sharedMemory = sharedMemory;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String instructions;
            while ((instructions = reader.readLine()) != null) {
                sharedMemory.writeInstructions(instructions);
                System.out.println("Instructions received from client and written to shared memory: " + instructions);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
