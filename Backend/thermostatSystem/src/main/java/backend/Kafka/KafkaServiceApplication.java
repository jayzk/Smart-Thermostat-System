package backend.Kafka;

import backend.Thermostats.ThermostatBackendApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@SpringBootApplication
@EnableWebMvc
public class KafkaServiceApplication {

    public static void main(String[] args) {
        int port = 11000;
        SpringApplication.run(KafkaServiceApplication.class, "--server.port=" + port);
    }

}
