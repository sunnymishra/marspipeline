package com.marsplay.scraper;

import java.io.IOException;
import java.util.ArrayList;
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

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cloudinary.Cloudinary;
import com.marsplay.repository.ItemRepository;
import com.marsplay.repository.Job;
import com.marsplay.repository.JobRepository;
import com.marsplay.repository.lib.Constants.JobStatus;
import com.marsplay.scraper.agents.Agent;
import com.marsplay.scraper.lib.CloudinarySingleton;
import com.marsplay.scraper.lib.Constants;
import com.marsplay.scraper.lib.Constants.Endsites;
import com.marsplay.scraper.lib.ExecutorServiceExt;
import com.marsplay.scraper.lib.Util;

@Component
public class ScraperService implements CommandLineRunner {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
	// private ChromeDriverService seleniumService;
	@Autowired
	@Qualifier("myntraWebDriver")
	private WebDriver myntraWebDriver;

	Properties businessProps;
	Properties applicationProps;
	
	@Autowired
	private ItemRepository itemRepository;
	@Autowired
	private JobRepository jobRepository;
	@Autowired
	private List<Callable<String>> agents;
	
	private ExecutorService executor = null;

	@Override
	public void run(String... arg0) throws Exception {
		LOGGER.info("#######ENTER ScraperService.run().");
		businessProps = Constants.getBusinessProps();
		applicationProps = Constants.getApplicationProps();
		String cloudinaryUrl = applicationProps
				.getProperty("cloudinary.connection.url");
		// TODO: Cloudinary URL value should come from System variables
		//  set in startupscript
		if (cloudinaryUrl != null && cloudinaryUrl != "")
			CloudinarySingleton
					.registerCloudinary(new Cloudinary(cloudinaryUrl));
		
		int threadSize=agents.size();
		LOGGER.info("Thread size:{}",threadSize);
		executor = new ExecutorServiceExt(Executors.newFixedThreadPool(threadSize));

//		startScraping(new Job("sunglasses", new Date()));
		startScraping(new Job("polka dots dress", new Date()));
		startScraping(new Job("polka dots shirt", new Date()));
//		startScraping(new Job("polka dots tshirt", new Date()));
//		startScraping(new Job("polka dots top", new Date()));
//		startScraping(new Job("jasmine top", new Date()));
//		startScraping(new Job("jeans", new Date()));
	}

	public void startScraping(Job job) throws IOException, InterruptedException {
		// TODO: Add Filter pattern here for Sorting and Add Myntra site Filters
		Thread.sleep(200);
		try {
			long localStart = System.currentTimeMillis();
			job.setStatus(JobStatus.INPROGRESS.name());
			job.setUpdatedDate(new Date());
			jobRepository.save(job);
			LOGGER.info(Util.logTime(job, null, "MONGO_UPDATE_JOB", localStart));

			localStart = System.currentTimeMillis();

			callAgentIter(job);

			LOGGER.info(Util.logTime(job, null,  "JOB_SCRAPES_COMPLETION", localStart));

			localStart = System.currentTimeMillis();
			job.setStatus(JobStatus.FINISHED.name());
			job.setUpdatedDate(new Date());
			jobRepository.save(job);
			LOGGER.info(Util.logTime(job, null, "MONGO_UPDATE_SUCCESS_JOB",localStart));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LOGGER.info("EXIT ScraperService.startScraping().");
	}

	public void callAgentIter(Job job) throws InterruptedException {
		agents.stream().forEach(agent -> {
			((Agent) agent).setJob(job);
		});
		
		try {
			callAgent(executor, agents);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void callAgent(ExecutorService executor,
			List<Callable<String>> tasks) throws InterruptedException {
		Job job = ((Agent) tasks.get(0)).getJob();
		List<Future<String>> futures = executor.invokeAll(tasks, 12,
				TimeUnit.SECONDS);
		
		futures.stream()
				.map(future -> {
					String str = null;
					try {
						str = future.get();
					} catch (Exception e) {
						LOGGER.error(
								"Caught Exception in doThis() method. Stacktrace is::",
								e);
						// throw new IllegalStateException(e);
					}
					return str;
				}).forEach((s) -> {
					if (s != null && s.contains("SUCCESS"))
						LOGGER.info(s + "." + job.getId());
					else
						LOGGER.error(s + "." + job.getId());
				});
	}

	

	@PreDestroy
	public void cleanUp() throws Exception {
		LOGGER.info("Killing Selenium driver instances and SeleniumService, before Spring destroys ScraperService Bean");
		myntraWebDriver.quit();
		executor.shutdown();
		// seleniumService.stop();
	}
}
