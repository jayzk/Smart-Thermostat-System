package backend.CentralServer.Replica1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;



@SpringBootApplication
@EnableWebMvc
public class Replica1Application {

    public static void main(String[] args) {
        int applicationPort = 9500;

        SpringApplication.run(Replica1Application.class, "--server.port=" + applicationPort);
    }
}
