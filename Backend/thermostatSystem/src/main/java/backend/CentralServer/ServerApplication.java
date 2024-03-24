package backend.CentralServer;

import backend.Kafka.KafkaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ServerApplication {
    private final int numberOfRooms;

    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;
    private static ReplicatedMemory replicatedMemory;
    private boolean running = false;

    private KafkaService kafkaService;

    private final int proxyPort;
    private final int syncPort; //acts as an ID for the bully algorithm
    private int currLeader = 0;
    private int[] knownReplicas = new int[]{10500, 10501, 10502, 10503};

    private final Logger log;

    public ServerApplication(int numberOfRooms, int proxyPort, int syncPort){
        log = Logger.getLogger(ServerApplication.class.getName() + "-port:" + proxyPort);
        this.numberOfRooms = numberOfRooms;
        this.proxyPort = proxyPort;
        this.syncPort = syncPort;
        log.info("This one port: " + this.proxyPort);
        initCentralServer();
        receiveMessage();
        initiateElection();
        checkAlive();
    }

    public void checkAlive(){
        new Thread(() -> {
            while (true) {
                if (currLeader != syncPort) {
                    try (Socket socket = new Socket()) {
                        // Timeout for connection attempt (in milliseconds)
                        int timeout = 2000;

                        log.info("Checking if leader, " + currLeader + " is alive");
                        socket.connect(new InetSocketAddress("localhost", currLeader), timeout);
                        log.info("Leader " + currLeader + " is alive");
                    } catch (IOException e) {
                        log.info("Leader " + currLeader + " is dead");
                        initiateElection();
                    }
                    try {
                        Thread.sleep(2000); // Sleep for 2 seconds before next check
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }



    private String sendMessage(int port, String message){
        try {
            log.info("Sending this message: " + message + " to port: " + port);
            Socket socket = new Socket("localhost", port);
            socket.setSoTimeout(10000);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out.write(message + "\n");
            out.flush();
            log.info("Message sent");

            String response = in.readLine();

            in.close();
            out.close();
            socket.close();

            log.info("Received this message: " + response + " from port: " + port);

            return response;
        } catch (Exception e) {
            log.info("Error: " + e.getMessage());
            return "";
        }
    }

    private void sendOneMessage(int port, String message){
        try {
            log.info("Sending this message: " + message + " to port: " + port);
            Socket socket = new Socket("localhost", port);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write(message + "\n");
            out.flush();
            log.info("Message sent");

            out.close();
            socket.close();
        } catch (Exception e) {
            log.info("The replica on port: " + port + " is dead: " + e.getMessage());
        }
    }

    public void initiateElection(){
        running = true;
        log.info("Server " + syncPort + " is initiating an election");
        int maxPort = Arrays.stream(knownReplicas).max().getAsInt();
        if(maxPort == syncPort){
            currLeader = syncPort;
            for(int serverPort: knownReplicas){
                if(serverPort != syncPort){
                    String message = "{ \"type\": \"Leader\", \"portVal\":" + syncPort + "}";
                    sendOneMessage(serverPort, message);
                    running = false;
                }
            }
        }
        else{
            String response = "";
            for(int serverPort: knownReplicas){
                if(serverPort > syncPort){
                    String message = "{ \"type\": \"Election\", \"portVal\":" + syncPort + "}";
                    response = sendMessage(serverPort, message);
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(response.isEmpty()){
                currLeader = syncPort;
                log.info("-------CURRENT LEADER:--------" + currLeader);
                for(int serverPort: knownReplicas){
                    if(serverPort != syncPort){
                        String message = "{ \"type\": \"Leader\", \"portVal\":" + syncPort + "}";
                        sendOneMessage(serverPort, message);
                        running = false;
                    }
                }
            }
            else{
                try {
                    Thread.sleep(10000);
                    if(currLeader == 0){
                        initiateElection();
                    }
                    else{
                        running = false;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }


    private void receiveMessage() {
        Thread receiverThread = new Thread(() -> {
            try{
                ServerSocket serverSocket = new ServerSocket(syncPort);
                System.out.println("Server started. Listening for messages...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();

                    new Thread(() -> {
                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                            String message;
                            while ((message = in.readLine()) != null) {
                                log.info("Received this message: " + message);
                                JSONObject messageJson = new JSONObject(message);
                                switch (messageJson.getString("type")) {
                                    case "Leader" -> {
                                        currLeader = messageJson.getInt("portVal");
                                        log.info("-------CURRENT LEADER:--------" + currLeader);
                                        running = false;
                                    }
                                    case "Election" -> {
                                        if (messageJson.getInt("portVal") < syncPort) {
                                            // Write code here to send a "Bully" message back to the client with the same port
                                            JSONObject responseJson = new JSONObject();
                                            String responseString = "Bully\n";
                                            out.write(responseString);
                                            out.flush();
                                            log.info("Sent a 'Bully' message to client with port: " + clientSocket.getPort());
                                            if (!running) {
                                                initiateElection();
                                            }
                                        }
                                    }
                                }
                            }

                            in.close();
                            out.close();
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();

                }
            } catch (Exception e){
                log.warning("Error caught here: " + e.getMessage());
            }
        });

        receiverThread.start();
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
                    updateData(roomNum, Integer.parseInt(record.value()));

                }
            }
        }
    }

    public void updateData(int roomID, int temp) {

        String centralServerAddress = "127.0.0.1";
        System.out.println("Update roomID: " + roomID + " temp: " + temp);

        for(int port = 12005; port < 12006; port++){
            try {
                // Create a socket connection to the central server
                Socket socket = new Socket(centralServerAddress, port);

                // Create output stream to send request
                OutputStream outputStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(outputStream, true);

                // Send request to the JAVA DB
                System.out.println("Send update request to port: " + port);
                String updateMassage= "{ \"type\": 1, \"room\":" + roomID + ", \"temperature\":" + temp + "}";
                out.println(updateMassage);


                // Create input stream to receive response
                InputStream inputStream = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Update data port " + port + " is not avalible.");
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
            log.info("Shutting down...");
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
            log.info("Contents of shared memory:");
            replicatedMemory.printHashMap();
            log.info("Shutdown complete.");
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


                    if (type == 0){
                        int room = roomTempJson.getInt("room");
                        //Check current temperature
                        int[] currentTemperature = replicatedMemory.readInstructions(room);
                        log.info("Received a current temperature request for room: " + room + " value: " + currentTemperature[0]);
                        writer.write(Integer.toString(currentTemperature[0]) + "\n");
                        writer.flush();
                    }
                    else if (type == 1){
                        int room = roomTempJson.getInt("room");
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
