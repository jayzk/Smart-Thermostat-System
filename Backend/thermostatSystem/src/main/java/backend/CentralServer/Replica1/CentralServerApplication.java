package backend.CentralServer.Replica1;

import backend.CentralServer.Replica1.CentralServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;



@SpringBootApplication
@EnableWebMvc
public class CentralServerApplication {

    public static void main(String[] args) {
        int applicationPort = 9500;

        SpringApplication.run(backend.CentralServer.Replica1.CentralServerApplication.class, "--server.port=" + applicationPort);
    }
}
