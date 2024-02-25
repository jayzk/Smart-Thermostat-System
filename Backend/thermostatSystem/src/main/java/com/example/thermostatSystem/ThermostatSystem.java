package com.example.thermostatSystem;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Random;

public class ThermostatSystem {
    int currentTemp;
    final int MAX_TEMP = 32;
    final int MIN_TEMP = 16;
    public String roomId;
    public KafkaService kafka;
    final int LISTENER_PARTITION = 0;
    final int SENDER_PARTITION = 1;

    boolean isChangingTemperature = false;

    public ThermostatSystem(String roomId) {
        this.roomId = roomId;
        currentTemp = (int) ((Math.random() * (MAX_TEMP - MIN_TEMP)) + MIN_TEMP);
        kafka = new KafkaService(this.roomId);
        kafka.initConsumer(LISTENER_PARTITION);
        startFluctuationThread();
        Thread listenThread = new Thread(this::listenForChangeTemp);
        listenThread.start();
    }

    public void listenForChangeTemp(){
        while (true){
            ConsumerRecords<String, String> records = kafka.consume();
            if(!records.isEmpty()){
                ConsumerRecord<String, String> lastRecord = null;
                for (ConsumerRecord<String, String> record : records){
                    System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
                    lastRecord = record;
                }
                if(lastRecord!=null){
                    int newTemp = Integer.parseInt(lastRecord.value());
                    changeTemp(newTemp);
                }
            }
        }
    }

    public void sendTempChangeMessage(int currentTemp){
        kafka.produce(SENDER_PARTITION, currentTemp);
    }

    private void startFluctuationThread() {
        Thread fluctuationThread = new Thread(() -> {
            Random r = new Random();
            while (true) {
                if (!isChangingTemperature) {
                    int randomChange = (int) (Math.round((r.nextDouble() * 2 - 1) * 10.0) / 10.0);
                    currentTemp += randomChange;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        fluctuationThread.start();
    }



    public int getCurrentTemp() {
        return currentTemp;
    }

    public void changeTemp(int new_temperature) {
        isChangingTemperature = true;
        System.out.println("Temperature change begin for " + roomId);
        System.out.println("Current temp is: "  + currentTemp);
        Thread changeTempThread = new Thread(() -> {
            if (currentTemp < new_temperature) {
                while (currentTemp < new_temperature) {
                    currentTemp += 1;
                    sendTempChangeMessage(currentTemp);
                    System.out.println("Increased");
                    System.out.println("Now it is: " + currentTemp);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (currentTemp > new_temperature) {
                while (currentTemp > new_temperature) {
                    currentTemp -= 1;
                    sendTempChangeMessage(currentTemp);
                    System.out.println("Decreased to");
                    System.out.println("Now it is: " + currentTemp);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("Temperature change complete");
            isChangingTemperature = false;
        });
        changeTempThread.start();
    }



    // public static void main(String[] args) {
    //     ThermostatSystem thermostat1 = new ThermostatSystem(22.0f);

    //     // Check temperature
    //     for (int i = 0; i < 10; i++) {
    //         System.out.println("Current temperature: " + thermostat1.getCurrentTemp()); 
    //         try {
    //             Thread.sleep(1000);
    //         } catch (InterruptedException e) {
    //             e.printStackTrace();
    //         }
    //     }

    //     // Change temperature
    //     thermostat1.changeTemp(25.0f);
    //     while(thermostat1.getCurrentTemp() < 25.0f) {
    //         System.out.println("Current temperature: " + thermostat1.getCurrentTemp()); 
    //         try {
    //             Thread.sleep(1000);
    //         } catch (InterruptedException e) {
    //             e.printStackTrace();
    //         }
    //     }
    // }
}