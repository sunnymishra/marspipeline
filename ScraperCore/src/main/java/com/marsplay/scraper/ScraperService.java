package com.marsplay.scraper;

import java.io.File;
import java.io.IOException;
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
	
	@Override
	public void run(String... arg0) throws Exception {
		LOGGER.info("#######Inside ScraperService method now ########");
		businessProps = Constants.getBusinessProps();
		applicationProps = Constants.getApplicationProps();
		initChrome();
		launchEndsite();
	}

	private void initChrome() throws IOException {
		LOGGER.info("#######Initializing Chrome ########");
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
	}

	private void launchEndsite() throws InterruptedException {
		LOGGER.info("#######Launching Endsite Myntra ########");
		driver = new RemoteWebDriver(seleniumService.getUrl(),
				DesiredCapabilities.chrome());

		Timeouts timeouts = driver.manage().timeouts();
		timeouts.pageLoadTimeout(Long.parseLong(businessProps
				.getProperty("common.page_load_timeout_seconds")),
				TimeUnit.SECONDS);
		try {
			driver.get(businessProps.getProperty("myntra.url"));
		} catch (org.openqa.selenium.TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		driver.manage().window().maximize();
		extractor = new MyntraAgent(driver, itemRepository);
		// TODO: See that we can avoid putting this constructor inside
		// startScraping(),else multiple objects will be created needlessly
		Thread.sleep(1000);
	}

	public void startScraping(Job job) throws IOException, InterruptedException {
		long localStart=System.currentTimeMillis();
		extractor.searchAction(job.getMessage());
		long localDuration=System.currentTimeMillis()-localStart;
		LOGGER.info("Search duration:"+ ((int) (localDuration / 1000) % 60)+"s "+((int) (localDuration%1000))+"m");
		// TODO: Add Filter pattern here for Sorting and Add Myntra site Filters
		Thread.sleep(200);
		try {
			localStart=System.currentTimeMillis();
			job.setStatus(JobStatus.INPROGRESS.name());
			job.setUpdatedDate(new Date());
			jobRepository.save(job);
			localDuration=System.currentTimeMillis()-localStart;
			LOGGER.info("MongoDB Job update1 duration:"+ ((int) (localDuration / 1000) % 60)+"s "+((int) (localDuration%1000))+"m");
			
			localStart=System.currentTimeMillis();
			extractor.scrapeAction(job);
			localDuration=System.currentTimeMillis()-localStart;
			LOGGER.info("Only Scraping duration:"+ ((int) (localDuration / 1000) % 60)+"s "+((int) (localDuration%1000))+"m");
			
			localStart=System.currentTimeMillis();
			job.setStatus(JobStatus.FINISHED.name());
			job.setUpdatedDate(new Date());
			jobRepository.save(job);
			localDuration=System.currentTimeMillis()-localStart;
			LOGGER.info("MongoDB Job update2 duration:"+ ((int) (localDuration / 1000) % 60)+"s "+((int) (localDuration%1000))+"m");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LOGGER.info("Existing startScraping() method.");
	}

	@PreDestroy
	public void cleanUp() throws Exception {
		LOGGER.info("Killing Selenium driver instances and SeleniymService, before Spring destroys ScraperService Bean");
		driver.quit();
		seleniumService.stop();
	}
}
