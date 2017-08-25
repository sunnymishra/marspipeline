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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.marsplay.repository.ItemRepository;

@Component(value="scraperService")
public class ScraperService implements CommandLineRunner {
	private ChromeDriverService seleniumService;
	private WebDriver driver ;
	private String query = "sunglasses men";
	
	@Autowired
	private ItemRepository itemRepository;
	
	@Override
	public void run(String... arg0) throws Exception {
		System.out.println("Commented ScraperService method here...................................");
		System.out.println("......................................................................");
		
		startScraping();
		
	}
	
	public void startScraping() throws IOException, InterruptedException {
		// TODO: Use DriverManager class here 
		// TODO: This value should come from System variables set in startup script
		String chromeDriver="C:\\Users\\smishra5\\Documents\\chromedriver.exe";
		seleniumService = new ChromeDriverService.Builder()
							        .usingDriverExecutable(new File(chromeDriver))
							        .usingAnyFreePort()
							        .build();
		seleniumService.start();
		driver = new RemoteWebDriver(seleniumService.getUrl(), DesiredCapabilities.chrome());
		Properties props = Constants.getProps();
		Timeouts timeouts = driver.manage().timeouts();
	    timeouts.pageLoadTimeout(Long.parseLong(props.getProperty("endsite.common.page_load_timeout_seconds")), TimeUnit.SECONDS);
		
	    try {
			driver.get(props.getProperty("endsite.myntra.url"));
		} catch (org.openqa.selenium.TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    driver.manage().window().maximize();
	    Thread.sleep(1000);
	    
	    Extractor extractor = new MyntraExtractor(driver, itemRepository);
//	    extractor.setDriver(driver);
	    extractor.searchAction(query);
	    // TODO: Add Filter pattern here for Sorting and Add Myntra site Filters
	    Thread.sleep(200);
	    extractor.scrapeAction();
	    
//	    driver.quit();
//	    seleniumService.stop();
		System.out.println("Done.");
		
	
	}



}
