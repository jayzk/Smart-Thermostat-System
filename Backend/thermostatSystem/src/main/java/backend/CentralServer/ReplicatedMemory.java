package backend.CentralServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReplicatedMemory {
    private Map<Integer, int[]> roomTemp;
    private final Lock mutex;

    public ReplicatedMemory() {
        this.roomTemp = new HashMap<>();
        this.mutex = new ReentrantLock();
    }

    public void writeInstructions(int roomNum, int currentTemp, int changedTemp) {
        mutex.lock();
        try {
            int[] temperatures = {currentTemp, changedTemp};
            roomTemp.put(roomNum, temperatures);
        } finally {
            mutex.unlock();
        }
    }

    public int[] readInstructions(int roomNum) {
        mutex.lock();
        try {
            return roomTemp.getOrDefault(roomNum, null);
        } finally {
            mutex.unlock();
        }
    }
    public void printHashMap() {
        mutex.lock();
        try {
            System.out.println("Contents of HashMap:");
            for (Map.Entry<Integer, int[]> entry : roomTemp.entrySet()) {
                int roomNumber = entry.getKey();
                int[] temperatures = entry.getValue();
                System.out.println("Room: " + roomNumber + ", Current Temp: " + temperatures[0] + ", Changed Temp: " + temperatures[1]);
            }
        } finally {
            mutex.unlock();
        }
    }
    public void initializeHashMap(int maxRoomNumber) {
        mutex.lock();
        try {
            for (int roomNumber = 1; roomNumber <= maxRoomNumber; roomNumber++) {
                roomTemp.put(roomNumber, new int[]{0, 100});
            }
        } finally {
            mutex.unlock();
        }
    }
}