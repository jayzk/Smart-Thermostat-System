package backend.CentralServer.Replica2;


import backend.CentralServer.SharedMemory;
import backend.Kafka.KafkaService;
import backend.Kafka.TopicCreator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


@Component
public class CentralServer {
    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;
    private static SharedMemory sharedMemory;

    private KafkaService kafkaService;

    @Value("${replica2.listenerPort}")
    private int port;


    public void listenForCurrentTemp(){
        while (true){
            ConsumerRecords<String, String> records = kafkaService.consume();
            if(!records.isEmpty()){
                for (ConsumerRecord<String, String> record : records){
                    System.out.println("Received this from thermostat: " +  record.topic() +" "+ record.value());
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
        final Logger log = Logger.getLogger(TopicCreator.class.getName());
        kafkaService = new KafkaService(numberOfRooms);
        kafkaService.initCentralServerConsumer();
        executor = Executors.newFixedThreadPool(1);
        serverSockets = new ArrayList<>();
        sharedMemory = new SharedMemory();
        sharedMemory.initializeHashMap(numberOfRooms);
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
            new Thread(() -> {

                try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                    log.info("Server listening on port " + this.port);
                    while (true) {
                        // Accept client connections and handle them
                        Socket clientSocket = serverSocket.accept();
                        log.info("Client connected to port " + this.port);
                        // Handle the client connection (e.g., create a new thread to handle it)
                        ClientHandler clientHandler = new ClientHandler(clientSocket, sharedMemory);
                        clientHandler.setKafkaService(kafkaService);
                        clientHandler.start();
                    }
                } catch (Exception e) {
                    log.warning("Exception occurred on port " + this.port + ": " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            log.warning("Some Exception has occurred " + e);
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
                    JSONObject roomTempJson = new JSONObject(instruction);

                    int type = roomTempJson.getInt("type");
                    int room = roomTempJson.getInt("room");


                    if (type == 0){
                        //Check current temperature
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                        int[] currentTemperature = sharedMemory.readInstructions(room);

                        writer.write(Integer.toString(currentTemperature[0]) + "\n");
                        writer.flush();
                    }
                    else{
                        int temperature = roomTempJson.getInt("temperature");
                        //Change temperature
                        // Extract room and temperature values
                        int[] currentTemperature = sharedMemory.readInstructions(room);
                        sharedMemory.writeInstructions(room, currentTemperature[0], temperature);
                        kafkaService.setRoomTopic("room" + room);
                        kafkaService.produce(0, temperature);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
