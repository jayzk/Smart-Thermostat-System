package backend.CentralServer;

import backend.Thermostats.ThermostatBackendApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class CentralServerApplication {

    public static void main(String[] args) {
        int port = 8082;
        SpringApplication.run(CentralServerApplication.class, "--server.port=" + port);
    }

}
