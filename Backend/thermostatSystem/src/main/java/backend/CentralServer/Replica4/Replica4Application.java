package backend.CentralServer.Replica4;

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
public class Replica4Application {

    public static void main(String[] args) {
        int applicationPort = 9503;

        SpringApplication.run(Replica4Application.class, "--server.port=" + applicationPort);
    }

    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Value("${replica4.listenerPort}")
    private int replicaPort;

    @Value("${replica4.electionPort}")
    private int electionPort;

    @Value("${replica4.syncPort}")
    private int syncPort;

    @Bean
    public ServerApplication setUpReplica1App(){
        return new ServerApplication(numberOfRooms, replicaPort, electionPort, syncPort);
    }
}
