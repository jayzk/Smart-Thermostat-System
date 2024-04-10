package backend.CentralServer;

import backend.Kafka.KafkaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 *  Main Central Server Application
 *  Leader Election - Bully Algorithm (Pseudo Code copied from the slides)
 *  Critical Section - Leader Based Algorithm (Pseudo Code from the slides)
 */

public class ServerApplication {
    private final int numberOfRooms;

    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;
    private boolean running = false;

    private volatile boolean iHaveLock = false;

    private KafkaService kafkaService;

    private final int proxyPort;
    private final int electionPort; //acts as an ID for the bully algorithm
    private final int syncPort;
    private int currLeader = 0; //linked with election ports
    private int currLeaderSync = 0; //linked with sync ports (for sync ports to know who is currently leader)
    private final int[] allElectionPorts = new int[]{10500, 10501, 10502, 10503};
    private final int[] db_ports = new int[]{12000, 12001, 12002, 12003};
    private final int[] db_getTemp_ports = new int[]{12100, 12101, 12102, 12103};
    private final ClientHandler clientHandler;
    private Queue<Integer> criticalSectionQ = new LinkedList<>();
    private boolean isCSBusy = false;

    private final Logger log;

    /**
     * Constructor for Server Application
     * @param numberOfRooms: Number of Hotel Rooms
     * @param proxyPort: The Port for this server
     * @param electionPort: The port used for election
     * @param syncPort: The port used for synchronization between different server replicas
     */
    public ServerApplication(int numberOfRooms, int proxyPort, int electionPort, int syncPort) {
        log = Logger.getLogger(ServerApplication.class.getName() + "-port:" + proxyPort);
        this.numberOfRooms = numberOfRooms;
        this.proxyPort = proxyPort;
        this.electionPort = electionPort;
        this.syncPort = syncPort;
        this.clientHandler = new ClientHandler(log);
        kafkaService = new KafkaService(numberOfRooms);
        this.clientHandler.setKafkaService(kafkaService);
        log.info("This one port: " + this.proxyPort);
        initCentralServer();
        receiveMessage();
        checkAlive();
        initiateElection();

        receiveAcquireMessages();

        receiveLeaderSyncMessage();
    }

    /**
     * Method to check if the leader replica is alive, periodically checking every 2 seconds with a 1 second timeout
     */
    public void checkAlive() {
        new Thread(() -> {
            while (true) {
                synchronized (this) {
                    if (currLeader != electionPort && currLeader > 0) {
                        try (Socket socket = new Socket()) {
                            // Timeout for connection attempt (in milliseconds)
                            socket.setSoTimeout(1000);
                            int timeout = 2000;

                            log.info("Checking if leader, " + currLeader + " is alive");
                            socket.connect(new InetSocketAddress("localhost", currLeader), timeout);
                            log.info("Leader " + currLeader + " is alive");
                        } catch (IOException e) {
                            log.info("Leader " + currLeader + " is dead");
                            initiateElection();
                        }
                    }
                }
                try {
                    Thread.sleep(2000); // Sleep for 2 seconds before next check
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Send a message to the other replicas (could be an "Election", "Leader" or "Bully" message)
     * @param port: port to send it to
     * @param message: message to send
     * @return: return the message received
     */
    private String sendMessage(int port, String message) {
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

    /**
     * Send a message without waiting for a response to the port
     * @param port: port number to send it to
     * @param message: message to send
     */
    private void sendOneMessage(int port, String message) {
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

    /**
     * Initiate the election in the case the replica notices a failure or when is booted
     * Follows the same pseudo code as the one presented on the course slides
     */
    public void initiateElection() {
        new Thread(() -> {
            running = true;
            log.info("Server " + electionPort + " is initiating an election");
            int maxPort = Arrays.stream(allElectionPorts).max().getAsInt();
            if (maxPort == electionPort) {
                currLeader = electionPort;
                currLeaderSync = syncPort; //TODO: check
                for (int serverPort : allElectionPorts) {
                    if (serverPort != electionPort) {
                        String message = "{ \"type\": \"Leader\", \"portVal\":" + electionPort + "}";
                        sendOneMessage(serverPort, message);
                        running = false;
                    }
                }
            } else {
                String response = "";
                for (int serverPort : allElectionPorts) {
                    if (serverPort > electionPort) {
                        String message = "{ \"type\": \"Election\", \"portVal\":" + electionPort + "}";
                        response = sendMessage(serverPort, message);
                        if(response.equals("Bully")){
                            log.info("Got bullied");
                            break;
                        }
                    }
                }
                if (response.isEmpty()) {
                    currLeader = electionPort;
                    currLeaderSync = syncPort;
                    log.info("-------CURRENT LEADER:--------" + currLeader);
                    for (int serverPort : allElectionPorts) {
                        if (serverPort != electionPort) {
                            String message = "{ \"type\": \"Leader\", \"portVal\":" + electionPort + "}";
                            sendOneMessage(serverPort, message);
                            running = false;
                        }
                    }
                } else {
                    try {
                        Thread.sleep(10000);
                        if (currLeader == 0) {
                            log.info("Re-initiate election" + response);
                            initiateElection();
                        } else {
                            log.info("No need to re-run, leader elected");
                            running = false;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }).start();
    }


    /**
     * Listen for any acquire message from the ports, used for the critical section leader based algorithm
     */
    public void receiveAcquireMessages(){
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(syncPort + 100);
                log.info("Waiting for acquire message on port " + syncPort + "...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    log.info("Acquire message received from: " + clientSocket.getInetAddress());

                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String message = in.readLine();
                    log.info("Received message: " + message);

                    iHaveLock = true;

                    in.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }


    /**
     * Send an enter to the Critical Section if the replica is trying to write to the database
     */
    private void sendEnterCS() {
        String message = "{ \"type\": \"Request\", \"portVal\":" + syncPort + "}";
        try {
            log.info("Sending this message: " + message + " to port: " + currLeaderSync);
            Socket socket = new Socket("localhost", currLeaderSync);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write(message + "\n");
            out.flush();
            log.info("Message sent");

            out.close();
            socket.close();

            while (!iHaveLock) {
                Thread.onSpinWait();
            }
        } catch (Exception e) {
            log.info("Error: " + e.getMessage());
        }
    }

    /**
     * Send a release message when it is complete writing esentially releasing the lock
     */
    private void sendExitCS() {
        try {
            String message = "{ \"type\": \"Release\", \"portVal\":" + syncPort + "}";
            log.info("Sending RELEASE message: " + message + " to port: " + currLeaderSync);
            Socket socket = new Socket("localhost", currLeaderSync);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write(message + "\n");
            out.flush();
            log.info("RELEASE Message sent");

            //replica no longer has access to CS
            iHaveLock = false;

            out.close();
            socket.close();
        } catch (Exception e) {
            log.info("sendAcquire(): The replica on port: " + currLeaderSync + " is dead: " + e.getMessage());
        }
    }

    //only to be used if replica is the leader

    /**
     * This function is used by the leader replica, which listens for messages related to the Critical Section
     * Similar structure and copies the pseudo code from the lecture, and runs different functionality based on the message type
     */
    private void receiveLeaderSyncMessage() {
        Thread receiverSyncThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(syncPort);
                log.info("Server started. Listening for messages...");

                while (true) {
                    if(currLeader == electionPort) {
                        Socket clientSocket = serverSocket.accept();

                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                            String message;
                            while ((message = in.readLine()) != null) {
                                log.info("Received this message: " + message);
                                JSONObject messageJson = new JSONObject(message);
                                switch (messageJson.getString("type")) {
                                    case "Request" -> {
                                        log.info("Request message: " + message);
                                        int portVal = messageJson.getInt("portVal");
                                        if(isCSBusy) { //critical section is busy
                                            log.info("Critical Section is busy, adding sync port " + portVal + " to queue");
                                            criticalSectionQ.add(portVal);
                                        }
                                        else { //critical section is not busy
                                            log.info("Send acquire to sync port " + portVal);
                                            isCSBusy = true;
                                            sendOneMessage(portVal+100, "Acquire\n");
                                        }
                                    }
                                    case "Release" -> {
                                        log.info("Release message: " + message);
                                        if(criticalSectionQ.isEmpty()) { //no replicas are waiting for CS
                                            log.info("Release: Critical Section queue is empty");
                                            isCSBusy = false; //CS is available
                                        }
                                        else { //at least one replica needs the CS
                                            //take replica port out of the head of the queue
                                            int portVal = criticalSectionQ.remove();
                                            log.info("Send acquire to sync port " + portVal);
                                            //inform the replica that it can enter CS
                                            sendOneMessage(portVal+100, "Acquire\n");
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
                    }
                }
            } catch (Exception e) {
                log.warning("Error caught here: " + e.getMessage());
            }
        });

        receiverSyncThread.start();
    }

    /**
     * This thread function listens for any changes to the Leader and Election (Bully algorithm)
     */
    private void receiveMessage() {
        Thread receiverThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(electionPort);
                log.info("Server started. Listening for messages...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();

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
                                    currLeaderSync = currLeader + 200; //TODO: def change this wtf
                                    log.info("-------CURRENT LEADER:--------" + currLeader);
                                    running = false;
                                }
                                case "Election" -> {
                                    if (messageJson.getInt("portVal") < electionPort) {
                                        //send a "Bully" message back to the client with the same port
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
                }
            } catch (Exception e) {
                log.warning("Error caught here: " + e.getMessage());
            }
        });

        receiverThread.start();
    }

    /**
     * Kafka listener thread function to constantly listen for current room temperatures and update the database
     */
    public void listenForCurrentTemp() {
        while (true) {
            ConsumerRecords<String, String> records = kafkaService.consume();
            if (!records.isEmpty()) {
                sendEnterCS();
                //Wait for the lock
                while (!iHaveLock) {
                    Thread.onSpinWait();
                }
                // Update the database with the new messages
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Received this from thermostat: " + record.topic() + " " + record.value());
                    String topic = record.topic();
                    String numberStr = topic.substring("room".length());
                    int roomNum = Integer.parseInt(numberStr);
                    clientHandler.updateData(roomNum, Integer.parseInt(record.value()), record.timestamp());
                }
                sendExitCS();
            }
        }
    }


    /**
     * Initialize the central server replica, starting up all the threads and initiating an election
     */
    public void initCentralServer() {
        kafkaService = new KafkaService(numberOfRooms);
        kafkaService.initCentralServerConsumer();
        executor = Executors.newFixedThreadPool(1);
        serverSockets = new ArrayList<>();

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
            log.info("Contents of shared memory:");
            log.info("Shutdown complete.");
        }));

        //creating server sockets
        try {
            new Thread(() -> {

                try (ServerSocket serverSocket = new ServerSocket(this.proxyPort)) {
                    log.info("Server listening on port " + this.proxyPort);
                    while (true) {
                        clientHandler.clientSocket = serverSocket.accept();
                        clientHandler.run();
                    }
                } catch (Exception e) {
                    log.warning("Exception occurred on port " + this.proxyPort + ": " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            log.warning("Some Exception has occurred " + e);
        }
    }


    /**
     * Handler function to handle updates to the database and also listen for requests from the proxy.
     */
    class ClientHandler{
        private Socket clientSocket;
        private KafkaService kafkaService;
        final Logger log;

        public ClientHandler(Logger log) {
            this.log = log;
        }

        public void setClientSocket(Socket socket){
            this.clientSocket = socket;
        }

        /**
         * Set up the kafka service to send temperature requests
         * @param service: kafka service to use for sending messages
         */
        public void setKafkaService(KafkaService service) {
            this.kafkaService = service;
        }

        /**
         * Function to update the database replica
         * This function updates any available database replica which then uses passive replication to update the other databases
         * @param roomID: room to update
         * @param temp: temperature value to update
         * @param recordTimeStamp: kafka record timestamp value
         */
        public void updateData(int roomID, int temp, long recordTimeStamp) {
            String centralServerAddress = "127.0.0.1";
            log.info("Update roomID: " + roomID + " temp: " + temp);

            boolean dbUpdated = false;

            //Keep on trying to update a database until one of the database is able to respond
            while(!dbUpdated){
                for (int port : db_ports) {
                    try {
                        // Create a socket connection to the central server
                        Socket socket = new Socket(centralServerAddress, port);

                        // Create output stream to send request
                        OutputStream outputStream = socket.getOutputStream();
                        PrintWriter out = new PrintWriter(outputStream, true);

                        // Send request to the JAVA DB
                        log.info("Send update request to port: " + port);
                        String updateMessage = "{ \"type\": 1, \"room\":" + roomID +
                                ", \"temperature\":" + temp +
                                ", \"timestamp\":"+ recordTimeStamp +"}";
                        log.info("Message from thermo: " + updateMessage);
                        out.println(updateMessage);

                        // Create input stream to receive response
                        InputStream inputStream = socket.getInputStream();
                        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                        Thread.sleep(10);
                        out.close();
                        in.close();
                        socket.close();
                        dbUpdated = true;
                        break;
                    } catch (IOException e) {
                        log.info("Update data port " + port + " is not available.");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                if(dbUpdated){
                    break;
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         * Function to retrieve the temperature value of a particular room
         * @param roomID: room number query
         * @return: temperature value of that room
         */
        public int getTemp(int roomID) {
            int intValue = 0;

            String centralServerAddress = "127.0.0.1";
            log.info("Get temperature from roomID: " + roomID);

            for (int port : db_getTemp_ports) {
                try {
                    // Create a socket connection to the central server
                    Socket socket = new Socket(centralServerAddress, port);
                    socket.setSoTimeout(1000);

                    // Create output stream to send request
                    OutputStream outputStream = socket.getOutputStream();
                    PrintWriter out = new PrintWriter(outputStream, true);

                    // Send request to the JAVA DB
                    log.info("Send get temp request to port: " + port);
                    String getTempMassage = "{ \"type\": 0, \"room\":" + roomID + "}";
                    out.println(getTempMassage);


                    // Create input stream to receive response
                    InputStream inputStream = socket.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                    String respond = in.readLine();
                    log.info("Get tempurature Respond: " + respond);
                    out.close();
                    in.close();
                    socket.close();
                    intValue = Integer.parseInt(respond);
                    break;
                } catch (IOException e) {
                    log.info("Get temp request data port " + port + " is not avalible.");
                }
            }
            // intValue = Integer.parseInt(respond);
            return intValue;
        }

        /**
         * Listen for incoming messages from the proxy and update/read the database
         */
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                String instruction;

                while ((instruction = reader.readLine()) != null) {
                    JSONObject roomTempJson = new JSONObject(instruction);

                    int type = roomTempJson.getInt("type");
                    


                    if (type == 0) {
                        int room = roomTempJson.getInt("room");
                        //Check current temperature
                        writer.write(Integer.toString(getTemp(room)) + "\n");
                        writer.flush();
                    } else if (type == 1) {
                        int room = roomTempJson.getInt("room");
                        int temperature = roomTempJson.getInt("temperature");
                        //Change temperature
                        // Extract room and temperature values
                        kafkaService.setRoomTopic("room" + room);
                        kafkaService.produce(0, temperature);
                        log.info("Received a change temperature request for room: " + room + " value: " + temperature);
                        writer.write("Changing success\n");
                        writer.flush();
                    } else if (type == 2) {
                        log.info("Received an Alive message from proxy");
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