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
                                    this.roomTemp.put(roomNum, new long[]{temp, timestamp});
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
                        long timeStamp = roomTempJson.getInt("timestamp");


                        int temperature = roomTempJson.getInt("temperature");
                        addTemperature(room, (long) temperature, timeStamp);
                        syncDatabaseReplicas();

                    }
                    printRoomTemperatures();
                } catch (IOException e) {
                    System.out.println("Exception recieved: " + e.getMessage());
                }
            }
        }).start();
    }


    public void sendDataToReplicaManager() {
        new Thread(() -> {
            System.out.println("Listening on port " + this.requestPort + "...");
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
                    printRoomTemperatures();
                } catch (IOException e) {
                    System.out.println("Exception recieved: " + e.getMessage());
                }
            }
        }).start();
    }

    public void addTemperature(Integer roomId, Long temperature, Long timeStamp) {
        roomTemp.put(roomId, new long[]{temperature, timeStamp});
    }
    public void initializeHashMap(int maxRoomNumber) {
        for (int roomNumber = 1; roomNumber <= maxRoomNumber; roomNumber++) {
            roomTemp.put(roomNumber, new long[]{0, -1});
        }
    }

    public long getTemperature(Integer roomId) {
        return roomTemp.get(roomId)[0];
    }

    public void printRoomTemperatures() {
        System.out.println("Room Temperatures:");
        for (Map.Entry<Integer, long[]> entry : roomTemp.entrySet()) {
            System.out.println("Room " + entry.getKey() + ": " + Arrays.toString(entry.getValue()));
        }
    }

}
