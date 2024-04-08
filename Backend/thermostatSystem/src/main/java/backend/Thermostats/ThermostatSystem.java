package backend.Thermostats;

import backend.Kafka.KafkaService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.logging.Logger;

/*
 * Mock Thermostat System
 * This class listens for temperature change commands and adjusting the temperature of a specific room
 */

public class ThermostatSystem {
    int currentTemp;
    final int MAX_TEMP = 32;
    final int MIN_TEMP = 16;
    public String roomId;
    public KafkaService kafka;
    final int LISTENER_PARTITION = 0;
    final int SENDER_PARTITION = 1;

    Thread changeTempThread = new Thread();

    private static final Logger log = Logger.getLogger(ThermostatSystem.class.getName());

    /*
     * Constructor
     * Initializes a new thermostat system for the specified room and sets a random current temperature
     * Initializes Kafka consumer for listening to temperature change requests and starts a thread to listen for these requests
     */
    public ThermostatSystem(String roomId) {
        this.roomId = roomId;
        currentTemp = (int) ((Math.random() * (MAX_TEMP - MIN_TEMP)) + MIN_TEMP);
        kafka = new KafkaService(this.roomId);
        kafka.initThermostatConsumer(LISTENER_PARTITION);
        sendTempChangeMessage();
        Thread listenThread = new Thread(this::listenForChangeTemp);
        listenThread.start();
    }

    /*
     * listenForChangeTemp()
     * Listens for temperature change commands from Kafka. Upon receiving a command, it updates currentTemp 
     */
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
                    if(this.changeTempThread.isAlive()){
                        this.changeTempThread.interrupt();
                    }
                    this.changeTempThread = new Thread(() -> changeTemp(newTemp));
                    this.changeTempThread.start();
                }
            }
        }
    }

    /*
     * sendTempChangeMessage()
     * Publishes the current temperature to a Kafka topic
     */
    public void sendTempChangeMessage(){
        kafka.produce(SENDER_PARTITION, currentTemp);
    }


    /*
     * changeTemp(int new_temperature)
     * Adjusts currentTemp to match new_temperature then sends temperature updates through Kafka after each adjustment
     */
    public void changeTemp(int new_temperature) {
        log.info("Temperature change begin for " + roomId);
        log.info("Current temp is: "  + currentTemp);
        if (currentTemp < new_temperature) {
            while (currentTemp < new_temperature) {
                currentTemp += 1;
                sendTempChangeMessage();
                log.info("Increased - for room: " + roomId);
                log.info("Now it is: " + currentTemp);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
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
                    break;
                }
            }
        }

        log.info("Temperature change complete");
    }
}