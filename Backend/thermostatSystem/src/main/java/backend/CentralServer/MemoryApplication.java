package backend.CentralServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONObject;

public class MemoryApplication {
    private Map<Integer, long[]> roomTemp;
    private int requestPort;
    private int updatePort;
    private int syncPort;

    private int[] knownDBPorts = new int[]{12200, 12201, 12202, 12203};

    private final Logger log;

    /**
     * Constructor for MemoryApplication class.
     * @param port Port number for communication
     * @param numberOfRooms Number of rooms to initialize
     */
    public MemoryApplication(int port, int numberOfRooms) {
        log = Logger.getLogger(MemoryApplication.class.getName() + "-port:" + port);
        this.updatePort = port;
        this.requestPort = port + 100;
        this.syncPort = port + 200;
        this.roomTemp = new HashMap<>();
        initializeHashMap(numberOfRooms);

        receiveSyncMessageFromDB();
        sendAliveMessageToAllDB();

        receiveDataFromReplicaManager();
        sendDataToReplicaManager();
    }

    /* Sends an 'Alive' message to all known database ports, except the sync port.
       This method establishes a socket connection to each port, sends a JSON-formatted
       message indicating that the sender is alive, and logs the successful transmission.
       If any errors occur during the process, they are ignored.
    */
    private void sendAliveMessageToAllDB(){
        for (int port: knownDBPorts) {
            if(port != syncPort){
                try (Socket socket = new Socket("localhost", port)) {
                    socket.setSoTimeout(1000);
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    String message = "{ \"type\": \"Alive\"" + "}";
                    out.write(message + "\n");
                    out.flush();
                    out.close();
                    log.info("Sent alive message to port: " + port);
                } catch (IOException ignored) {
                }
            }
        }
    }
    /* Synchronizes database replicas with other known database ports.
       This method iterates over known database ports, excluding the sync port.
       For each port, it establishes a socket connection and sends updates for
       room temperature data stored in the 'roomTemp' map. The data is sent in
       JSON format, including room number, temperature, and timestamp. After
       sending updates, it logs the successful transmission.
    */
    private void syncDatabaseReplicas(){
        for (int port: knownDBPorts) {
            if(port != syncPort){
                try (Socket socket = new Socket("localhost", port)) {
                    socket.setSoTimeout(1000);
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    for(Map.Entry<Integer, long[]> roomData: roomTemp.entrySet()){
                        String message = "{ \"room\":" + roomData.getKey() +
                                ", \"temperature\":" + roomData.getValue()[0] +
                                ", \"timestamp\":" + roomData.getValue()[1] + "}";
                        out.write(message + "\n");
                        out.flush();
                    }
                    out.close();
                    log.info("Sent updates to port: " + port);
                } catch (IOException ignored) {

                }
            }
        }
    }

    /* Receives synchronization messages from database ports.
       This method creates a separate thread for receiving messages.
       Inside the thread, it sets up a ServerSocket on the specified
       synchronization port. It continuously listens for incoming
       connections and processes messages accordingly. If an 'Alive'
       message is received, it triggers the synchronization process
       by calling the syncDatabaseReplicas() method. Otherwise, it
       updates the room temperature data based on the received message.
       Any exceptions occurring during the process are logged.
    */
    private void receiveSyncMessageFromDB() {
        Thread receiverThread = new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(this.syncPort);
                log.info("Server started. Listening for messages...");

                while (true) {
                    Socket clientSocket = serverSocket.accept();

                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                        String message;
                        while ((message = in.readLine()) != null) {
                            JSONObject messageJson = new JSONObject(message);

                            if(messageJson.has("type") && messageJson.getString("type").equals("Alive")){
                                syncDatabaseReplicas();
                            }
                            else{
                                int roomNum = messageJson.getInt("room");
                                long temp = messageJson.getLong("temperature");
                                long timestamp = messageJson.getLong("timestamp");

                                if(this.roomTemp.get(roomNum)[1] < timestamp){
                                    addTemperature(roomNum, temp, timestamp);
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
    /* Receives data from the replica manager.
       This method creates a new thread for receiving data.
       Inside the thread, it sets up a ServerSocket on the specified
       update port. It continuously listens for incoming connections
       and processes instructions accordingly. Upon receiving data
       in JSON format, it extracts room number, temperature, and
       timestamp, and updates the room temperature data accordingly.
       Additionally, it triggers the synchronization process by
       calling the syncDatabaseReplicas() method. Any exceptions
       occurring during the process are logged.
    */
    public void receiveDataFromReplicaManager() {
        new Thread(() -> {
            while (true){
                try (ServerSocket serverSocket = new ServerSocket(this.updatePort)) {
                    Socket servSocket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(servSocket.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(servSocket.getOutputStream()));
                    String instruction;

                    while ((instruction = reader.readLine()) != null) {
                        // instruction = reader.readLine();
                        // if (instruction != null){
                        JSONObject roomTempJson = new JSONObject(instruction);

                        int room = roomTempJson.getInt("room");
                        long timeStamp = roomTempJson.getLong("timestamp");


                        int temperature = roomTempJson.getInt("temperature");
                        addTemperature(room, (long) temperature, timeStamp);
                        syncDatabaseReplicas();

                    }

//                    printRoomTemperatures();
                } catch (IOException e) {
                    log.info("Exception recieved: " + e.getMessage());
                }
            }
        }).start();
    }

    /* Sends data to the replica manager.
       This method creates a new thread for sending data.
       Inside the thread, it sets up a ServerSocket on the specified
       request port. It continuously listens for incoming connections
       and processes instructions accordingly. Upon receiving a room
       number, it retrieves the corresponding temperature and sends
       it back to the requester. Any exceptions occurring during the
       process are logged.
    */
    public void sendDataToReplicaManager() {
        new Thread(() -> {
            log.info("Listening on port " + this.requestPort + "...");
            while (true){
                try (ServerSocket serverSocket = new ServerSocket(this.requestPort)) {
                    Socket servSocket = serverSocket.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(servSocket.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(servSocket.getOutputStream()));
                    String instruction;

                    while ((instruction = reader.readLine()) != null) {
                        // instruction = reader.readLine();
                        // if (instruction != null){
                        JSONObject roomTempJson = new JSONObject(instruction);

                        int room = roomTempJson.getInt("room");


                        //Check current temperature
                        writer.write(getTemperature(room) + "\n");
                        writer.flush();

                    }
//                    printRoomTemperatures();
                } catch (IOException e) {
                    log.info("Exception recieved: " + e.getMessage());
                }
            }
        }).start();
    }
    /*Takes in the paramenters room temperature and timestamp and stores it in the hashmap */
    public void addTemperature(Integer roomId, Long temperature, Long timeStamp) {
        roomTemp.put(roomId, new long[]{temperature, timeStamp});
        log.info("Updated room data: " + roomId + " " + temperature + " " + timeStamp);
    }
    
    /*
    * Initializes a HashMap with room numbers as keys (from 1 to maxRoomNumber) and initializes the arrays as zeros values.
    */
    public void initializeHashMap(int maxRoomNumber) {
        for (int roomNumber = 1; roomNumber <= maxRoomNumber; roomNumber++) {
            roomTemp.put(roomNumber, new long[]{0, 0});
        }
    }
    /* 
     * This method gets the temperature value from the hashmap for the specific rooms based on the input of the roomId. The  
    */
    public long getTemperature(Integer roomId) {
        return roomTemp.get(roomId)[0];
    }
    /*This function is just used to print the messages to the log as they are being updated. */
    public void printRoomTemperatures() {
        log.info("Room Temperatures:");
        for (Map.Entry<Integer, long[]> entry : roomTemp.entrySet()) {
            log.info("Room " + entry.getKey() + ": " + Arrays.toString(entry.getValue()));
        }
    }

}
