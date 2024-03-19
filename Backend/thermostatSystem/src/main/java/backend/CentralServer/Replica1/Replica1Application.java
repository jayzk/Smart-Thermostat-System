package backend.CentralServer.Replica1;

import backend.CentralServer.ServerApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;



@SpringBootApplication
@EnableWebMvc
public class Replica1Application {

    public static void main(String[] args) {
        int applicationPort = 9500;

        SpringApplication.run(Replica1Application.class, "--server.port=" + applicationPort);
    }

    @Value("${kafka.number-of-rooms}")
    private int numberOfRooms;

    @Value("${replica1.listenerPort}")
    private int replicaPort;

    @Bean
    public ServerApplication setUpReplica1App(){
        return new ServerApplication(numberOfRooms, replicaPort);
    }
}
