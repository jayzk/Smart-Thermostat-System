package backend.Proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc
public class BackupProxyApplication {

    public static void main(String[] args) {
        int port = 8081;
        SpringApplication.run(backend.Proxy.ProxyApplication.class, "--server.port=" + port);
    }

}