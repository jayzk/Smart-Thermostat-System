package com.example.thermostatSystem;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;


public class Producer {

//    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
//
//    public static void main(String[] args) {
//        log.info("This class will produce messages to Kafka");
//
//        Properties properties = new Properties();
//
//        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
//
//        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
//
//        ProducerRecord<String, String> producerRecord = new ProducerRecord<>("my_first_topic", "", "Hello again");
//
//        producer.send(producerRecord);
//
//        producer.close();
//    }


}
