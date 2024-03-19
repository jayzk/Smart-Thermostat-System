package backend.CentralServer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfig {
    @Bean
    public Server server1() {
        return new Server(9500, 1, false);
    }

}
