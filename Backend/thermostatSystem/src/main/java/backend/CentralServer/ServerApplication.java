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

public class ServerApplication {
    private final int numberOfRooms;

    private static ExecutorService executor;
    private static List<ServerSocket> serverSockets;
    private boolean running = false;

    private boolean iHaveLock = false; //TODO: change this to false

    private KafkaService kafkaService;

    private final int proxyPort;
    private final int electionPort; //acts as an ID for the bully algorithm
    private final int syncPort;
    private int currLeader = 0; //linked with election ports
    private int currLeaderSync = 0; //linked with sync ports (for sync ports to know who is currently leader)
    private final int[] allElectionPorts = new int[]{10500, 10501, 10502, 10503};
    private final int[] db_ports = new int[]{12000, 12001, 12002, 12003};
    private final ClientHandler clientHandler;
    private Queue<Integer> criticalSectionQ = new LinkedList<>();
    private boolean isCSBusy = false;

    private final Logger log;

    public ServerApplication(int numberOfRooms, int proxyPort, int electionPort, int syncPort) {
        log = Logger.getLogger(ServerApplication.class.getName() + "-port:" + proxyPort);
        this.numberOfRooms = numberOfRooms;
        this.proxyPort = proxyPort;
        this.electionPort = electionPort;
        this.syncPort = syncPort;
        this.clientHandler = new ClientHandler(log);
        this.clientHandler.setKafkaService(kafkaService);
        log.info("This one port: " + this.proxyPort);
        initCentralServer();
        receiveMessage();
        checkAlive();
        initiateElection();

        receiveSyncMessage();
    }

    public void checkAlive() {
        new Thread(() -> {
            while (true) {
                synchronized (this) {
                    //log.info("leader: " + currLeader + " elecPort: " + electionPort); //TODO: delete later maybe
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
                    currLeaderSync = syncPort; //TODO: check
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

    //TODO: can prob just use sendOneMessage()
    private void sendEnterCS(int thisSyncPort) {
        try {
            //socket setup
            Socket socket = new Socket("localhost", thisSyncPort);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //send request to leader
            String message = "{ \"type\": \"Request\", \"portVal\":" + thisSyncPort + "}";
            log.info("Sending this SYNC message: " + message + " to port: " + thisSyncPort);
            out.write(message + "\n");
            out.flush();
            log.info("Message sent");

            //TODO: problem here
            // Indefinitely wait for acquire response from leader
            String response = null;
            boolean responseReceived = false;
            while (!responseReceived) { //TODO: may have to include an additonal condition here
                try {
                    // Read the response (this will block until data is available or an EOF is reached)
                    response = in.readLine();
                    responseReceived = true; // Response received, exit loop
                } catch (IOException e) {
                    // Handle IOException, e.g., connection reset by peer
                    //TODO: change later
                    log.info("Error reading sync response: " + e.getMessage());
                }
            }

            //close socket connections
            in.close();
            out.close();
            socket.close();

            log.info("Received this message: " + response + " from port: " + currLeaderSync);

            //allow replica to enter critical section
            iHaveLock = true;
        } catch (Exception e) {
            log.info("Error: " + e.getMessage());
        }
    }

    private void sendExitCS(int thisSyncPort) {
        try {
            String message = "{ \"type\": \"Release\", \"portVal\":" + thisSyncPort + "}";
            log.info("Sending RELEASE message: " + message + " to port: " + thisSyncPort);
            Socket socket = new Socket("localhost", thisSyncPort);
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

    private void sendAcquire(int port, String message) {
        try {
            log.info("Sending ACQUIRE message: " + message + " to port: " + port);
            Socket socket = new Socket("localhost", port);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            out.write(message + "\n");
            out.flush();
            log.info("ACQUIRE Message sent");

            out.close();
            socket.close();
        } catch (Exception e) {
            log.info("sendAcquire(): The replica on port: " + port + " is dead: " + e.getMessage());
        }
    }

    //only to be used if replica is the leader
    private void receiveSyncMessage() {
        Thread receiverThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(syncPort);
                log.info("Sync Receiver started on port " + syncPort + ". Listening for sync messages...");

                while (true) {
                    //if current replica is a leader
                    if(currLeader == electionPort) {
                        log.info("TEST 1");
                        Socket clientSocket = serverSocket.accept();
                        try {
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                            log.info("TEST 2");
                            String message;
                            while ((message = in.readLine()) != null) {
                                log.info("Received this sync message: " + message);
                                JSONObject messageJson = new JSONObject(message);
                                switch (messageJson.getString("type")) {
                                    case "Request" -> {
                                        int portVal = messageJson.getInt("portVal");
                                        if(isCSBusy) { //critical section is busy
                                            log.info("Critical Section is busy, adding sync port " + portVal + " to queue");
                                            criticalSectionQ.add(portVal);
                                        }
                                        else { //critical section is not busy
                                            log.info("Send acquire to sync port " + portVal);
                                            isCSBusy = true;
                                            sendAcquire(portVal, "ACQUIRE ACK");

                                        }
                                    }
                                    case "Release" -> {
                                        if(criticalSectionQ.isEmpty()) { //no replicas are waiting for CS
                                            log.info("Release: Critical Section queue is empty");
                                            isCSBusy = false; //CS is available
                                        }
                                        else { //at least one replica needs the CS
                                            //take replica port out of the head of the queue
                                            int portVal = criticalSectionQ.remove();

                                            log.info("Send acquire to sync port " + portVal);
                                            //inform the replica that it can enter CS
                                            sendAcquire(portVal, "ACQUIRE ACK");
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

        receiverThread.start();
    }

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
                }
            } catch (Exception e) {
                log.warning("Error caught here: " + e.getMessage());
            }
        });

        receiverThread.start();
    }

    //TODO: use for CS entering
        public void listenForCurrentTemp() {
            while (true) {
                ConsumerRecords<String, String> records = kafkaService.consume();
                if (!records.isEmpty()) {
                    for (ConsumerRecord<String, String> record : records) {
                        log.info("Received this from thermostat: " + record.topic() + " " + record.value());
                        String topic = record.topic();
                        String numberStr = topic.substring("room".length());
                        int roomNum = Integer.parseInt(numberStr);
//                        sendEnterCS(syncPort, "SYNC TEST"); //TODO: check this later
                        clientHandler.updateData(roomNum, Integer.parseInt(record.value()));
                    }
                }
            }
        }


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
            //
            // int roomNum = 0;
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

        public void setKafkaService(KafkaService service) {
            this.kafkaService = service;
        }

        public void updateData(int roomID, int temp) {
            while (true){ //TODO: do we need this while loop???
                log.info("SYNC TEST");
                //request permission to enter CS from leader
                sendEnterCS(syncPort);
                if(iHaveLock){ //enter CS
                    String centralServerAddress = "127.0.0.1";
                    log.info("Update roomID: " + roomID + " temp: " + temp);
//                    sendEnterCS(currLeaderSync, "SYNC TEST");

                    for (int port : db_ports) {
                        try {
                            // Create a socket connection to the central server
                            Socket socket = new Socket(centralServerAddress, port);

                            // Create output stream to send request
                            OutputStream outputStream = socket.getOutputStream();
                            PrintWriter out = new PrintWriter(outputStream, true);

                            // Send request to the JAVA DB
                            log.info("Send update request to port: " + port);
                            String updateMassage = "{ \"type\": 1, \"room\":" + roomID + ", \"temperature\":" + temp + "}";
                            out.println(updateMassage);

                            // Create input stream to receive response
                            InputStream inputStream = socket.getInputStream();
                            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                            Thread.sleep(10);
                            out.close();
                            in.close();
                            socket.close();
                        } catch (IOException e) {
                            log.info("Update data port " + port + " is not available.");
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    //exit out of CS
                    sendExitCS(syncPort);
                    break;
                }

            }

        }

        public int getTemp(int roomID) {
            int intValue = 0;

            String centralServerAddress = "127.0.0.1";
            log.info("Get temperature from roomID: " + roomID);

            for (int port : db_ports) {
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
                    // return intValue;

                } catch (IOException e) {
                    log.info("Get temp request data port " + port + " is not avalible.");
                }
            }
            // intValue = Integer.parseInt(respond);
            return intValue;
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


                    if (type == 0) {
                        //Check current temperature
                        writer.write(Integer.toString(getTemp(room)) + "\n");
                        writer.flush();
                    } else if (type == 1) {
                        int temperature = roomTempJson.getInt("temperature");
                        //Change temperature
                        // Extract room and temperature values
                        updateData(room, temperature);
                        kafkaService.setRoomTopic("room" + room);
                        kafkaService.produce(0, temperature);
                        log.info("Received a change temperature request for room: " + room + " value: " + temperature);
                    } else if (type == 2) {
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