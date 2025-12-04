package com.focalizze.Focalizze;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableScheduling
@EnableMethodSecurity
@EnableAsync
public class FocalizzeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FocalizzeApplication.class, args);
	}


}
