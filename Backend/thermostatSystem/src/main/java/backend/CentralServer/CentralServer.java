package backend.CentralServer;


import com.example.thermostatSystem.KafkaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
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

    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;
    private static SharedMemory sharedMemory;

    private KafkaService kafkaService;

    public void listenForCurrentTemp(){
        while (true){
            ConsumerRecords<String, String> records = kafkaService.consume();
            if(!records.isEmpty()){
                for (ConsumerRecord<String, String> record : records){
                    String topic = record.topic();
                    String numberStr = topic.substring("room".length());
                    int roomNum = Integer.parseInt(numberStr);
                    sharedMemory.writeInstructions(roomNum, Integer.parseInt(record.value()), 0);
                }
            }
        }
    }

    @Bean
    public void initCentralServer() {
        executor = Executors.newFixedThreadPool(NUM_SERVERS);
        serverSockets = new ArrayList<>();
        sharedMemory = new SharedMemory();
        // INITIALIZING 5 rooms values to 0 everything.
        sharedMemory.initializeHashMap(numberOfRooms);
        kafkaService = new KafkaService(numberOfRooms);
        kafkaService.initCentralServerConsumer();
        Thread listenThread = new Thread(this::listenForCurrentTemp);
        listenThread.start();


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
                roomTemp.put(roomNumber, new int[]{0, 100});
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
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            String instruction;

            while ((instruction = reader.readLine()) != null) {
                JSONObject roomTempJson = new JSONObject(instruction);

                int type = roomTempJson.getInt("type");
                int room = roomTempJson.getInt("room");
                

                if (type == 0){
                    //Check current temperature
                    int[] currentTemperature = sharedMemory.readInstructions(room);

                    writer.write(Integer.toString(currentTemperature[0]) + "\n");
                    writer.flush();
                }
                else if (type == 1){
                    int temperature = roomTempJson.getInt("temperature");
                    //Change temperature
                    // Extract room and temperature values
                    int[] currentTemperature = sharedMemory.readInstructions(room); 
                    sharedMemory.writeInstructions(room, currentTemperature[0], temperature);
                    kafkaService.setRoomTopic("room" + room);
                    kafkaService.produce(0, temperature);
                }
                else if (type == 2){
                    // Return if replica is alive
                    writer.write("Alive\n");
                    writer.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
}
