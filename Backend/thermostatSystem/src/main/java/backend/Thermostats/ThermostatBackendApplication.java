package backend.Thermostats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Spring boot application to run the thermostat application
 */
@SpringBootApplication
@EnableWebMvc
public class ThermostatBackendApplication {

	public static void main(String[] args) {
		int port = 20000;
		SpringApplication.run(ThermostatBackendApplication.class, "--server.port=" + port);
	}

}
