package net.aniby.aura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuraBackend {
    // Velocity
//    @Getter
//    private static final Logger logger = Logger.getLogger("Aura");

    public static void main(String[] args) {
        SpringApplication.run(AuraBackend.class, args);
    }
}