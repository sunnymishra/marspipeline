package com.marsplay.scraper.agents;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.common.base.Function;
import com.marsplay.scraper.lib.CloudinarySingleton;
import com.marsplay.scraper.lib.Constants;
import com.marsplay.scraper.lib.FluentElementWait;
import com.marsplay.scraper.lib.Util;
import com.marsplay.scraper.lib.Constants.ElementType;
import com.marsplay.scraper.lib.Constants.Endsites;

public abstract class Agent {
	WebDriver driver = null;
	protected Properties businessProps = null;
	protected Properties applicationProps = null;
	protected Endsites endsite = null;

	public Agent(WebDriver driver) {
		this.driver = driver;
		businessProps = Constants.getBusinessProps();
		applicationProps = Constants.getApplicationProps();
	}

	public void searchAction(String query) throws InterruptedException {
		WebElement searchTextbox = driver.findElement(By.xpath(businessProps
				.getProperty("endsite.myntra.xpath.searchbox")));
		WebElement searchButton = driver.findElement(By.xpath(businessProps
				.getProperty("endsite.myntra.xpath.searchbutton")));
		searchTextbox.sendKeys(query);
		searchButton.click();
	}

	public WebElement waitAndExtractElement(WebElement elem,
			ElementType elemType, String elemIdentifier) {
		FluentElementWait wait = new FluentElementWait(elem)
				.withTimeout(
						Long.parseLong(businessProps
								.getProperty("endsite.common.element_load_timeout_seconds")),
						TimeUnit.SECONDS)
				.pollingEvery(
						Long.parseLong(businessProps
								.getProperty("endsite.common.element_load_sleep_timeout_millis")),
						TimeUnit.MILLISECONDS)
				.ignoring(NoSuchElementException.class,
						StaleElementReferenceException.class);
		WebElement myElement = wait
				.until(new Function<WebElement, WebElement>() {
					public WebElement apply(WebElement parent) {
						WebElement element = null;
						if (elemType.equals(ElementType.ID))
							element = parent.findElement(By.id(elemIdentifier));
						else if (elemType.equals(ElementType.XPATH))
							element = parent.findElement(By
									.xpath(elemIdentifier));
						return element;
					}
				});
		return myElement;
	}

	public abstract void scrapeAction() throws Exception;

	public String getPageScreenshot() throws IOException {
		TakesScreenshot imgcapture = (TakesScreenshot) driver;
		File screen = imgcapture.getScreenshotAs(OutputType.FILE);
		String fpath = "D:\\MyProject\\reports_Page_" + Util.getTime()
				+ ".html";
		FileUtils.copyFile(screen, new File(fpath));
		return fpath;
	}

	public String getElementScreenshot(WebElement ele) throws Exception {
		TakesScreenshot imgcapture = (TakesScreenshot) driver;
		File screenshot = imgcapture.getScreenshotAs(OutputType.FILE);
		BufferedImage fullImg = ImageIO.read(screenshot);

		// Get the location of element on the page
		Point point = ele.getLocation();

		// Get width and height of the element
		int eleWidth = ele.getSize().getWidth();
		int eleHeight = ele.getSize().getHeight();

		// Crop the entire page screenshot to get only element screenshot
		BufferedImage eleScreenshot = fullImg.getSubimage(point.getX(),
				point.getY(), eleWidth, eleHeight);
		ImageIO.write(eleScreenshot, "png", screenshot);
		String path = "D:\\MyProject\\reports_Elem_" + Util.getTime() + ".html";
		// Copy the element screenshot to disk
		FileUtils.copyFile(screenshot, new File(path));
		return path;
	}

	public String uploadFile(String imageUrl) throws IOException {
		// new PhotoUploadValidator().validate(photoUpload, result);
		Map uploadResult = null;
		uploadResult = CloudinarySingleton
				.getCloudinary()
				.uploader()
				.upload(imageUrl,
						ObjectUtils.asMap("resource_type", "auto", "tags",
								endsite));
		return (String) uploadResult.get("secure_url");
	}
}
