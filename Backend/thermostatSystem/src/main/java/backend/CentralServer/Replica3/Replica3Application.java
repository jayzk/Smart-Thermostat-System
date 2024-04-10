package backend.CentralServer.Replica3;

import backend.CentralServer.ServerApplication;
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
public class Replica3Application {

    public static void main(String[] args) {
        int applicationPort = 9502;

        SpringApplication.run(Replica3Application.class, "--server.port=" + applicationPort);
    }

    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Value("${replica3.listenerPort}")
    private int replicaPort;

    @Value("${replica3.electionPort}")
    private int electionPort;

    @Value("${replica3.syncPort}")
    private int syncPort;

    @Bean
    public ServerApplication setUpReplica1App(){
        return new ServerApplication(numberOfRooms, replicaPort, electionPort, syncPort);
    }
}
