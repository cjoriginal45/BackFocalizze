package com.focalizze.Focalizze;

import com.focalizze.Focalizze.models.User;
import com.focalizze.Focalizze.repository.UserRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FocalizzeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FocalizzeApplication.class, args);
	}


}
