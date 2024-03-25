package backend.CentralServer.Database1;

import backend.CentralServer.MemoryApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class DB_replica1 {
    public static void main(String[] args) {
        int applicationPort = 12500;

        SpringApplication.run(DB_replica1.class, "--server.port=" + applicationPort);
    }

    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Value("${db-replica1.port}")
    private int replicaPort;


    @Bean
    public MemoryApplication setUpReplica1App() {
        return new MemoryApplication(replicaPort, numberOfRooms);
    }
}





