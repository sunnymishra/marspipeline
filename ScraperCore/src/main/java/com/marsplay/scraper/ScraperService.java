package com.marsplay.scraper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cloudinary.Cloudinary;
import com.marsplay.repository.ItemRepository;
import com.marsplay.repository.Job;
import com.marsplay.repository.JobRepository;
import com.marsplay.repository.lib.Constants.JobStatus;
import com.marsplay.scraper.agents.Agent;
import com.marsplay.scraper.agents.AmazonAgent;
import com.marsplay.scraper.agents.MyntraAgent;
import com.marsplay.scraper.lib.CloudinarySingleton;
import com.marsplay.scraper.lib.Constants;
import com.marsplay.scraper.lib.ExecutorServiceExt;
import com.marsplay.scraper.lib.Util;

@Component
public class ScraperService implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
//	private ChromeDriverService seleniumService;
	@Autowired
	@Qualifier("myntraWebDriver")
	private WebDriver myntraWebDriver;

	Properties businessProps;
	Properties applicationProps;
	
	@Autowired
	@Qualifier("amazonAgent")
	private Agent amazonAgent;
	
	@Autowired
	@Qualifier("myntraAgent")
	private Agent myntraAgent;
	
	@Autowired
	private ItemRepository itemRepository;
	@Autowired
	private JobRepository jobRepository;

//	@Value("${selenium.server.url}")
//	private String seleniumServerUrl;

	@Override
	public void run(String... arg0) throws Exception {
		LOGGER.info("#######ENTER ScraperService.run().");
		businessProps = Constants.getBusinessProps();
		applicationProps = Constants.getApplicationProps();
		String cloudinaryUrl = applicationProps
				.getProperty("cloudinary.connection.url");
		// TODO: Cloudinary URL value should come from System variables set in
		// startupscript
		if (cloudinaryUrl != null && cloudinaryUrl != "")
			CloudinarySingleton
					.registerCloudinary(new Cloudinary(cloudinaryUrl));
		
	//	startScraping(new Job("sunglasses", new Date()));
//		startScraping(new Job("polka dots dress", new Date()));
//		startScraping(new Job("polka dots shirt", new Date()));
//		startScraping(new Job("polka dots tshirt", new Date()));
//		startScraping(new Job("polka dots top", new Date()));
	}

	public void startScraping(Job job) throws IOException, InterruptedException {
		long localStart = System.currentTimeMillis();
//		extractor.searchAction(job.getMessage());
		try {
//			myntraWebDriver.get(businessProps.getProperty("myntra.endsite.url")+job.getMessage());
//			driver.manage().window().maximize();
			LOGGER.info("#######Launched Endsite successfully");
		} catch (org.openqa.selenium.TimeoutException e) {
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
			amazonAgent.scrapeAction(job);	// This code will scrape the EndSite
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
	
	/*private ExecutorService executor = null;
	@Autowired
	@Qualifier("myntra22Agent")
	private Agent myntra22Agent;
	
	public void test() throws InterruptedException {
		executor = new ExecutorServiceExt(Executors.newFixedThreadPool(3));
		
		Job job1=new Job("aaa", new Date());
		Job job2=new Job("bbb", new Date());
		Job job3=new Job("ccc", new Date());
		Job job4=new Job("ddd", new Date());
		
		List<Job> jobs = Arrays.asList(job1, job2);
		
		jobs.stream()
			   .forEach((job) -> {
				   myntraAgent.setJob(job);
				   myntra22Agent.setJob(job);
					try {
						doThis(executor, Arrays.asList(myntraAgent, myntra22Agent));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});

		executor.shutdown();
	}

	private void doThis(ExecutorService executor, List<Callable<String>> tasks)
			throws InterruptedException {
		Job job = ((Agent)tasks.get(0)).getJob();
		List<Future<String>> futures = executor.invokeAll(tasks, 12,
				TimeUnit.SECONDS);
		futures.stream()
				.map(future -> {
					String str = null;
					try {
						str = job.getId()+"."+job.getMessage() + "." + future.get();
					} catch (Exception e) {
						LOGGER.error(
								"Caught Exception in doThis method. Stacktrace is::",
								e);
						// throw new IllegalStateException(e);
					}
					return str;
				}).forEach((s) -> {
					String success = s == null ? "Failure" : "Success";
					System.out.println(success + "->" + s);
				});
	}*/
	@PreDestroy
	public void cleanUp() throws Exception {
		LOGGER.info("Killing Selenium driver instances and SeleniumService, before Spring destroys ScraperService Bean");
		myntraWebDriver.quit();
		// seleniumService.stop();
	}
}
