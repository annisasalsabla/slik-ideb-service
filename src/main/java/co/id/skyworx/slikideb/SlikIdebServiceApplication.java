package co.id.skyworx.slikideb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application bootstrap for SLIK IDEB Service.
 */
@SpringBootApplication
@EnableAsync
public class SlikIdebServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SlikIdebServiceApplication.class, args);
    }
}
