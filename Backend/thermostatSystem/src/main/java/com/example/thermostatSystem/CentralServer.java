package com.example.thermostatSystem;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.json.JSONObject;


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

@Component
public class CentralServer {
    private static final int NUM_SERVERS = 4; // Number of server instances
    private static final String SERVER_HOST = "localhost";

    @Value("${central-server.ports}")
    private int[] SERVER_PORT;

    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;
    private static SharedMemory sharedMemory;

    private KafkaService kafkaService;


    @Bean
    public void initCentralServer() {
        executor = Executors.newFixedThreadPool(NUM_SERVERS);
        serverSockets = new ArrayList<>();
        sharedMemory = new SharedMemory();
        kafkaService = new KafkaService();


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
                            clientHandler.setKafkaService(kafkaService);
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
    private KafkaService kafkaService;

    public ClientHandler(Socket socket, SharedMemory sharedMemory) {
        this.clientSocket = socket;
        this.sharedMemory = sharedMemory;
    }

    public void setKafkaService(KafkaService service){
        this.kafkaService = service;
    }

    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String instruction;

            while ((instruction = reader.readLine()) != null) {
                sharedMemory.writeInstructions(instruction);
                JSONObject roomTempJson = new JSONObject(instruction);
                System.out.println(roomTempJson.get("room"));

                // Extract room and temperature values
                int room = roomTempJson.getInt("room");
                int temperature = roomTempJson.getInt("temperature");

                kafkaService.setRoomTopic("room" + room);
                kafkaService.produce(0, temperature);
                System.out.println("Instruction received from client and written to shared memory: " + instruction);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
