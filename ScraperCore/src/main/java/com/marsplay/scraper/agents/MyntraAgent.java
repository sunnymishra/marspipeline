package com.marsplay.scraper.agents;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jetty.util.StringUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.marsplay.repository.Item;
import com.marsplay.repository.ItemRepository;
import com.marsplay.repository.Job;
import com.marsplay.scraper.ScraperService;
import com.marsplay.scraper.lib.Constants.ElementType;
import com.marsplay.scraper.lib.Constants.Endsites;
import com.marsplay.scraper.lib.Util;
import com.mongodb.DuplicateKeyException;

@Service("myntraAgent")
public class MyntraAgent extends Agent implements Callable<String>{
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
	@Autowired
	private ItemRepository itemRepository;
	
	@Autowired
	@Qualifier("myntraWebDriver")
	private WebDriver driver;
	
	private Job job;

//	public MyntraAgent(WebDriver driver, ItemRepository itemRepository) {
	public MyntraAgent() {
//		super(driver);
		super();
		LOGGER.info("Constructor MyntraAgent()");
//		this.itemRepository = itemRepository;
		this.endsite = Endsites.MYNTRA;
		// PageFactory.initElements(driver, this);
	}
	@Override
    public String call() throws Exception {
		scrapeAction(job);
        return "success";
    }
	@Override
	public void setJob(Job job) {
		this.job = job;
	}
	@Override
	public Job getJob() {
		return this.job;
	}
	@Override
	public void scrapeAction(Job job) throws Exception {
		LOGGER.info("Scraping Myntra for Job '{}'", job);
		//Thread.sleep(1000);
		long start=System.currentTimeMillis();
		capturePageScreenshot(driver, job.getId());	// Taking Page screenshot
		LOGGER.info(Util.logTime(start, "PAGE_SCREENSHOT"));
		
		WebElement container = driver.findElement(By.xpath(businessProps
				.getProperty("myntra.xpath.container")));
		List<WebElement> itemContainer = driver.findElements(By
				.xpath(businessProps.getProperty("myntra.xpath.item")));
		int counter = 0;
		int scrollHeight = -1, scrollItemCount = -1;
		
		for (WebElement item : itemContainer) {
			int retry = 0;
			// ItemVO itemVO = new ItemVO(Endsites.MYNTRA);
			Item itemVO = new Item();
			itemVO.setJob(job);
			itemVO.setEndSite(Endsites.MYNTRA.name());
			++counter;
//			getElementScreenshot(item, "item"+counter, job.getId());
			// item.sendKeys( Keys.DOWN ); //simulate visual movement
			boolean isElementLoaded = false;
			while (!isElementLoaded) {
				try {
					WebElement url = waitAndExtractElement(
							item,
							ElementType.XPATH,
							businessProps
									.getProperty("myntra.relative.xpath.url"));
					String endsiteUrl = url.getAttribute("href");
					if(StringUtil.isBlank(endsiteUrl)){
						LOGGER.error("endsiteUrl not found. Skipping jobId:{} for Endsite:{}", job.getId(), endsite);
						break;
					}
					itemVO.setEndsiteUrl(url.getAttribute("href"));
					WebElement brand = item.findElement(By.xpath(businessProps
							.getProperty("myntra.relative.xpath.brand")));
					itemVO.setBrand(brand.getText());
//					getElementScreenshot(item, "brand"+counter, job.getId());
					// WebElement element =
					// webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("periodicElement")));
					WebElement name = item.findElement(By.xpath(businessProps
							.getProperty("myntra.relative.xpath.name")));
					itemVO.setName(name.getText());
//					getElementScreenshot(item, "name"+counter, job.getId());
					LOGGER.info(itemVO.toString());

					WebElement price = null;
					try {
						price = item.findElement(By.xpath(businessProps
								.getProperty("myntra.relative.xpath.price1")));
//						getElementScreenshot(item, "priceA"+counter, job.getId());
					} catch (org.openqa.selenium.NoSuchElementException e) {
						LOGGER.error("Price1 Exception for JobId:"+job.getId()+" Endsite:"+endsite + " endsiteUrl:"+ itemVO.getEndsiteUrl()+"", e.getMessage());
						try {
							price = item.findElement(By.xpath(businessProps
									.getProperty("myntra.relative.xpath.price2")));
						} catch (org.openqa.selenium.NoSuchElementException e1) {
							LOGGER.error("Price2 Exception for JobId:"+job.getId()+" Endsite:"+endsite + " endsiteUrl:"+ itemVO.getEndsiteUrl()+"", e1.getMessage());
						}
//						getElementScreenshot(item, "priceB"+counter, job.getId());
					}
					try {
						itemVO.setPrice(formatPrice(price.getText()));
					} catch (IllegalArgumentException e) {
						// TODO Log this error for Flagging Scraping error
						e.printStackTrace();
					}

					WebElement image;
					try {
						// Image load may take time, therefore using Selenium
						// FluentWait API below
						image = waitAndExtractElement(
								item,
								ElementType.XPATH,
								businessProps
										.getProperty("myntra.relative.xpath.image1"));
					} catch (org.openqa.selenium.NoSuchElementException e) {
						image = waitAndExtractElement(
								item,
								ElementType.XPATH,
								businessProps
										.getProperty("myntra.relative.xpath.image2"));
					}
					itemVO.setEndsiteImageUrl(image.getAttribute("src"));

					isElementLoaded = true;
				} catch (org.openqa.selenium.NoSuchElementException e) {
					LOGGER.error(
							"Failed to find element for JobId="+job.getId()+", Endsite={}, ErrorMessage={}",
							endsite, e.getMessage());
					Thread.sleep(1000);
					// TODO If this Exception times out, then Log this error for
					// Flagging Scraping error
					isElementLoaded = false;
					retry++;
					if (retry >= Integer.parseInt(businessProps
							.getProperty("common.element_fetch_retry_max"))) {
						LOGGER.error(
								"We maxed out retries to find element for JobId="+job.getId()+", Endsite={}. ErrorMessage={}. Throwing exception now.",
								endsite, e.getMessage());
						throw e;
					}
				}

			}
			try {
				if(!isElementLoaded){
					LOGGER.error("Skipping jobId:{} for Endsite:{}", job.getId(), endsite);
					break;
				}
				try {
					start=System.currentTimeMillis();
					Map<String, Object> responseMap = uploadFile(itemVO
							.getEndsiteImageUrl());
					LOGGER.info(Util.logTime(start, "CLOUDINARY_UPLOAD"));

					itemVO.setCdnImageUrl((String) responseMap.get("secure_url"));
					itemVO.setCdnImageId((String) responseMap.get("public_id"));
				} catch (Exception e) {
					LOGGER.error("Cloudinary_upload_exception for Job:"+job.getId() + " Endsite:{}, itemUrl:{}",endsite, itemVO.getEndsiteUrl());
				}
				
				start=System.currentTimeMillis();
				itemRepository.save(itemVO);
				LOGGER.info(Util.logTime(start, "MONGO_SAVE_ITEM"));
			} catch (Exception e) {
//				e.printStackTrace();
				Throwable rootException = ExceptionUtils.getRootCause(e);
				if (rootException instanceof DuplicateKeyException) {
					// Eat exception here else throw exception
					LOGGER.warn("Eating Mongo DuplicateKeyException here in Extractor for Job:"+job.getId()+" endsite:{}, itemUrl:{}",endsite, itemVO.getEndsiteUrl());
				} else {
					LOGGER.error("Mongo save exception for JobId:"+job.getId()+", Endsite:{}, itemUrl:{}",endsite, itemVO.getEndsiteUrl());
				}

			} // MongoDB save in ITEM collection

			if (scrollItemCount == -1)
				scrollItemCount = Util.getNoOfItemsInEachRow(container
						.getSize().getWidth(), item.getSize().getWidth());
			if (counter % scrollItemCount == 0) {
				Thread.sleep(300);
				JavascriptExecutor jse = (JavascriptExecutor) driver;
				if (scrollHeight == -1)
					scrollHeight = Util
							.ceilPixel(
									item.getSize().getHeight(),
									Integer.parseInt(businessProps
											.getProperty("common.vertical_scroll_offset_pixel")));
				jse.executeScript("window.scrollBy(0," + scrollHeight + ")", "");
			}
			if (counter >= Integer.parseInt(businessProps
					.getProperty("common.no_of_items_to_scrape")))
				break;

		}
	}
	/**
	 * Myntra Scraped Price eg. "Rs. 500". This Util method
	 * trims the String and strips Alphabets 'Rs.' and then
	 * returns parsed Double of the price
	 * @param String
	 * @return double
	 */
	public BigDecimal formatPrice(String price){
		String priceTemp=price;
		if(priceTemp==null || priceTemp.isEmpty())
			throw new IllegalArgumentException("Price cannot be empty");
		priceTemp=priceTemp.trim();
		if(priceTemp.startsWith(businessProps.getProperty("myntra.price.extratext"))){
			priceTemp=priceTemp.substring(3);
			priceTemp=priceTemp.trim();
		}
		BigDecimal price1;
		try {
			price1 = new BigDecimal(priceTemp);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Not able to parse price:\""+price+"\"",e);
		}
		return price1;
	}
}
