package com.drum_delivery_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DrumDeliveryBackendApplication {

	public static void main(String[] args) {
		// Set default profile to 'dev' if no profile is explicitly provided
		if (System.getProperty("spring.profiles.active") == null && 
            System.getenv("SPRING_PROFILES_ACTIVE") == null) {
            System.setProperty("spring.profiles.active", "dev");
        }
		
		SpringApplication.run(DrumDeliveryBackendApplication.class, args);
	}

}
