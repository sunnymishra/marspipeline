package com.marsplay.scraper;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.marsplay.scraper.lib.Constants;

@Configuration
public class WebDriverConfig {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(WebDriverConfig.class);
	private Properties applicationProps = Constants.getApplicationProps();
	private Properties businessProps = Constants.getBusinessProps();

	@Bean("myntraWebDriver")
	public WebDriver myntraWebDriver() throws IOException {
		LOGGER.info("#######Initializing Myntra Chrome...");
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