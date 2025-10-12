package com.example.springoauth2profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class SpringOauth2ProfileApplication {

	private static final Logger logger = LoggerFactory.getLogger(SpringOauth2ProfileApplication.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(SpringOauth2ProfileApplication.class);

		
		app.addListeners(event -> {
			if (event instanceof org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent) {
				Environment env = ((org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent) event).getEnvironment();
				logger.info("=== Spring OAuth2 Profile Application Starting ===");
				logger.info("Server Port: {}", env.getProperty("server.port", "8080"));
				logger.info("H2 Console Enabled: {}", env.getProperty("spring.h2.console.enabled", "false"));
				logger.info("H2 Console Path: {}", env.getProperty("spring.h2.console.path", "/h2-console"));
				logger.info("Database URL: {}", env.getProperty("spring.datasource.url", "not configured"));
			}
		});

		app.run(args);
	}

}
