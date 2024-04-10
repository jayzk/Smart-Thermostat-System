package backend.CentralServer.Replica2;

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
public class Replica2Application {

    public static void main(String[] args) {
        int applicationPort = 9501;

        SpringApplication.run(Replica2Application.class, "--server.port=" + applicationPort);
    }

    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Value("${replica2.listenerPort}")
    private int replicaPort;

    @Value("${replica2.electionPort}")
    private int electionPort;

    @Value("${replica2.syncPort}")
    private int syncPort;

    @Bean
    public ServerApplication setUpReplica1App(){
        return new ServerApplication(numberOfRooms, replicaPort, electionPort, syncPort);
    }
}
