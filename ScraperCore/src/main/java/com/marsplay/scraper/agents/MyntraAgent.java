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
	public Object launchEndsite(Job job) {
		long localStart = System.currentTimeMillis();
		// extractor.searchAction(job.getMessage());
		try {
			// The below line will open the endsite on the headless browser
			driver.get(businessProps.getProperty("myntra.endsite.searchurl")
					+ job.getMessage());
			// driver.manage().window().maximize();
			LOGGER.info("#######Selenium Launched Endsite successfully");
		} catch (org.openqa.selenium.TimeoutException e) {
			LOGGER.error("Could not open Myntra endsite", e);
		}
		LOGGER.info(Util.logTime(job, endsite, "ENDITE_MYNTRA_OPEN", localStart));
		return null;
	}
	
	@Override
	public void scrapeAction(Job job) throws Exception {
		launchEndsite(job);
		LOGGER.info("Scraping Myntra for Job '{}'", job);
		//Thread.sleep(1000);
		long start=System.currentTimeMillis();
		capturePageScreenshot(driver, job.getId());	// Taking Page screenshot
		LOGGER.info(Util.logTime(job, endsite, "PAGE_SCREENSHOT", start));
		
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
			itemVO.setEndsite(Endsites.MYNTRA.name());
			++counter;
//			getElementScreenshot(item, "item"+counter, job.getId());
			// item.sendKeys( Keys.DOWN ); //simulate visual movement
			boolean isElementLoaded = false;
			while (!isElementLoaded) {
				try {
					try {
						WebElement url = waitAndExtractElement(
								item,
								ElementType.XPATH,
								businessProps
										.getProperty("myntra.relative.xpath.url1"));
						String endsiteUrl = url.getAttribute("href");
						if(StringUtil.isBlank(endsiteUrl)){
							LOGGER.error("endsiteUrl not found. Skipping jobId:{} for Endsite:{}", job.getId(), endsite);
							break;
						}
						itemVO.setEndsiteUrl(endsiteUrl);
					} catch (org.openqa.selenium.NoSuchElementException e) {
//						getElementScreenshot(item, "brand"+counter, job.getId());
						LOGGER.error(endsite+".{}.ENDSITE_URL_NOT_FOUND__SKIPPING_ITEM",job.getId());
					}
					try {
						WebElement brand = item.findElement(By.xpath(businessProps
								.getProperty("myntra.relative.xpath.brand")));
						itemVO.setBrand(brand.getText());
					}catch (org.openqa.selenium.NoSuchElementException e) {
						LOGGER.error(endsite+".{}.BRAND_NOT_FOUND__IGNORING.{}",job.getId(),itemVO.getEndsiteUrl());
					}
					try {
						WebElement name = item.findElement(By.xpath(businessProps
								.getProperty("myntra.relative.xpath.name")));
						itemVO.setName(name.getText());
					}catch (org.openqa.selenium.NoSuchElementException e) {
						LOGGER.error(endsite+".{}.NAME_NOT_FOUND__IGNORING.{}",job.getId(),itemVO.getEndsiteUrl());
					}

					WebElement price = null;
					try {
						price = item.findElement(By.xpath(businessProps
								.getProperty("myntra.relative.xpath.price1")));
//						getElementScreenshot(item, "priceA"+counter, job.getId());
					} catch (org.openqa.selenium.NoSuchElementException e) {
						LOGGER.warn(endsite + "." + job.getId()	+ ".PRICE1_NOT_FOUND__TRYING_PRICE2.{}",itemVO.getEndsiteUrl());
						try {
							price = item.findElement(By.xpath(businessProps
									.getProperty("myntra.relative.xpath.price2")));
						} catch (org.openqa.selenium.NoSuchElementException e1) {
							LOGGER.error(endsite + "." + job.getId()+ ".PRICE1_NOT_FOUND__IGNORING.{}",itemVO.getEndsiteUrl());
						}
//						getElementScreenshot(item, "priceB"+counter, job.getId());
					}
					try {
						if(price!=null)
							itemVO.setPrice(formatPrice(price.getText()));
					} catch (IllegalArgumentException e) {
						LOGGER.error(endsite + "." + job.getId()+ ".PRICE_FORMATTING_FAILED__IGNORING.{}.{}",itemVO.getEndsiteUrl(), e.getMessage());
					}

					WebElement image=null;
					try {
						// Image load may take time, therefore using Selenium
						// FluentWait API below
						image = waitAndExtractElement(
								item,
								ElementType.XPATH,
								businessProps
										.getProperty("myntra.relative.xpath.image1"));
					} catch (org.openqa.selenium.NoSuchElementException e) {
						LOGGER.warn(endsite+".{}.IMAGE1_NOT_FOUND__TRYING_IMAGE2.{}",job.getId(),itemVO.getEndsiteUrl());
						try {
							image = waitAndExtractElement(
									item,
									ElementType.XPATH,
									businessProps
											.getProperty("myntra.relative.xpath.image2"));
						} catch (org.openqa.selenium.NoSuchElementException e1) {
							LOGGER.error(endsite + "." + job.getId()+ ".IMAGE2_NOT_FOUND__IGNORING.{}",itemVO.getEndsiteUrl());
						}
					}
					itemVO.setEndsiteImageUrl(image.getAttribute("src"));

					isElementLoaded = true;
				} catch (org.openqa.selenium.NoSuchElementException e) {
					LOGGER.error(endsite+"."+job.getId()+".ELEMENT_NOT_FOUND__SLEEPING_RETRYING.{}",itemVO.getEndsiteUrl()
							, e.getMessage());
					Thread.sleep(1000);
					// TODO If this Exception times out, then Log this error for
					// Flagging Scraping error
					isElementLoaded = false;
					retry++;
					if (retry >= Integer.parseInt(businessProps
							.getProperty("common.element_fetch_retry_max"))) {
						LOGGER.error(endsite+"."+job.getId()+".MAXED_OUT_RETRIES_ELEMENT_NOT_FOUND__THROWING_EXEPTION.{}",itemVO.getEndsiteUrl());
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
					LOGGER.info(Util.logTime(job, endsite, "CLOUDINARY_UPLOAD", start));

					itemVO.setCdnImageUrl((String) responseMap.get("secure_url"));
					itemVO.setCdnImageId((String) responseMap.get("public_id"));
				} catch (Exception e) {
					LOGGER.error("Cloudinary_upload_exception for Job:"+job.getId() + " Endsite:{}, itemUrl:{}",endsite, itemVO.getEndsiteUrl());
				}
				
				start=System.currentTimeMillis();
				itemRepository.save(itemVO);
				LOGGER.info(Util.logTime(job, endsite, "MONGO_SAVE_ITEM", start));
			} catch (Exception e) {
//				e.printStackTrace();
				Throwable rootException = ExceptionUtils.getRootCause(e);
				if (rootException instanceof DuplicateKeyException) {
					// Eat exception here else throw exception
					LOGGER.warn(endsite+"."+job.getId()+".MONGO_DuplicateKeyException_EATING itemUrl:{}",
							itemVO.getEndsiteUrl());
				} else {
					LOGGER.error(endsite+"."+job.getId()+".MONGO_SAVE_EXCEPTION itemUrl:{}",
							itemVO.getEndsiteUrl(),e);
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
		String extraText = businessProps.getProperty("myntra.price.extratext");
		if(priceTemp.contains(extraText)){
			priceTemp = priceTemp.replace(extraText,"");
		}
		priceTemp=priceTemp.trim();
		BigDecimal price1=null;
		try {
			price1 = new BigDecimal(priceTemp);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Not able to parse price:\""+price+"\"",e);
		}
		return price1;
	}
}
