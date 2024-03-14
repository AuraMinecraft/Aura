package net.aniby.aura;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Logger;

@SpringBootApplication
public class AuraBackend {
    // Velocity
    @Getter
    private static final Logger logger = Logger.getLogger("Aura");

    public static void main(String[] args) {
        SpringApplication.run(AuraBackend.class, args);
    }
}