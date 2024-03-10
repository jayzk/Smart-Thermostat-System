package backend.CentralServer.Replica2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;



@SpringBootApplication
@EnableWebMvc
public class CentralServerApplication {

    public static void main(String[] args) {
        int applicationPort = 9501;

        SpringApplication.run(CentralServerApplication.class, "--server.port=" + applicationPort);
    }
}
