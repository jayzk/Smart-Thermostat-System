package backend.Proxy;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Spring boot application to create the kafka topics on creation
 */
@Component
public class TopicCreator {
    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Bean
    public CommandLineRunner createTopics() {
        final Logger log = Logger.getLogger(TopicCreator.class.getName());
        return args -> {
            // Kafka bootstrap server
            String bootstrapServers = "localhost:9092";

            // Number of partitions for the topics
            int numPartitions = 2;

            // Replication factor for the topics
            short replicationFactor = 1;

            // Create AdminClient properties
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

            try (AdminClient adminClient = AdminClient.create(props)) {
                for (int roomNum = 1; roomNum <= numberOfRooms; roomNum++) {
                    // Check if the topic already exists
                    String topicName = "room" + roomNum;
                    if (adminClient.listTopics().names().get().contains(topicName)) {
                        log.info("Topic " + topicName + " already exists. Skipping.");
                        continue;
                    }

                    // Create a new Kafka topic
                    NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);

                    // Add topic creation request to a list
                    adminClient.createTopics(Collections.singletonList(newTopic));

                    log.info("Topic " + topicName + " created successfully.");
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        };
    }
}
