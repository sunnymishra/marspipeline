package com.marsplay.scraper.agents;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import us.codecraft.xsoup.Xsoup;

import com.marsplay.repository.Item;
import com.marsplay.repository.ItemRepository;
import com.marsplay.repository.Job;
import com.marsplay.scraper.ScraperService;
import com.marsplay.scraper.lib.Constants.ElementType;
import com.marsplay.scraper.lib.Constants.Endsites;
import com.marsplay.scraper.lib.Util;
import com.mongodb.DuplicateKeyException;

@Service("amazonAgent")
public class AmazonAgent extends Agent {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
	@Autowired
	private ItemRepository itemRepository;

	private Job job;

	public AmazonAgent() {
		super();
		LOGGER.info("Constructor AmazonAgent()");
		// this.itemRepository = itemRepository;
		this.endsite = Endsites.AMAZON;
		// PageFactory.initElements(driver, this);
	}

	@Override
	public String call() throws Exception {
		scrapeAction(job);
		return "success";
	}

	public void setJob(Job job) {
		this.job = job;
	}

	@Override
	public Job getJob() {
		return this.job;
	}

	@Override
	public void scrapeAction(Job job) throws Exception {
		LOGGER.info("Scraping {} for Job '{}'", endsite, job);

		String endsiteBaseUrl = businessProps.getProperty("amazon.endsite.url");
		String endsiteUrl = endsiteBaseUrl + job.getMessage();
		/*
		 * byte[] fileBytes = Files.readAllBytes(Paths.get("D:/amazon.html"));
		 * String htmlStr = new String(fileBytes); Document document =
		 * Jsoup.parse(htmlStr);
		 */
		long start = System.currentTimeMillis();
		int pageLoadTimeout=(int)TimeUnit.SECONDS.toMillis(Long.parseLong(businessProps.getProperty("common.page_load_timeout_seconds")));
		Document document = Jsoup.connect(endsiteUrl)
								 .timeout(pageLoadTimeout)
								 .get();
		LOGGER.info(Util.logTime(start, "ENDITE_AMAZON_OPEN"));

		start = System.currentTimeMillis();
		saveScrapedHtml(document, job.getId()); // Saving Scraped Html
		LOGGER.info(Util.logTime(start, "SAVE_AMAZON_HTML"));

		Elements itemContainer = Xsoup
				.compile(businessProps.getProperty("amazon.xpath.container"))
				.evaluate(document).getElements();
		Elements itemContainer1 = itemContainer.select(businessProps
				.getProperty("amazon.xpath.item"));
		int counter = 0;
		int exceptionSkippingCounter = 0;
		String exceptionSkippingMaxCountStr = businessProps
				.getProperty("exception.skipping.maxcount");
		int exceptionSkippingMaxCount = Integer
				.parseInt(exceptionSkippingMaxCountStr);

		for (Element item : itemContainer1) {
			Item itemVO = new Item();
			itemVO.setJob(job);
			itemVO.setEndSite(endsite.name());
			
			Elements urlElem = Xsoup
					.compile(
							businessProps
									.getProperty("amazon.relative.xpath.url"))
					.evaluate(item).getElements();
			itemVO.setEndsiteUrl(urlElem.attr("href"));
			if (StringUtil.isBlank(itemVO.getEndsiteUrl())) {
				LOGGER.error(
						"endsiteUrl not found. Skipping jobId:{} for Endsite:{}",
						job.getId(), endsite.name());
				exceptionSkippingCounter++;
				if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
					LOGGER.error(
							"exceptionSkippingCounter breached max for JobId:{} and Endsite:{}",
							job.getId(), endsite.name());
					break;
				} else
					continue;
			}
			++counter;

			String brandPath = businessProps
					.getProperty("amazon.relative.xpath.brand");
			String brand = "";
			if (!StringUtil.isBlank(brandPath)) {
				Elements brandElem = Xsoup.compile(brandPath).evaluate(item)
						.getElements();
				brand = brandElem.text();
			}

			itemVO.setBrand(brand);
			Elements name = Xsoup
					.compile(
							businessProps
									.getProperty("amazon.relative.xpath.name"))
					.evaluate(item).getElements();
			itemVO.setName(name.text());
			Elements image = Xsoup
					.compile(
							businessProps
									.getProperty("amazon.relative.xpath.image1"))
					.evaluate(item).getElements();
			itemVO.setEndsiteImageUrl(image.attr("src"));
			Elements price = Xsoup
					.compile(
							businessProps
									.getProperty("amazon.relative.xpath.price1"))
					.evaluate(item).getElements();
			try {
				itemVO.setPrice(formatPrice(price.text()));
			} catch (IllegalArgumentException e) {
				LOGGER.error("Price1 Exception for JobId:" + job.getId()
						+ " itemUrl:\"" + itemVO.getEndsiteUrl() + "\"::"
						+ e.getMessage());
				price = Xsoup
						.compile(
								businessProps
										.getProperty("amazon.relative.xpath.price2"))
						.evaluate(item).getElements();
				try {
					itemVO.setPrice(formatPrice(price.text()));
				} catch (IllegalArgumentException e1) {
					LOGGER.error("Price2 Exception for JobId:" + job.getId()
							+ " itemUrl:\"" + itemVO.getEndsiteUrl() + "\"::",
							e1.getMessage());
				}
			}
			
			validateScrapedHtml(job.getId(), itemVO);	// This will log what itemVO attributes were not found
			
			try {
				start = System.currentTimeMillis();
				Map<String, Object> responseMap = uploadFile(itemVO
						.getEndsiteImageUrl());
				LOGGER.info(Util.logTime(start, "CLOUDINARY_UPLOAD"));

				itemVO.setCdnImageUrl((String) responseMap.get("secure_url"));
				itemVO.setCdnImageId((String) responseMap.get("public_id"));
			} catch (Exception e) {
				LOGGER.error("Cloudinary_upload_exception for Job:{}, Endsite:{}, itemUrl:{} "
						, job.getId(), endsite ,itemVO.getEndsiteUrl());
				exceptionSkippingCounter++;
				if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
					LOGGER.error(
							"exceptionSkippingCounter breached max for JobId:{} and itemUrl:{}",
							job.getId(), itemVO.getEndsiteUrl());
					break;
				} else
					continue;
			}
			try {
				start = System.currentTimeMillis();
				itemRepository.save(itemVO);
				LOGGER.info(Util.logTime(start, "MONGO_SAVE_ITEM"));
			} catch (Exception e) {
				// e.printStackTrace();
				Throwable rootException = ExceptionUtils.getRootCause(e);
				if (rootException instanceof DuplicateKeyException) {
					// Eat exception here else throw exception
					LOGGER.warn("Eating Mongo DuplicateKeyException here in Extractor for Job:"
							+ job.getId()
							+ " itemUrl:"
							+ itemVO.getEndsiteUrl());
				} else {
					LOGGER.error(
							"Mongo save exception for JobId:{} and itemUrl:{}",
							job.getId(), itemVO.getEndsiteUrl());
					exceptionSkippingCounter++;
					if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
						LOGGER.error(
								"exceptionSkippingCounter breached max for JobId:{} and itemUrl:{}",
								job.getId(), itemVO.getEndsiteUrl());
						break;
					} else
						continue;
				}

			} // MongoDB save in ITEM collection
			if (counter >= Integer.parseInt(businessProps
					.getProperty("common.no_of_items_to_scrape")))
				break;

		}

	}

	/**
	 * Scraped Price eg. "  3,910 -   8,890" or "  2,745" This Util method trims
	 * the String and strips Alphabets 'Rs.' and then returns parsed Double of
	 * the price
	 * 
	 * @param String
	 * @return double
	 * @throws
	 */
	public BigDecimal formatPrice(String price) {
		String priceTemp = price;
		if (priceTemp == null)
			throw new IllegalArgumentException("Price cannot be null");
		priceTemp = priceTemp.trim();
		if (priceTemp.isEmpty())
			throw new IllegalArgumentException("Price cannot be empty");
		String extraText = businessProps.getProperty("amazon.price.extratext");
		if (priceTemp.contains(extraText)) {
			priceTemp = priceTemp.substring(0, priceTemp.indexOf(extraText));
		}
		BigDecimal price1;
		priceTemp = priceTemp.trim();
		priceTemp = priceTemp.replaceAll("\u00A0", "");
		try {
			NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
			Number number = format.parse(priceTemp);
			price1 = new BigDecimal(number.toString());
		} catch (NumberFormatException | ParseException e) {
			throw new IllegalArgumentException("Not able to parse price:\""
					+ price + "\"", e);
		}
		return price1;
	}

}
