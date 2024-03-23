package backend.CentralServer.Database3;

import backend.CentralServer.Database1.DB_replica1;
import backend.CentralServer.Database2.DB_replica2;
import backend.CentralServer.MemoryApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;

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
