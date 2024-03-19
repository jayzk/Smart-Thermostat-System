package backend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
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

    public MemoryApplication(int port) {
        this.port = port;
        this.roomTemp = new HashMap<>();
    }

    public void startListening() {
        System.out.println("Listening on port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            Socket servSocket = serverSocket.accept();
            System.out.println("Listening for data on port " + this.port + "...");
            BufferedReader reader = new BufferedReader(new InputStreamReader(servSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(servSocket.getOutputStream()));
            String instruction;
            
            while ((instruction = reader.readLine()) != null) {
                JSONObject roomTempJson = new JSONObject(instruction);

                int type = roomTempJson.getInt("type");
                int room = roomTempJson.getInt("room");


                if (type == 0){
                    //Check current temperature
                    writer.write(getTemperature(room));
                    writer.flush();
                    printRoomTemperatures(roomTemp);
                }
                else if (type == 1){
                    //Change temperature
                    int temperature = roomTempJson.getInt("temperature");
                    addTemperature(room, temperature);
                    printRoomTemperatures(roomTemp);
                }
            }
                } catch (IOException e) {
                    System.out.println("Exception recieved: " + e.getMessage());
                }
        }
        

    public void addTemperature(Integer roomId, Integer temperature) {
        roomTemp.put(roomId, temperature);
    }

    public int getTemperature(Integer roomId) {
        return roomTemp.getOrDefault(roomId, 0);
    }

    public void printRoomTemperatures(Map<Integer, Integer> map) {
        System.out.println("Room Temperatures:");
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            System.out.println("Room " + entry.getKey() + ": " + entry.getValue());
        }
    }

    public static void main(String[] args) {

        int port = 12000;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("");
            }
        }

        MemoryApplication app = new MemoryApplication(port);
        app.startListening();
    }
}
