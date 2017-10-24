package com.marsplay.scraper.agents;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudinary.utils.ObjectUtils;
import com.google.common.base.Function;
import com.marsplay.repository.Item;
import com.marsplay.repository.Job;
import com.marsplay.scraper.lib.CloudinarySingleton;
import com.marsplay.scraper.lib.Constants;
import com.marsplay.scraper.lib.Constants.ElementType;
import com.marsplay.scraper.lib.Constants.Endsites;
import com.marsplay.scraper.lib.Constants.ItemAttribute;
import com.marsplay.scraper.lib.FluentElementWait;
import com.marsplay.scraper.lib.Util;

public abstract class Agent implements Callable<String> {
	private static final Logger LOGGER = LoggerFactory.getLogger(Agent.class);

	protected Properties businessProps = null;
	protected Properties applicationProps = null;
	protected Endsites endsite = null;

	// WebDriver driver = null;
	// public Agent(WebDriver driver) {
	public Agent() {
		// this.driver = driver;
		businessProps = Constants.getBusinessProps();
		applicationProps = Constants.getApplicationProps();
	}

	protected Job job;

	public void setJob(Job job) {
		this.job = job;
	}

	public Job getJob() {
		return this.job;
	}

	@Override
	public String call() throws Exception {
		try {
			scrapeAction(job);
			return "SUCCESS." + endsite.name();
		} catch (Exception e) {
			LOGGER.error("Exception for JobId:" + job.getId()
					+ " for Endsite:{} \"::\"", endsite.name(), e);
		}
		return "FAILURE." + endsite.name();
	}

	/*
	 * public void searchAction(String query) throws InterruptedException {
	 * WebElement searchTextbox = driver.findElement(By.xpath(businessProps
	 * .getProperty("myntra.xpath.searchbox"))); WebElement searchButton =
	 * driver.findElement(By.xpath(businessProps
	 * .getProperty("myntra.xpath.searchbutton"))); long
	 * start=System.currentTimeMillis(); searchTextbox.sendKeys(query); long
	 * duration=System.currentTimeMillis()-start;
	 * LOGGER.info("Search typing duration:"+ ((int) (duration / 1000) %
	 * 60)+"s "+((int) (duration%1000))+"m");
	 * 
	 * start=System.currentTimeMillis(); searchButton.click();
	 * duration=System.currentTimeMillis()-start;
	 * LOGGER.info("Search buttonclick duration:"+ ((int) (duration / 1000) %
	 * 60)+"s "+((int) (duration%1000))+"m");
	 * 
	 * }
	 */
	public abstract Object launchEndsite(Job job) throws Exception;
	
	public WebElement waitAndExtractElement(WebElement elem,
			ElementType elemType, String elemIdentifier) {
		FluentElementWait wait = new FluentElementWait(elem)
				.withTimeout(
						Long.parseLong(businessProps
								.getProperty("common.element_load_timeout_seconds")),
						TimeUnit.SECONDS)
				.pollingEvery(
						Long.parseLong(businessProps
								.getProperty("common.element_load_sleep_timeout_millis")),
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

	public abstract void scrapeAction(Job job) throws Exception;

	public String capturePageScreenshot(WebDriver driver, String jobId)
			throws IOException {
		TakesScreenshot imgcapture = (TakesScreenshot) driver;
		File screen = imgcapture.getScreenshotAs(OutputType.FILE);
		String screenshotBasePath = applicationProps
				.getProperty("scraper.screenshot.path");
		String fpath = screenshotBasePath + File.separator + "" + endsite
				+ "." + jobId + ".png";
		FileUtils.copyFile(screen, new File(fpath));
		return fpath;
	}

	public String captureElementScreenshot(WebDriver driver, WebElement elem,
			String elemName, String jobId) throws Exception {
		TakesScreenshot imgcapture = (TakesScreenshot) driver;
		File screenshot = imgcapture.getScreenshotAs(OutputType.FILE);
		BufferedImage fullImg = ImageIO.read(screenshot);

		// Get the location of element on the page
		Point point = elem.getLocation();

		// Get width and height of the element
		int eleWidth = elem.getSize().getWidth();
		int eleHeight = elem.getSize().getHeight();

		// Crop the entire page screenshot to get only element screenshot
		BufferedImage eleScreenshot = fullImg.getSubimage(point.getX(),
				point.getY(), eleWidth, eleHeight);
		ImageIO.write(eleScreenshot, "png", screenshot);

		String screenshotBasePath = applicationProps
				.getProperty("scraper.screenshot.path");
		String path = screenshotBasePath + File.separator + elemName
				+ "." + endsite + "." + jobId + "." + Util.getTime()
				+ ".png";
		// Copy the element screenshot to disk
		FileUtils.copyFile(screenshot, new File(path));
		return path;
	}

	public void saveScrapedHtml(Document doc, String jobId) throws IOException {
		String htmlBasePath = applicationProps.getProperty("scraper.html.path");
		if (!Files.exists(Paths.get(htmlBasePath)))
			Files.createDirectories(Paths.get(htmlBasePath));
		String fpath = htmlBasePath + File.separator + endsite + ".jobId."
				+ jobId + ".html";
		Files.write(Paths.get(fpath), doc.toString().getBytes());
	}

	public Map<String, Object> uploadFile(String imageUrl) throws IOException {
		// new PhotoUploadValidator().validate(photoUpload, result);
		Map uploadResult = null;
		uploadResult = CloudinarySingleton
				.getCloudinary()
				.uploader()
				.upload(imageUrl,
						ObjectUtils.asMap("resource_type", "auto", "tags",
								endsite));
		return uploadResult;
	}

	public void validateScrapedHtml(String jobId, Item item) {
		List<ItemAttribute> errorProperties = new ArrayList<ItemAttribute>();

		if (!isAttributeIgnorable(ItemAttribute.BRAND)
				&& StringUtil.isBlank(item.getBrand())) {
			errorProperties.add(ItemAttribute.BRAND);
		}
		if (!isAttributeIgnorable(ItemAttribute.IMAGEURL)
				&& StringUtil.isBlank(item.getEndsiteImageUrl())) {
			errorProperties.add(ItemAttribute.IMAGEURL);
		}
		if (!isAttributeIgnorable(ItemAttribute.NAME)
				&& StringUtil.isBlank(item.getName())) {
			errorProperties.add(ItemAttribute.NAME);
		}
		if (!isAttributeIgnorable(ItemAttribute.PRICE)
				&& item.getPrice() == null) {
			errorProperties.add(ItemAttribute.PRICE);
		}
		if (!errorProperties.isEmpty())
			LOGGER.error("ATTRIBUTE_NOT_FOUND Job:" + jobId + ", Endsite:"
					+ endsite + ", itemUrl:{}, Attributes Missing: ",
					item.getEndsiteUrl(), errorProperties);
		// return errorProperties;

	}

	/**
	 * This method tells whether it is okay to ignore a certain attribute of any
	 * Endsite's properties. For eg. For Amazon BRAND is always blank in UI. So
	 * we will ignore that Item attribute and not log any error also.
	 * 
	 * @param attr
	 * @return
	 */
	private boolean isAttributeIgnorable(ItemAttribute attr) {
		String itemAttrPath = endsite.name() + ".relative.xpath." + attr.name();
		return StringUtil.isBlank(businessProps.getProperty(itemAttrPath));
	}

}
