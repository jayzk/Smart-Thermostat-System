package com.example.thermostatSystem;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@Configuration
public class TopicCreator {

    private static final Logger logger = Logger.getLogger(TopicCreator.class.getName());

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topics}")
    private String[] topicNames;

    @Bean
    @ConfigurationProperties("kafka")
    public KafkaConfig kafkaConfig() {
        return new KafkaConfig();
    }

    @Bean
    public CommandLineRunner createTopics(AdminClient adminClient) {
        return args -> {
            try {
                Collection<NewTopic> newTopics = new ArrayList<>();
                for (String topicName : topicNames) {
                    if (adminClient.listTopics().names().get().contains(topicName)) {
                        logger.info("Topic '" + topicName + "' already exists. Skipping.");
                        continue;
                    }

                    // Create a new Kafka topic with configured partitions, replication factor, and retention
                    newTopics.add(new NewTopic(topicName,
                            kafkaConfig().getNumPartitions(),
                            kafkaConfig().getReplicationFactor())
                            );
                }

                // Create topics asynchronously and handle potential errors
                adminClient.createTopics(newTopics);
                logger.info("Topics created successfully: " + newTopics);
            } catch (InterruptedException | ExecutionException e) {
                logger.severe("Error creating topics: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    @Bean
    public AdminClient adminClient() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(props);
    }

    // Kafka configuration class (optional)
    public static class KafkaConfig {

        private int numPartitions = 2;
        private short replicationFactor = 1;
        private long retentionMs = -1; // Use default retention unless specified

        public int getNumPartitions() {
            return numPartitions;
        }

        public void setNumPartitions(int numPartitions) {
            this.numPartitions = numPartitions;
        }

        public short getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(short replicationFactor) {
            this.replicationFactor = replicationFactor;
        }

        public long getRetentionMs() {
            return retentionMs;
        }

        public void setRetentionMs(long retentionMs) {
            this.retentionMs = retentionMs;
        }
    }
}
