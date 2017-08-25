package com.marsplay;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Timeouts;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.marsplay.scraper.Constants;
import com.marsplay.scraper.Extractor;
import com.marsplay.scraper.MyntraExtractor;

public class Application {
	private static ChromeDriverService seleniumService;
	private static WebDriver driver ;
	private static String query = "sunglasses men";
	
	public static void main(String[] args) throws IOException, InterruptedException {
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
	    driver.get(props.getProperty("endsite.myntra.url"));
	    Timeouts timeouts = driver.manage().timeouts();
	    timeouts.pageLoadTimeout(Long.parseLong(props.getProperty("endsite.common.page_load_timeout_seconds")), TimeUnit.SECONDS);
	    driver.manage().window().maximize();
	    Thread.sleep(1000);
	    
	    Extractor extractor = new MyntraExtractor(driver);
	    extractor.searchAction(query);
	    // TODO: Add Filter pattern here for Sorting and Add Myntra site Filters
	    Thread.sleep(200);
	    extractor.scrapeAction();
	    
//	    driver.quit();
//	    seleniumService.stop();
		System.out.println("Done.");
		

	
	}


}
