package com.example.thermostatSystem;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;


import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class KafkaService {

    private KafkaConsumer<String, String> consumer;
    private KafkaProducer<String, String> producer;
    private String roomTopic;

    private int numberOfRooms;

    public KafkaService(String roomTopic){
        consumer = setKafkaConsumer();
        producer = setKafkaProducer();
        this.roomTopic = roomTopic;
    }

    public KafkaService(int numberOfRooms){
        consumer = setKafkaConsumer();
        producer = setKafkaProducer();
        this.numberOfRooms = numberOfRooms;
    }

    public void setRoomTopic(String roomTopic){
        this.roomTopic = roomTopic;
    }

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

    public void initCentralServerConsumer(){
        Collection<TopicPartition> partitions = new ArrayList<>();

        for(int roomNum = 1; roomNum <= this.numberOfRooms; roomNum++){
            System.out.println("assigning room"+ roomNum);
            partitions.add(new TopicPartition("room" + roomNum, 1));
        }
        System.out.println("Assigned");
        consumer.assign(partitions);
    }

    public void initThermostatConsumer(int partition){
        Collection<TopicPartition> partitions = new ArrayList<>();
        partitions.add(new TopicPartition(this.roomTopic, partition));
        System.out.println("Assigning");
        consumer.assign(partitions);
    }


    public void produce(int partition, int currentTemp){
        ProducerRecord<String, String> record = new ProducerRecord<>(this.roomTopic, Integer.toString(partition), Integer.toString(currentTemp));
        producer.send(record);
    }

    private KafkaConsumer<String, String> setKafkaConsumer() {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092");
        props.setProperty("group.id", "test");
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("auto.offset.reset", "earliest");
        return new KafkaConsumer<>(props);
    }

}
