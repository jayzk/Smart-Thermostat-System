package backend.Kafka;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;


import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

/**
 * Kafka Service class to handle the producing and consuming of messages to the kafka brokers
 */
public class KafkaService {

    private KafkaConsumer<String, String> consumer;
    private KafkaProducer<String, String> producer;
    private String roomTopic;

    private int numberOfRooms;

    /**
     * Create a kafka service for a particular room topic
     * @param roomTopic: roomTopic to create a kafka service
     */
    public KafkaService(String roomTopic){
        consumer = setKafkaConsumer();
        producer = setKafkaProducer();
        this.roomTopic = roomTopic;
    }

    /**
     * Create a kakfa service for multiple rooms
     * @param numberOfRooms: number of rooms to create a kafka service for
     */
    public KafkaService(int numberOfRooms){
        consumer = setKafkaConsumer();
        producer = setKafkaProducer();
        this.numberOfRooms = numberOfRooms;
    }

    public void setRoomTopic(String roomTopic){
        this.roomTopic = roomTopic;
    }

    /**
     * Configure the kafka producer
     * @return configured kafka producer
     */
    public KafkaProducer<String, String> setKafkaProducer(){
        System.out.println("Kafka producer set");
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092"); // Kafka server address and port
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<>(props);
    }

    public ConsumerRecords<String, String> consume(){
        return consumer.poll(Duration.ofMillis(100));
    }

    /**
     * Initialize the central server consumer by assigning topic partitions
     */
    public void initCentralServerConsumer(){
        Collection<TopicPartition> partitions = new ArrayList<>();

        for(int roomNum = 1; roomNum <= this.numberOfRooms; roomNum++){
            partitions.add(new TopicPartition("room" + roomNum, 1));
        }
        consumer.assign(partitions);
    }

    /**
     * initialize the thermostat consumer and assign a topic partition
     * @param partition: partition to assign
     */
    public void initThermostatConsumer(int partition){
        Collection<TopicPartition> partitions = new ArrayList<>();
        partitions.add(new TopicPartition(this.roomTopic, partition));
        consumer.assign(partitions);
    }


    /**
     * Kafka produce function to produce messages
     * @param partition: partition to send to
     * @param currentTemp: temperature value to update
     */
    public void produce(int partition, int currentTemp){
        ProducerRecord<String, String> record = new ProducerRecord<>(this.roomTopic, Integer.toString(partition), Integer.toString(currentTemp));
        producer.send(record);
    }

    /**
     * Configure the Kafka Consumer
     * @return the configured kafka consumer
     */
    private KafkaConsumer<String, String> setKafkaConsumer() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092");
        props.setProperty("group.id", "same");
        props.setProperty("client.id", "consumer-same-" + UUID.randomUUID());
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("auto.offset.reset", "earliest");
        return new KafkaConsumer<>(props);
    }

}
