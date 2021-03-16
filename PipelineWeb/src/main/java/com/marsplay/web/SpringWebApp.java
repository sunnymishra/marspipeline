package com.marspipeline.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = { "com.marspipeline.web",
		"com.marspipeline.repository" })
@EnableMongoRepositories(basePackages = { "com.marspipeline.repository" })
public class SpringWebApp {

	public static void main(String[] args) throws Exception {
		System.out.println("Spring boot starting....");
		ConfigurableApplicationContext context = SpringApplication.run(
				SpringWebApp.class, args);
		// CommandLineRunner scraperService =
		// context.getBean(ScraperService.class);
		System.out.println("Spring boot started....");
	}

	// }

	/*
	 * @Bean CommandLineRunner init(AccountRepository accountRepository,
	 * BookmarkRepository bookmarkRepository) { return (evt) -> Arrays.asList(
	 * "simple,complex,faulty,regular".split(",")) .forEach( a -> { List<Item>
	 * items = itemRepository.findAll(); items.forEach(a ->
	 * System.out.println(a.getName())); Bookmark bookmark =
	 * bookmarkRepository.save(new Bookmark(a, "A lame description")); Account
	 * account = accountRepository.save(new Account("Account_"+a,"password",
	 * bookmark));
	 * 
	 * }); }
	 */
}