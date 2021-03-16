package com.marspipeline.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = { "com.marspipeline.scraper",
		"com.marspipeline.repository" })
@EnableMongoRepositories(basePackages = { "com.marspipeline.repository" })
public class SpringScraperApp{
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringScraperApp.class);

	public static void main(String[] args) throws Exception {
		LOGGER.info("######Spring boot starting....");
		ConfigurableApplicationContext context = SpringApplication.run(
				SpringScraperApp.class, args);
		LOGGER.info("######Scraper Spring boot started='{}'", "Successfully");
	}

}
