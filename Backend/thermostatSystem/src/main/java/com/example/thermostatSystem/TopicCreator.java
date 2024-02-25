package com.example.thermostatSystem;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
public class TopicCreator {

    @Bean
    public ApplicationRunner runner(KafkaAdmin kafkaAdmin) {
        return args -> {
            AdminClient admin = KafkaAdminClient.create(kafkaAdmin.getConfigurationProperties());
            List<NewTopic> topics = new ArrayList<>();
            topics.add(new NewTopic("room1", 2, (short) 1));
            topics.add(new NewTopic("room2", 2, (short) 1));
            try {
                admin.createTopics(topics).all().get();
            } catch (ExecutionException e) {
                if (e.getCause() instanceof TopicExistsException) {
                    // Handle topic exists exception
                    System.out.println("One or more topics already exist.");
                } else {
                    // Handle other exceptions
                    e.printStackTrace();
                }
            }
        };
    }
}
