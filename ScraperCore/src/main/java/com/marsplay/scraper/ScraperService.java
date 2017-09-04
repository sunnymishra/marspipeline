package com.marsplay.scraper;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.cloudinary.Cloudinary;
import com.marsplay.repository.ItemRepository;
import com.marsplay.scraper.agents.Agent;
import com.marsplay.scraper.agents.MyntraAgent;
import com.marsplay.scraper.lib.CloudinarySingleton;
import com.marsplay.scraper.lib.Constants;

@SpringBootApplication(scanBasePackages = { "com.marsplay.scraper",
		"com.marsplay.repository" })
@EnableMongoRepositories(basePackages = { "com.marsplay.repository" })
public class ScraperService implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScraperService.class);
	private ChromeDriverService seleniumService;
	private WebDriver driver;
	private String query = "sunglasses men";
	Properties businessProps;
	Properties applicationProps;

	@Autowired
	private ItemRepository itemRepository;

	public static void main(String[] args) throws Exception {
		LOGGER.info("Spring boot starting....");
		ConfigurableApplicationContext context = SpringApplication.run(
				ScraperService.class, args);
		LOGGER.info("Scraper Spring boot started='{}'", "Successfully");
	}

	@Override
	public void run(String... arg0) throws Exception {
		System.out.println("#######Inside ScraperService method now ########");
		businessProps = Constants.getBusinessProps();
		applicationProps = Constants.getApplicationProps();
//		startScraping();

	}

	public void startScraping() throws IOException, InterruptedException {
		String cloudinaryUrl = applicationProps
				.getProperty("cloudinary.connection.url");
		if (cloudinaryUrl != null && cloudinaryUrl != "")
			CloudinarySingleton
					.registerCloudinary(new Cloudinary(cloudinaryUrl));
		// Note: If application.properties doesn't have cloudinary url,
		// then CoudinarySingleton will expect and fetch it from JVM args

		// TODO: Use DriverManager class here
		// TODO: This value should come from System variables set in startup
		// script
		String chromeDriver = applicationProps
				.getProperty("chrome.driver.path");
		seleniumService = new ChromeDriverService.Builder()
				.usingDriverExecutable(new File(chromeDriver))
				.usingAnyFreePort().build();
		seleniumService.start();
		driver = new RemoteWebDriver(seleniumService.getUrl(),
				DesiredCapabilities.chrome());
		// Properties props = Constants.getBusinessProps();
		Timeouts timeouts = driver.manage().timeouts();
		timeouts.pageLoadTimeout(Long.parseLong(businessProps
				.getProperty("endsite.common.page_load_timeout_seconds")),
				TimeUnit.SECONDS);

		try {
			driver.get(businessProps.getProperty("endsite.myntra.url"));
		} catch (org.openqa.selenium.TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		driver.manage().window().maximize();
		Thread.sleep(1000);

		Agent extractor = new MyntraAgent(driver, itemRepository);
		// extractor.setDriver(driver);
		extractor.searchAction(query);
		// TODO: Add Filter pattern here for Sorting and Add Myntra site Filters
		Thread.sleep(200);
		try {
			extractor.scrapeAction();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// driver.quit();
		// seleniumService.stop();
		LOGGER.info("Done.");

	}

}
