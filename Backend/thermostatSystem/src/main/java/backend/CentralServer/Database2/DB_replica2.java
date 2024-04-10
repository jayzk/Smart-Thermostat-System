package backend.CentralServer.Database2;

import backend.CentralServer.MemoryApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Spring boot application to run the replica
 */
@SpringBootApplication
@EnableWebMvc
public class DB_replica2 {
    public static void main(String[] args) {
        int applicationPort = 12501;

        SpringApplication.run(DB_replica2.class, "--server.port=" + applicationPort);
    }

    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Value("${db-replica2.port}")
    private int replicaPort;


    @Bean
    public MemoryApplication setUpReplica1App() {
        return new MemoryApplication(replicaPort, numberOfRooms);
    }
}





