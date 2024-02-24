package com.example.thermostatSystem;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
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
    private static SharedMemory sharedMemory;

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
        sharedMemory = new SharedMemory();

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
            System.out.println("Contents of shared memory:");
            System.out.println(sharedMemory.readInstructions());
            System.out.println("Shutdown complete.");
        }));

        //creating server sockets
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
                            ClientHandler clientHandler = new ClientHandler(clientSocket, sharedMemory);
                            clientHandler.start();
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
}

class SharedMemory {
    private List<String> instructions;
    private final Lock mutex;

    public SharedMemory() {
        this.instructions = new ArrayList<>();
        this.mutex = new ReentrantLock();
    }

    public void writeInstructions(String instruction) {
        mutex.lock();
        try {
            this.instructions.add(instruction);
        } finally {
            mutex.unlock();
        }
    }

    public List<String> readInstructions() {
        mutex.lock();
        try {
            return new ArrayList<>(this.instructions);
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
            String instruction;

            //TODO: add queues here
            while ((instruction = reader.readLine()) != null) {
                sharedMemory.writeInstructions(instruction);
                System.out.println("Instruction received from client and written to shared memory: " + instruction);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
