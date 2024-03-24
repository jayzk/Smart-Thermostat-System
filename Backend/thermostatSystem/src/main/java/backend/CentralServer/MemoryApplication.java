package backend.CentralServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class MemoryApplication {
    private Map<Integer, Integer> roomTemp;
    private int port;

    public MemoryApplication(int port, int numberOfRooms) {
        this.port = port;
        this.roomTemp = new HashMap<>();
        initializeHashMap(numberOfRooms);
        startListening();
    }

    public void startListening() {
        new Thread(() -> {
            System.out.println("Listening on port " + port + "...");
            while (true){
                try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                    Socket servSocket = serverSocket.accept();
                    System.out.println("Listening for data on port " + this.port + "...");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(servSocket.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(servSocket.getOutputStream()));
                    String instruction;

                    while ((instruction = reader.readLine()) != null) {
                        // instruction = reader.readLine();
                        // if (instruction != null){
                        JSONObject roomTempJson = new JSONObject(instruction);

                        int type = roomTempJson.getInt("type");
                        int room = roomTempJson.getInt("room");

                        System.out.println("Instruction received: " + instruction);


                        if (type == 0){
                            //Check current temperature
                            writer.write(getTemperature(room) + "\n");
                            writer.flush();
                        }
                        else if (type == 1){
                            //Change temperature
                            int temperature = roomTempJson.getInt("temperature");
                            addTemperature(room, temperature);
                        }

                    }
                    printRoomTemperatures();
                } catch (IOException e) {
                    System.out.println("Exception recieved: " + e.getMessage());
                }
            }
        }).start();
    }

    public void addTemperature(Integer roomId, Integer temperature) {
        roomTemp.put(roomId, temperature);
    }
    public void initializeHashMap(int maxRoomNumber) {
        for (int roomNumber = 1; roomNumber <= maxRoomNumber; roomNumber++) {
            roomTemp.put(roomNumber, 0);
        }
    }

    public int getTemperature(Integer roomId) {
        return roomTemp.getOrDefault(roomId, 0);
    }

    public void printRoomTemperatures() {
        System.out.println("Room Temperatures:");
        for (Map.Entry<Integer, Integer> entry : roomTemp.entrySet()) {
            System.out.println("Room " + entry.getKey() + ": " + entry.getValue());
        }
    }

}
