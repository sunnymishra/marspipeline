package com.marsplay.scraper;

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

import com.marsplay.scraper.agents.Agent;
import com.marsplay.scraper.lib.Constants;
import com.marsplay.scraper.lib.Constants.Endsites;

@Configuration
public class AgentsConfig {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AgentsConfig.class);
	private Properties applicationProps = Constants.getApplicationProps();
	private Properties businessProps = Constants.getBusinessProps();

	@Autowired
	@Qualifier("myntraAgent")
	private Agent myntraAgent;

	@Autowired
	@Qualifier("amazonAgent")
	private Agent amazonAgent;
	
	@Autowired
	@Qualifier("flipkartAgent")
	private Agent flipkartAgent;

	@Autowired
	@Qualifier("koovsAgent")
	private Agent koovsAgent;
	
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
	@Bean("agents")
	public List<Callable<String>> getAgents() {
		String activeAgentsStr = applicationProps.getProperty("scraper.agent.active");
		if(StringUtils.isEmpty(activeAgentsStr))
			return null;
		List<String> agents = Arrays.asList(activeAgentsStr.trim().split(","));
		List<Callable<String>> agentList = new ArrayList<Callable<String>>();
		List<Endsites> agentsAccepted=new ArrayList<Endsites>();
		for(String agentString: agents){
			Endsites agent=null;
			try {
				agent = Endsites.valueOf(agentString);
			} catch (IllegalArgumentException e) {
				LOGGER.error("#### ILLEGAL_ENUM_VALUE:{}",agentString,e.getMessage());
				continue;
			}
			
			switch(agent){
				case MYNTRA: agentList.add(myntraAgent);agentsAccepted.add(Endsites.MYNTRA);break;
				case AMAZON: agentList.add(amazonAgent);agentsAccepted.add(Endsites.AMAZON);break;
				case FLIPKART: agentList.add(flipkartAgent);agentsAccepted.add(Endsites.FLIPKART);break;
				case KOOVS: agentList.add(koovsAgent);agentsAccepted.add(Endsites.KOOVS);break;
			}
		}
		LOGGER.info("#### AGENTS_ACCEPTED:{}",agentsAccepted.toString());
		return agentList;
	}
}