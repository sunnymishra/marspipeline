package com.marsplay.core;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
/*	@Bean
	CommandLineRunner init(AccountRepository accountRepository,
			BookmarkRepository bookmarkRepository) {
		return (evt) -> Arrays.asList(
				"simple,complex,faulty,regular".split(","))
				.forEach(
						a -> {
							Bookmark bookmark = bookmarkRepository.save(new Bookmark(a, "A lame description"));
							Account account = accountRepository.save(new Account("Account_"+a,"password", bookmark));
							
						});
	}*/
}