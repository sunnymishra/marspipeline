package com.marspipeline.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marspipeline.scraper.agents.Agent;
import com.marspipeline.scraper.lib.Constants;
import com.marspipeline.scraper.lib.Constants.Endsites;

@Configuration
public class WebDriverAgentConfig {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(WebDriverAgentConfig.class);
	private Properties applicationProps = Constants.getApplicationProps();
	private Properties businessProps = Constants.getBusinessProps();

	@Bean("myntraWebDriver")
	public WebDriver myntraWebDriver() throws IOException {
		LOGGER.info("#### Initializing Myntra Chrome...");
		ChromeOptions options = new ChromeOptions();
		System.setProperty("webdriver.chrome.driver",
				applicationProps.getProperty("chrome.driver.path"));
		options.addArguments("--headless", "--disable-gpu",
				"--window-size=1920,1520"); // 1280,1696	"--incognito",
		WebDriver myntraDriver = new ChromeDriver(options);

		Timeouts myntraTimeouts = myntraDriver.manage().timeouts();
		myntraTimeouts.pageLoadTimeout(Long.parseLong(businessProps
				.getProperty("common.page_load_timeout_seconds")),
				TimeUnit.SECONDS);
		return myntraDriver;
	}
}