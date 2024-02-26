package com.example.thermostatSystem;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.Random;
import java.util.logging.Logger;

public class ThermostatSystem {
    int currentTemp;
    final int MAX_TEMP = 32;
    final int MIN_TEMP = 16;
    public String roomId;
    public KafkaService kafka;
    final int LISTENER_PARTITION = 0;
    final int SENDER_PARTITION = 1;

    boolean isChangingTemperature = false;
    private static final Logger log = Logger.getLogger(ThermostatSystem.class.getName());

    public ThermostatSystem(String roomId) {
        this.roomId = roomId;
        currentTemp = (int) ((Math.random() * (MAX_TEMP - MIN_TEMP)) + MIN_TEMP);
        kafka = new KafkaService(this.roomId);
        kafka.initThermostatConsumer(LISTENER_PARTITION);
        startFluctuationThread();
        sendTempChangeMessage();
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

    public void sendTempChangeMessage(){
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

    public void changeTemp(int new_temperature) {
        isChangingTemperature = true;
        log.info("Temperature change begin for " + roomId);
        log.info("Current temp is: "  + currentTemp);
        Thread changeTempThread = new Thread(() -> {
            if (currentTemp < new_temperature) {
                while (currentTemp < new_temperature) {
                    currentTemp += 1;
                    sendTempChangeMessage();
                    log.info("Increased - for room: " + roomId);
                    log.info("Now it is: " + currentTemp);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else if (currentTemp > new_temperature) {
                while (currentTemp > new_temperature) {
                    currentTemp -= 1;
                    sendTempChangeMessage();
                    log.info("Decreased - for room: " + roomId);
                    log.info("Now it is: " + currentTemp);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            log.info("Temperature change complete");
            isChangingTemperature = false;
        });
        changeTempThread.start();
    }
}