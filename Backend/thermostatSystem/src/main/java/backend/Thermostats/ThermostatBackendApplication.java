package backend.Thermostats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class ThermostatBackendApplication {

	public static void main(String[] args) {
		int port = 12000;
		SpringApplication.run(ThermostatBackendApplication.class, "--server.port=" + port);
	}

}
