package backend.CentralServer.Database3;

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
public class DB_replica3 {
    public static void main(String[] args) {
        int applicationPort = 12502;

        SpringApplication.run(DB_replica3.class, "--server.port=" + applicationPort);
    }

    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Value("${db-replica3.port}")
    private int replicaPort;


    @Bean
    public MemoryApplication setUpReplica1App() {
        return new MemoryApplication(replicaPort, numberOfRooms);
    }
}
