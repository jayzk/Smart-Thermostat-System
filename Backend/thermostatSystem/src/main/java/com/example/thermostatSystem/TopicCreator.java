package com.example.thermostatSystem;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Component
public class TopicCreator {

    @Bean
    public CommandLineRunner createTopics() {
        return args -> {
            // Kafka bootstrap server
            String bootstrapServers = "localhost:9092";

            // List of topic names to create
            List<String> topicNames = new ArrayList<>();
            topicNames.add("room1");
            topicNames.add("room2");
            topicNames.add("room3");
            topicNames.add("room4");
            topicNames.add("room5");
            topicNames.add("room6");
            topicNames.add("room7");

            // Number of partitions for the topics
            int numPartitions = 2;

            // Replication factor for the topics
            short replicationFactor = 1;

            // Create AdminClient properties
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

            try (AdminClient adminClient = AdminClient.create(props)) {
                for (String topicName : topicNames) {
                    // Check if the topic already exists
                    if (adminClient.listTopics().names().get().contains(topicName)) {
                        System.out.println("Topic " + topicName + " already exists. Skipping.");
                        continue;
                    }

                    // Create a new Kafka topic
                    NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);

                    // Add topic creation request to a list
                    adminClient.createTopics(Collections.singletonList(newTopic));

                    System.out.println("Topic " + topicName + " created successfully.");
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        };
    }
}
