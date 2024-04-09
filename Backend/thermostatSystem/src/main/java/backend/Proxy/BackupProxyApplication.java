package backend.Proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableWebMvc

/**
 * Backup proxy application with port 8081
 * Run this file and the backup proxy will be working
 * If frontend did not get respond from the first proxy, resend to this one
 */
public class BackupProxyApplication {

    public static void main(String[] args) {
        int port = 8081;
        SpringApplication.run(backend.Proxy.ProxyApplication.class, "--server.port=" + port);
    }

}