package backend.CentralServer;


import backend.Kafka.KafkaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class ServerApplication {
    private final int numberOfRooms;

    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;
    private static ReplicatedMemory replicatedMemory;

    private KafkaService kafkaService;

    private final int proxyPort;
    private final int syncPort;
    private int currLeader;
    private int[] knownReplicas = new int[]{10500, 10501, 10502, 10503};

    private final Logger log;

    public ServerApplication(int numberOfRooms, int proxyPort, int syncPort){
        log = Logger.getLogger(ServerApplication.class.getName() + "-port:" + proxyPort);
        this.numberOfRooms = numberOfRooms;
        this.proxyPort = proxyPort;
        this.syncPort = syncPort;
        System.out.println("This one port: " + this.proxyPort);
        initCentralServer();
    }


    public void listenForCurrentTemp(){
        while (true){
            ConsumerRecords<String, String> records = kafkaService.consume();
            if(!records.isEmpty()){
                for (ConsumerRecord<String, String> record : records){
                    log.info("Received this from thermostat: " +  record.topic() +" "+ record.value());
                    String topic = record.topic();
                    String numberStr = topic.substring("room".length());
                    int roomNum = Integer.parseInt(numberStr);
                    replicatedMemory.writeInstructions(roomNum, Integer.parseInt(record.value()), 0);
                }
            }
        }
    }

    public void initCentralServer() {
        kafkaService = new KafkaService(numberOfRooms);
        kafkaService.initCentralServerConsumer();
        executor = Executors.newFixedThreadPool(1);
        serverSockets = new ArrayList<>();
        replicatedMemory = new ReplicatedMemory();
        replicatedMemory.initializeHashMap(numberOfRooms);
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
            replicatedMemory.printHashMap();
            System.out.println("Shutdown complete.");
        }));

        //creating server sockets
        try {
            new Thread(() -> {

                try (ServerSocket serverSocket = new ServerSocket(this.proxyPort)) {
                    log.info("Server listening on port " + this.proxyPort);
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        ClientHandler clientHandler = new ClientHandler(log, clientSocket, replicatedMemory);
                        clientHandler.setKafkaService(kafkaService);
                        clientHandler.start();
                    }
                } catch (Exception e) {
                    log.warning("Exception occurred on port " + this.proxyPort + ": " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            log.warning("Some Exception has occurred " + e);
        }
    }




class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final ReplicatedMemory replicatedMemory;
    private KafkaService kafkaService;
    final Logger log;

    public ClientHandler(Logger log, Socket socket, ReplicatedMemory replicatedMemory) {
        this.clientSocket = socket;
        this.log = log;
        this.replicatedMemory = replicatedMemory;
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
                    int[] currentTemperature = replicatedMemory.readInstructions(room);
                    log.info("Received a current temperature request for room: " + room + " value: " + currentTemperature[0]);
                    writer.write(Integer.toString(currentTemperature[0]) + "\n");
                    writer.flush();
                }
                else if (type == 1){
                    int temperature = roomTempJson.getInt("temperature");
                    //Change temperature
                    // Extract room and temperature values
                    int[] currentTemperature = replicatedMemory.readInstructions(room);
                    replicatedMemory.writeInstructions(room, currentTemperature[0], temperature);
                    kafkaService.setRoomTopic("room" + room);
                    kafkaService.produce(0, temperature);
                    log.info("Received a change temperature request for room: " + room + " value: " + temperature);
                }
                else if (type == 2){
                    log.info("Received an Alive message");
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
