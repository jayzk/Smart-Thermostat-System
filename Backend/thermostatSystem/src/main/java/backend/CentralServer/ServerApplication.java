//package backend.CentralServer;
//
//
//import backend.Kafka.KafkaService;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.apache.kafka.clients.consumer.ConsumerRecords;
//import org.json.JSONObject;
//
//import java.io.*;
//import java.net.*;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeoutException;
//import java.util.logging.Logger;
//
//
//public class ServerApplication {
//    private final int numberOfRooms;
//
//    private static ExecutorService executor;
//    private static List<ServerSocket> serverSockets;
//    private static ReplicatedMemory replicatedMemory;
//    private boolean leaderAlive;
//    private boolean running = false;
//
//    private KafkaService kafkaService;
//
//    private final int proxyPort;
//    private final int syncPort; //acts as an ID for the bully algorithm
//    private int currLeader = 0;
//    private int[] knownReplicas = new int[]{10500, 10501, 10502, 10503};
//
//    private final Logger log;
//
//    public ServerApplication(int numberOfRooms, int proxyPort, int syncPort){
//        log = Logger.getLogger(ServerApplication.class.getName() + "-port:" + proxyPort);
//        this.numberOfRooms = numberOfRooms;
//        this.proxyPort = proxyPort;
//        this.syncPort = syncPort;
//        log.info("This one port: " + this.proxyPort);
//        initCentralServer();
//        initiateElection();
//        receiveMessage();
//    }
//
//
//
//    private String sendMessage(int port, String message){
//        try {
//            log.info("Sending this message: " + message + " to port: " + port);
//            Socket socket = new Socket("localhost", port);
//            socket.setSoTimeout(10000);
//            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
//            writer.println(message);
//            log.info("Message sent");
//
//            // Read response from the server
//            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//            String response = reader.readLine();
//
//            log.info("Received this: " + response);
//
//
//            return "Bully";
//        } catch (Exception e) {
//            log.info("Error: " + e.getMessage());
//            return "";
//        }
//    }
//
//    private void sendOneMessage(int port, String message){
//        try {
//            log.info("Sending this message: " + message + " to port: " + port);
//            Socket socket = new Socket("localhost", port);
//            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
//            writer.println(message);
//            log.info("Message sent");
//
//        } catch (Exception e) {
//            log.info("Error: " + e.getMessage());
//        }
//    }
//
//    public void initiateElection(){
//        running = true;
//        log.info("Server " + syncPort + " is initiating an election");
//        int maxPort = Arrays.stream(knownReplicas).max().getAsInt();
//        if(maxPort == syncPort){
//            currLeader = syncPort;
//            for(int serverPort: knownReplicas){
//                if(serverPort != syncPort){
//                    String message = "{ \"type\": \"Leader\", \"portVal\":" + syncPort + "}";
//                    sendOneMessage(serverPort, message);
//                    running = false;
//                }
//            }
//        }
//        else{
//            String response = "";
//            for(int serverPort: knownReplicas){
//                if(serverPort > syncPort){
//                    String message = "{ \"type\": \"Election\", \"portVal\":" + syncPort + "}";
//                    response = sendMessage(serverPort, message);
//                }
//            }
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            if(response.isEmpty()){
//                currLeader = syncPort;
//                log.info("Setting the leader replica to: " + currLeader);
//                for(int serverPort: knownReplicas){
//                    if(serverPort != syncPort){
//                        String message = "{ \"type\": \"Leader\", \"portVal\":" + syncPort + "}";
//                        sendOneMessage(serverPort, message);
//                    }
//                }
//            }
//            else{
//                try {
//                    Thread.sleep(10000);
//                    if(currLeader == 0){
//                        initiateElection();
//                    }
//                    else{
//                        running = false;
//                    }
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//
//            }
//        }
//    }
//
////    private void sendMessageToPorts() {
////        Thread checkThread = new Thread(() -> {
////            while(true){
////                if (currLeader > 0 && currLeader != syncPort) {
////                    try (Socket ignored = new Socket("localhost", currLeader)) {
//////                        String message = "{ \"type\": \"Alive\", \"myPort\":" + syncPort + "}";
//////                        sendMessage(currLeader, message);
//////                        socket.setSoTimeout(1000);
////                        leaderAlive =  true;
////                    } catch (SocketTimeoutException | ConnectException e) {
////                        leaderAlive = false;
////                    } catch (IOException e) {
////                        leaderAlive = false;
////                        e.printStackTrace();
////                    }
////                } else if (currLeader == syncPort) {
////                    leaderAlive = true;
////                }
////                else{
////                    leaderAlive = false;
////                }
////                if(!leaderAlive){
////                    initiateElection();
////                }
////                try {
////                    Thread.sleep(1000);
////                } catch (InterruptedException e) {
////                    throw new RuntimeException(e);
////                }
////                log.info("Leader status: " + leaderAlive);
////            }
////        });
////        checkThread.start();
////    }
//
////    private void receiveMessage() {
////        Thread receiverThread = new Thread(() -> {
////            try (ServerSocket serverSocket = new ServerSocket(syncPort)) {
////
////                while (true) {
////                    log.info("serverSocketclose: " + serverSocket.isClosed());
////                    log.info("Server listening on port " + syncPort);
////                    Socket clientSocket = serverSocket.accept();
////                    log.info("clientSocket: " + clientSocket.isClosed());
////                    log.info("Client connected: " + clientSocket.getInetAddress() + clientSocket.getPort());
////
////                    // Read messages from the client continuously
////                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
////                        String message;
////                        while ((message = reader.readLine()) != null) {
////                            log.info("Message from client: " + message);
////                            JSONObject messageJson = new JSONObject(message);
////                            switch (messageJson.getString("type")) {
////                                case "Leader" -> {
////                                    currLeader = messageJson.getInt("portVal");
////                                    log.info("Setting the leader replica to: " + currLeader);
////                                    running = false;
////                                }
////                                case "Election" -> {
////                                    if (messageJson.getInt("portVal") < syncPort) {
////                                        // Write code here to send a "Bully" message back to the client with the same port
////                                        JSONObject responseJson = new JSONObject();
////                                        responseJson.put("type", "Bully");
////                                        responseJson.put("portVal", syncPort);
////
////                                        try (PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
////                                            writer.println(responseJson.toString());
////                                            log.info("Sent 'Bully' message to client with port: " + clientSocket.getPort());
////                                        } catch (IOException e) {
////                                            e.printStackTrace();
////                                        }
////                                    }
////                                    if (!running) {
////                                        initiateElection();
////                                    }
////                                }
////                            }
////                        }
////                    } catch (Exception e) {
////                        log.info("Caught here 1");
////                        e.printStackTrace();
////                    } finally {
////                        try {
////                            clientSocket.close();
////                        } catch (Exception e) {
////                            log.info("Caught here 2");
////
////                            e.printStackTrace();
////                        }
////                    }
////                }
////            } catch (Exception e) {
////                log.info("Caught here 3");
////
////                e.printStackTrace();
////            }
////        });
////
////        receiverThread.start();
////    }
//
//    private void receiveMessage() {
//        Thread receiverThread = new Thread(() -> {
//            try{
//                ServerSocket serverSocket = new ServerSocket(syncPort);
//                while (true) {
//                    log.info("serverSocketClosed: " + serverSocket.isClosed());
//                    log.info("Server listening on port " + syncPort);
//                    Socket clientSocket = serverSocket.accept();
//                    log.info("clientSocket: " + clientSocket.isClosed());
//                    log.info("Client connected: " + clientSocket.getInetAddress() + clientSocket.getPort());
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//                    // Read messages from the client continuously
//                    String message;
//                    while ((message = reader.readLine()) != null) {
//                        log.info("Message from client: " + message);
//                        JSONObject messageJson = new JSONObject(message);
//                        switch (messageJson.getString("type")) {
//                            case "Leader" -> {
//                                currLeader = messageJson.getInt("portVal");
//                                log.info("Setting the leader replica to: " + currLeader);
//                                running = false;
//                            }
//                            case "Election" -> {
//                                if (messageJson.getInt("portVal") < syncPort) {
//                                    // Write code here to send a "Bully" message back to the client with the same port
//                                    JSONObject responseJson = new JSONObject();
//                                    responseJson.put("type", "Bully");
//                                    responseJson.put("portVal", syncPort);
//                                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
//                                    writer.println(responseJson);
//                                    log.info("Sent 'Bully' message to client with port: " + clientSocket.getPort());
//                                    log.info("running: " + running);
//                                    if (!running) {
//                                        initiateElection();
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                }
//            } catch (Exception e){
//                log.warning("Error caught here: " + e.getMessage());
//            }
//        });
//
//        receiverThread.start();
//    }
//
//    public void listenForCurrentTemp(){
//        while (true){
//            ConsumerRecords<String, String> records = kafkaService.consume();
//            if(!records.isEmpty()){
//                for (ConsumerRecord<String, String> record : records){
//                    log.info("Received this from thermostat: " +  record.topic() +" "+ record.value());
//                    String topic = record.topic();
//                    String numberStr = topic.substring("room".length());
//                    int roomNum = Integer.parseInt(numberStr);
//                    replicatedMemory.writeInstructions(roomNum, Integer.parseInt(record.value()), 0);
//                }
//            }
//        }
//    }
//
//    public void initCentralServer() {
//        kafkaService = new KafkaService(numberOfRooms);
//        kafkaService.initCentralServerConsumer();
//        executor = Executors.newFixedThreadPool(1);
//        serverSockets = new ArrayList<>();
//        replicatedMemory = new ReplicatedMemory();
//        replicatedMemory.initializeHashMap(numberOfRooms);
//        Thread listenThread = new Thread(this::listenForCurrentTemp);
//        listenThread.start();
//
//
//        // Add shutdown hook to close server sockets
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            log.info("Shutting down...");
//            executor.shutdown(); // Shutdown the executor service
//            try {
//                // Close all server sockets
//                for (ServerSocket serverSocket : serverSockets) {
//                    serverSocket.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            //
//            // int roomNum = 0;
//            log.info("Contents of shared memory:");
//            replicatedMemory.printHashMap();
//            log.info("Shutdown complete.");
//        }));
//
//        //creating server sockets
//        try {
//            new Thread(() -> {
//
//                try (ServerSocket serverSocket = new ServerSocket(this.proxyPort)) {
//                    log.info("Server listening on port " + this.proxyPort);
//                    while (true) {
//                        Socket clientSocket = serverSocket.accept();
//                        ClientHandler clientHandler = new ClientHandler(log, clientSocket, replicatedMemory);
//                        clientHandler.setKafkaService(kafkaService);
//                        clientHandler.start();
//                    }
//                } catch (Exception e) {
//                    log.warning("Exception occurred on port " + this.proxyPort + ": " + e.getMessage());
//                }
//            }).start();
//        } catch (Exception e) {
//            log.warning("Some Exception has occurred " + e);
//        }
//    }
//
//
//
//
//class ClientHandler extends Thread {
//    private final Socket clientSocket;
//    private final ReplicatedMemory replicatedMemory;
//    private KafkaService kafkaService;
//    final Logger log;
//
//    public ClientHandler(Logger log, Socket socket, ReplicatedMemory replicatedMemory) {
//        this.clientSocket = socket;
//        this.log = log;
//        this.replicatedMemory = replicatedMemory;
//    }
//
//    public void setKafkaService(KafkaService service){
//        this.kafkaService = service;
//    }
//
//    public void run() {
//        try {
//            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
//            String instruction;
//
//            while ((instruction = reader.readLine()) != null) {
//                JSONObject roomTempJson = new JSONObject(instruction);
//
//                int type = roomTempJson.getInt("type");
//
//
//                if (type == 0){
//                    int room = roomTempJson.getInt("room");
//                    //Check current temperature
//                    int[] currentTemperature = replicatedMemory.readInstructions(room);
//                    log.info("Received a current temperature request for room: " + room + " value: " + currentTemperature[0]);
//                    writer.write(Integer.toString(currentTemperature[0]) + "\n");
//                    writer.flush();
//                }
//                else if (type == 1){
//                    int room = roomTempJson.getInt("room");
//                    int temperature = roomTempJson.getInt("temperature");
//                    //Change temperature
//                    // Extract room and temperature values
//                    int[] currentTemperature = replicatedMemory.readInstructions(room);
//                    replicatedMemory.writeInstructions(room, currentTemperature[0], temperature);
//                    kafkaService.setRoomTopic("room" + room);
//                    kafkaService.produce(0, temperature);
//                    log.info("Received a change temperature request for room: " + room + " value: " + temperature);
//                }
//                else if (type == 2){
//                    log.info("Received an Alive message");
//                    // Return if replica is alive
//                    writer.write("Alive\n");
//                    writer.flush();
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
//}

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
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;


public class ServerApplication {
    private final int numberOfRooms;

    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;
    private static ReplicatedMemory replicatedMemory;
    private boolean leaderAlive;
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
            log.info("Error: " + e.getMessage());
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
                log.info("Setting the leader replica to: " + currLeader);
                for(int serverPort: knownReplicas){
                    if(serverPort != syncPort){
                        String message = "{ \"type\": \"Leader\", \"portVal\":" + syncPort + "}";
                        sendOneMessage(serverPort, message);
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
                    log.info("serverSocketClosed: " + serverSocket.isClosed());
                    log.info("Server listening on port " + syncPort);
                    Socket clientSocket = serverSocket.accept();
                    log.info("clientSocket: " + clientSocket.isClosed());
                    log.info("Client connected: " + clientSocket.getInetAddress() + clientSocket.getPort());
                    // Read messages from the client continuously

                    new Thread(() -> {
                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                            String message;
                            while ((message = in.readLine()) != null) {
                                log.info("Message from client: " + message);
                                JSONObject messageJson = new JSONObject(message);
                                switch (messageJson.getString("type")) {
                                    case "Leader" -> {
                                        currLeader = messageJson.getInt("portVal");
                                        log.info("Setting the leader replica to: " + currLeader);
                                        running = false;
                                    }
                                    case "Election" -> {
                                        if (messageJson.getInt("portVal") < syncPort) {
                                            // Write code here to send a "Bully" message back to the client with the same port
                                            JSONObject responseJson = new JSONObject();
                                            String responseString = "Bully\n";
                                            out.write(responseString);
                                            out.flush();
                                            log.info("Sent 'Bully' message to client with port: " + clientSocket.getPort());
                                            log.info("running: " + running);
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

//                    while ((message = reader.readLine()) != null) {
//                        log.info("Message from client: " + message);
//                        JSONObject messageJson = new JSONObject(message);
//                        switch (messageJson.getString("type")) {
//                            case "Leader" -> {
//                                currLeader = messageJson.getInt("portVal");
//                                log.info("Setting the leader replica to: " + currLeader);
//                                running = false;
//                            }
//                            case "Election" -> {
//                                if (messageJson.getInt("portVal") < syncPort) {
//                                    // Write code here to send a "Bully" message back to the client with the same port
//                                    JSONObject responseJson = new JSONObject();
//                                    responseJson.put("type", "Bully");
//                                    responseJson.put("portVal", syncPort);
//                                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
//                                    writer.println(responseJson);
//                                    log.info("Sent 'Bully' message to client with port: " + clientSocket.getPort());
//                                    log.info("running: " + running);
//                                    if (!running) {
//                                        initiateElection();
//                                    }
//                                }
//                            }
//                        }
//                    }

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
