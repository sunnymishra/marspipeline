package com.marsplay.scraper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cloudinary.Cloudinary;
import com.marsplay.repository.ItemRepository;
import com.marsplay.repository.Job;
import com.marsplay.repository.JobRepository;
import com.marsplay.repository.lib.Constants.JobStatus;
import com.marsplay.scraper.agents.Agent;
import com.marsplay.scraper.agents.MyntraAgent;
import com.marsplay.scraper.lib.CloudinarySingleton;
import com.marsplay.scraper.lib.Constants;
import com.marsplay.scraper.lib.Util;

@Component
public class ScraperService implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
	private ChromeDriverService seleniumService;
	private WebDriver driver;
	// private String query = "sunglasses men";
	Properties businessProps;
	Properties applicationProps;
	Agent extractor = null;

	@Autowired
	private ItemRepository itemRepository;
	@Autowired
	private JobRepository jobRepository;

	@Value("${selenium.server.url}")
	private String seleniumServerUrl;

	@Override
	public void run(String... arg0) throws Exception {
		LOGGER.info("#######ENTER ScraperService.run().");
		businessProps = Constants.getBusinessProps();
		applicationProps = Constants.getApplicationProps();
		initChrome();
		launchEndsite();
//		startScraping(new Job("rayban sunglasses", new Date()));
//		startScraping(new Job("polka dots dress", new Date()));
//		startScraping(new Job("polka dots shirt", new Date()));
//		startScraping(new Job("polka dots tshirt", new Date()));
//		startScraping(new Job("polka dots top", new Date()));
	}

	private void initChrome() throws IOException {
		LOGGER.info("#######Initializing Chrome...");
		String cloudinaryUrl = applicationProps
				.getProperty("cloudinary.connection.url");
		// TODO: Cludinary URL value should come from System variables set in
		// startupscript
		if (cloudinaryUrl != null && cloudinaryUrl != "")
			CloudinarySingleton
					.registerCloudinary(new Cloudinary(cloudinaryUrl));
		// Note: If application.properties doesn't have cloudinary url,
		// then CoudinarySingleton will expect and fetch it from JVM args

		// TODO: Use DriverManager class here

		/*
		 * String chromeDriver = applicationProps
		 * .getProperty("chrome.driver.path"); seleniumService = new
		 * ChromeDriverService.Builder() .usingDriverExecutable(new
		 * File(chromeDriver)) .usingAnyFreePort().build();
		 * seleniumService.start();
		 */
	}

	private void launchEndsite() throws InterruptedException,
			MalformedURLException {
		LOGGER.info("#######Launching Endsite Myntra.");
		URL serverUrl = new URL(seleniumServerUrl);
		// URL serverUrl = seleniumService.getUrl(),
		driver = new RemoteWebDriver(serverUrl, DesiredCapabilities.chrome());

		Timeouts timeouts = driver.manage().timeouts();
		timeouts.pageLoadTimeout(Long.parseLong(businessProps
				.getProperty("common.page_load_timeout_seconds")),
				TimeUnit.SECONDS);
		/*try {
			driver.get(businessProps.getProperty("myntra.url"));
			driver.manage().window().maximize();
			LOGGER.info("#######Launched Endsite success####");
		} catch (org.openqa.selenium.TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		extractor = new MyntraAgent(driver, itemRepository);
		// TODO: See that we can avoid putting this constructor inside
		// startScraping(),else multiple objects will be created needlessly
	}

	public void startScraping(Job job) throws IOException, InterruptedException {
		long localStart = System.currentTimeMillis();
//		extractor.searchAction(job.getMessage());
		try {
			driver.get(businessProps.getProperty("myntra.url")+job.getMessage());
//			driver.manage().window().maximize();
			LOGGER.info("#######Launched Endsite successfully");
//			Thread.sleep(1000);
		} catch (org.openqa.selenium.TimeoutException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Could not open Myntra endsite", e);
		}
		LOGGER.info(Util.logTime(localStart, "ENDITE_MYNTRA_OPEN"));
		// TODO: Add Filter pattern here for Sorting and Add Myntra site Filters
		Thread.sleep(200);
		try {
			localStart = System.currentTimeMillis();
			job.setStatus(JobStatus.INPROGRESS.name());
			job.setUpdatedDate(new Date());
			jobRepository.save(job);
			LOGGER.info(Util.logTime(localStart, "MONGO_UPDATE_JOB"));

			localStart = System.currentTimeMillis();
			extractor.scrapeAction(job);	// This code will scrape the EndSite
			LOGGER.info(Util.logTime(localStart, "SCRAPE_WORK"));

			localStart = System.currentTimeMillis();
			job.setStatus(JobStatus.FINISHED.name());
			job.setUpdatedDate(new Date());
			jobRepository.save(job);
			LOGGER.info(Util.logTime(localStart, "MONGO_UPDATE_SUCCESS_JOB"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LOGGER.info("EXIT ScraperService.startScraping().");
	}

	@PreDestroy
	public void cleanUp() throws Exception {
		LOGGER.info("Killing Selenium driver instances and SeleniumService, before Spring destroys ScraperService Bean");
		driver.quit();
		// seleniumService.stop();
	}
}
