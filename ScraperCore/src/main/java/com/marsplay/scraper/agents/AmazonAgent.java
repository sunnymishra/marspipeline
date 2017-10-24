package com.marsplay.scraper.agents;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import us.codecraft.xsoup.Xsoup;

import com.marsplay.repository.Item;
import com.marsplay.repository.ItemRepository;
import com.marsplay.repository.Job;
import com.marsplay.scraper.ScraperService;
import com.marsplay.scraper.lib.Constants.Endsites;
import com.marsplay.scraper.lib.Util;
import com.mongodb.DuplicateKeyException;

@Service("amazonAgent")
public class AmazonAgent extends Agent {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
	@Autowired
	private ItemRepository itemRepository;

	public AmazonAgent() {
		super();
		LOGGER.info("Constructor AmazonAgent()");
		// this.itemRepository = itemRepository;
		this.endsite = Endsites.AMAZON;
		// PageFactory.initElements(driver, this);
	}

	@Override
	public Object launchEndsite(Job job) throws Exception {
		String endsiteBaseUrl = businessProps
				.getProperty("amazon.endsite.searchurl");
		String endsiteUrl = endsiteBaseUrl + job.getMessage();
		/*
		 * byte[] fileBytes = Files.readAllBytes(Paths.get("D:/amazon.html"));
		 * String htmlStr = new String(fileBytes); Document document =
		 * Jsoup.parse(htmlStr);
		 */
		long start = System.currentTimeMillis();
		int pageLoadTimeout = (int) TimeUnit.SECONDS.toMillis(Long
				.parseLong(businessProps
						.getProperty("common.page_load_timeout_seconds")));
		// Document document = Jsoup.connect(endsiteUrl)
		// .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
		// .timeout(pageLoadTimeout)
		// .get();
		Connection con = Jsoup
				.connect(endsiteUrl)
				.userAgent(
						"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
				.timeout(pageLoadTimeout);
		Connection.Response resp = con.execute();
		if (resp.statusCode() != 200)
			throw new HttpStatusException("Couldn't fetch Endsite url",
					resp.statusCode(), endsiteUrl);
		Document document = con.get();

		LOGGER.info(Util.logTime(job, endsite, "ENDITE_AMAZON_OPEN", start));
		return document;

	}

	@Override
	public void scrapeAction(Job job) throws Exception {
		LOGGER.info("Scraping {} for Job '{}'", endsite, job);

		Document document = (Document) launchEndsite(job);

		long start = System.currentTimeMillis();
		saveScrapedHtml(document, job.getId()); // Saving Scraped Html
		LOGGER.info(Util.logTime(job, endsite, "SAVE_AMAZON_HTML", start));

		Elements itemContainer = Xsoup
				.compile(businessProps.getProperty("amazon.xpath.container"))
				.evaluate(document).getElements();
		if (itemContainer.isEmpty()) {
			LOGGER.error(endsite + "." + job.getId()
					+ ".PARSING_FAILURE_EMPTY_CONTAINER");
			return;
		}
		Elements itemContainer1 = itemContainer.select(businessProps
				.getProperty("amazon.xpath.item"));
		if (itemContainer1.isEmpty()) {
			LOGGER.error(endsite + "." + job.getId()
					+ ".PARSING_FAILURE_EMPTY_SUB_CONTAINER");
			return;
		}
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

			Elements sponsoredElem1 = Xsoup
					.compile(
							businessProps
									.getProperty("amazon.relative.xpath.sponsoreditem"))
					.evaluate(item).getElements();
			if (!sponsoredElem1.isEmpty()) {
				LOGGER.warn(endsite + "." + job.getId()
						+ ".FOUND_SPONSORED_ELEMENT__SKIPPING");
				continue;
			}

			Elements urlElem = Xsoup
					.compile(
							businessProps
									.getProperty("amazon.relative.xpath.url1"))
					.evaluate(item).getElements();
			if (urlElem.isEmpty()) {
				LOGGER.warn(endsite + "." + job.getId()
						+ ".NOT_FOUND_ENDSITE_URL1__TRYING_URL2");

				urlElem = Xsoup
						.compile(
								businessProps
										.getProperty("amazon.relative.xpath.url2"))
						.evaluate(item).getElements();
				if (urlElem.isEmpty()) {
					LOGGER.error(endsite + "." + job.getId()
							+ ".NOT_FOUND_ENDSITE_URL2__SKIPPING_ITEM");
					exceptionSkippingCounter++;
					if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
						LOGGER.error(endsite
								+ "."
								+ job.getId()
								+ ".EXCEPTION_SKIPPING_COUNTER_EXCEEDED__BREAKINGLOOP");
						break;
					} else
						continue;
				}
			}
			itemVO.setEndsiteUrl(urlElem.attr("href"));

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
			if (image.isEmpty()) {
				LOGGER.warn(
						endsite
								+ "."
								+ job.getId()
								+ ".NOT_FOUND_IMAGE_URL1__TRYING_IMAGE_URL2.endsiteUrl:{}",
						itemVO.getEndsiteUrl());
				image = Xsoup
						.compile(
								businessProps
										.getProperty("amazon.relative.xpath.image2"))
						.evaluate(item).getElements();
				if (image.isEmpty()) {
					LOGGER.warn(
							endsite
									+ "."
									+ job.getId()
									+ ".NOT_FOUND_IMAGE_URL2__MAY_IGNORE.endsiteUrl:{}",
							itemVO.getEndsiteUrl());
					exceptionSkippingCounter++;
					if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
						LOGGER.error(endsite
								+ "."
								+ job.getId()
								+ ".EXCEPTION_SKIPPING_COUNTER_EXCEEDED__BREAKINGLOOP");
						break;
					}
				}

			}
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

			validateScrapedHtml(job.getId(), itemVO); // This will log what
														// itemVO attributes
														// were not found
			try {
				start = System.currentTimeMillis();
				Map<String, Object> responseMap = uploadFile(itemVO
						.getEndsiteImageUrl());
				LOGGER.info(Util.logTime(job, endsite, "CLOUDINARY_UPLOAD",
						start));

				itemVO.setCdnImageUrl((String) responseMap.get("secure_url"));
				itemVO.setCdnImageId((String) responseMap.get("public_id"));
			} catch (Exception e) {
				LOGGER.error(endsite + "." + job.getId()
						+ ".CLOUDINARY_UPLOAD_EXCEPTION", e);
				exceptionSkippingCounter++;
				if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
					LOGGER.error(
							endsite
									+ "."
									+ job.getId()
									+ ".CLOUDINARY_UPLOAD_EXCEPTION__SKIPPING_ITEM. itemUrl:{}",
							itemVO.getEndsiteUrl());
					break;
				}
			}
			try {
				start = System.currentTimeMillis();
				itemRepository.save(itemVO);
				LOGGER.info(Util
						.logTime(job, endsite, "MONGO_SAVE_ITEM", start));
			} catch (Exception e) {
				// e.printStackTrace();
				Throwable rootException = ExceptionUtils.getRootCause(e);
				if (rootException instanceof DuplicateKeyException) {
					// Eat exception here else throw exception
					LOGGER.warn(endsite + "." + job.getId()
							+ ".MONGO_DuplicateKeyException_EATING itemUrl:{}",
							itemVO.getEndsiteUrl());
				} else {
					LOGGER.error(endsite + "." + job.getId()
							+ ".MONGO_SAVE_FAILURE itemUrl:{}",
							itemVO.getEndsiteUrl(), e);
					exceptionSkippingCounter++;
					if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
						LOGGER.error(endsite + "." + job.getId()
								+ ".EXCEPTION_SKIPPED_COUNTER itemUrl:{}",
								itemVO.getEndsiteUrl(), e);
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
