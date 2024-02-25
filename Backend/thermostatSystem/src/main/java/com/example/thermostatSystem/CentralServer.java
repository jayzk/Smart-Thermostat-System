package com.example.thermostatSystem;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class CentralServer {
    private static final int NUM_SERVERS = 4; // Number of server instances
    // private static final String SERVER_HOST = "localhost";

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
        // INITIALIZING 5 rooms values to 0 everything. 
        sharedMemory.initializeHashMap(5);
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
            //
            // int roomNum = 0;
            System.out.println("Contents of shared memory:");
            sharedMemory.printHashMap();
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


class SharedMemory {
    private Map<Integer, int[]> roomTemp;
    private final Lock mutex;

    public SharedMemory() {
        this.roomTemp = new HashMap<>();
        this.mutex = new ReentrantLock();
    }

    public void writeInstructions(int roomNum, int currentTemp, int changedTemp) {
        mutex.lock();
        try {
            int[] temperatures = {currentTemp, changedTemp};
            roomTemp.put(roomNum, temperatures);
        } finally {
            mutex.unlock();
        }
    }

    public int[] readInstructions(int roomNum) {
        mutex.lock();
        try {
            return roomTemp.getOrDefault(roomNum, null);
        } finally {
            mutex.unlock();
        }
    }
    public void printHashMap() {
        mutex.lock();
        try {
            System.out.println("Contents of HashMap:");
            for (Map.Entry<Integer, int[]> entry : roomTemp.entrySet()) {
                int roomNumber = entry.getKey();
                int[] temperatures = entry.getValue();
                System.out.println("Room: " + roomNumber + ", Current Temp: " + temperatures[0] + ", Changed Temp: " + temperatures[1]);
            }
        } finally {
            mutex.unlock();
        }
    }
    public void initializeHashMap(int maxRoomNumber) {
        mutex.lock();
        try {
            for (int roomNumber = 1; roomNumber <= maxRoomNumber; roomNumber++) {
                roomTemp.put(roomNumber, new int[]{0, 0});
            }
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
            System.out.println("TEST: " + clientSocket.isClosed());
            String instruction;

            while ((instruction = reader.readLine()) != null) {
                System.out.println("TEST2: " + clientSocket.isClosed());
                //sharedMemory.writeInstructions(instruction);
                JSONObject roomTempJson = new JSONObject(instruction);
                System.out.println(roomTempJson.get("room"));

                int type = roomTempJson.getInt("type");
                int room = roomTempJson.getInt("room");
                

                if (type == 0){
                    System.out.println("TEST3: " + clientSocket.isClosed());
                    //Check current temperature
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                    int[] currentTemperature = sharedMemory.readInstructions(room);

                    System.out.println("TEST5: " + clientSocket.isClosed());
                    writer.write(currentTemperature.toString());
                    writer.flush();
                    //writer.close();

                    System.out.println("TEST6: " + clientSocket.isClosed());
                }
                else{
                    System.out.println("TEST4: " + clientSocket.isClosed());
                    int temperature = roomTempJson.getInt("temperature");
                    //Change temperature
                    // Extract room and temperature values
                    int[] currentTemperature = sharedMemory.readInstructions(room); 
                    sharedMemory.writeInstructions(room, currentTemperature[0], temperature);
                    kafkaService.setRoomTopic("room" + room);
                    kafkaService.produce(0, temperature);
                }
                // System.out.println("Instruction received from client and written to shared memory: " + instruction);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
}
