package backend.CentralServer.Replica4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;



@SpringBootApplication
@EnableWebMvc
public class Replica4Application {

    public static void main(String[] args) {
        int applicationPort = 9503;

        SpringApplication.run(Replica4Application.class, "--server.port=" + applicationPort);
    }
}
