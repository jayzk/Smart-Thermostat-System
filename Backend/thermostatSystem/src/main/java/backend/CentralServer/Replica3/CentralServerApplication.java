package backend.CentralServer.Replica3;

import backend.CentralServer.CentralServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class CentralServerApplication {

    public static void main(String[] args) {
        int applicationPort = 9502;
        int port = 10002;
        int numberOfRooms = 20;
        CentralServer replica = new CentralServer(numberOfRooms, port);
        replica.initCentralServer();
        SpringApplication.run(CentralServerApplication.class, "--server.port=" + applicationPort);
    }
}
