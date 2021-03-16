package com.marspipeline.scraper.agents;

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

import com.marspipeline.repository.Item;
import com.marspipeline.repository.ItemRepository;
import com.marspipeline.repository.Job;
import com.marspipeline.scraper.ScraperService;
import com.marspipeline.scraper.lib.Constants.Endsites;
import com.marspipeline.scraper.lib.Util;
import com.mongodb.DuplicateKeyException;

@Service("nykaaAgent")
public class NykaaAgent extends Agent {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
	@Autowired
	private ItemRepository itemRepository;

	public NykaaAgent() {
		super();
		LOGGER.info("Constructor NykaaAgent()");
		// this.itemRepository = itemRepository;
		this.endsite = Endsites.NYKAA;
		// PageFactory.initElements(driver, this);
	}

	 public static void main(String[] args) throws Exception {
		 NykaaAgent agent = new NykaaAgent();
		 agent.scrapeAction(new Job("lipstick", new Date()));
	 }
	@Override
	public Object launchEndsite(Job job) throws Exception {
		String endsiteBaseSearchUrl = businessProps
				.getProperty("nykaa.endsite.searchurl");
		String endsiteSearchUrl = endsiteBaseSearchUrl + job.getMessage();
		long start = System.currentTimeMillis();
		int pageLoadTimeout = (int) TimeUnit.SECONDS.toMillis(Long
				.parseLong(businessProps
						.getProperty("common.page_load_timeout_seconds")));
		Connection con = Jsoup
				.connect(endsiteSearchUrl)
				.userAgent(
						"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
				.timeout(pageLoadTimeout);
		Connection.Response resp = con.execute();
		if (resp.statusCode() != 200)
			throw new HttpStatusException("Couldn't fetch Endsite url",
					resp.statusCode(), endsiteSearchUrl);
		Document document = con.get();

		LOGGER.info(Util.logTime(job, endsite, "ENDITE_NYKAA_OPEN", start));
		return document;

	}

	@Override
	public void scrapeAction(Job job) throws Exception {
		LOGGER.info("Scraping {} for Job '{}'", endsite, job);

		Document document = (Document) launchEndsite(job);

		long start = System.currentTimeMillis();
		saveScrapedHtml(document, job.getId()); // Saving Scraped Html
		LOGGER.info(Util.logTime(job, endsite, "SAVE_HTML", start));

		Elements itemContainer = Xsoup
				.compile(businessProps.getProperty("nykaa.xpath.container"))
				.evaluate(document).getElements();
		if (itemContainer.isEmpty()) {
			LOGGER.error(endsite + "." + job.getId()
					+ ".PARSING_FAILURE_EMPTY_CONTAINER");
			return;
		}
		Elements itemContainer1 = Xsoup
				.compile(businessProps.getProperty("nykaa.xpath.item"))
				.evaluate(itemContainer.first()).getElements();
		
//		Elements itemContainer1 = itemContainer.select(businessProps
//				.getProperty("nykaa.xpath.item"));
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
			itemVO.setEndsite(endsite.name());

			Elements urlElem = Xsoup
					.compile(
							businessProps
									.getProperty("nykaa.relative.xpath.url1"))
					.evaluate(item).getElements();
			if (urlElem.isEmpty()) {
				LOGGER.error(endsite + "." + job.getId()
						+ ".NOT_FOUND_ENDSITE_URL1__SKIPPING_ITEM");
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
			itemVO.setEndsiteUrl(businessProps.getProperty("nykaa.endsite.baseurl")+urlElem.attr("href"));

			++counter;

			String brandPath = businessProps
					.getProperty("nykaa.relative.xpath.brand");
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
									.getProperty("nykaa.relative.xpath.name"))
					.evaluate(item).getElements();
			itemVO.setName(name.text());
			Elements image = Xsoup
					.compile(
							businessProps
									.getProperty("nykaa.relative.xpath.image1"))
					.evaluate(item).getElements();
			if (image.isEmpty()) {
				LOGGER.warn(
						endsite
								+ "."
								+ job.getId()
								+ ".NOT_FOUND_IMAGE1__MAY_IGNORE.endsiteUrl:{}",
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

			if (!image.isEmpty()) {
				itemVO.setEndsiteImageUrl(image.attr("src"));
			}
			Elements price = Xsoup
					.compile(
							businessProps
									.getProperty("nykaa.relative.xpath.price1"))
					.evaluate(item).getElements();
			try {
				itemVO.setPrice(formatPrice(price.text(), true));
			} catch (IllegalArgumentException e) {
				LOGGER.error(endsite
						+ "."
						+ job.getId()
						+ ".PRICE1_FORMATTING_EXCEPTION__MAY_IGNORE.{}",e.getMessage());
				exceptionSkippingCounter++;
				if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
					LOGGER.error(endsite
							+ "."
							+ job.getId()
							+ ".EXCEPTION_SKIPPING_COUNTER_EXCEEDED__BREAKINGLOOP");
					break;
				}
			}

			// This will log not found itemVO attributes
			validateScrapedHtml(job.getId(), itemVO);
			
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
									+ ".CLOUDINARY_UPLOAD_EXCEPTION__SKIPPING_ITEM.endsiteUrl:{}",
							itemVO.getEndsiteUrl());
					break;
				}
			}
			try {
				start = System.currentTimeMillis();
				itemRepository.save(itemVO);
//				System.out.println(itemVO);
				
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
	public BigDecimal formatPrice(String price, boolean isFormattingRequired) {
		String priceTemp = price;
		if (priceTemp == null)
			throw new IllegalArgumentException("Price cannot be null");
		priceTemp = priceTemp.trim();
		if (priceTemp.isEmpty())
			throw new IllegalArgumentException("Price cannot be empty");
		if(isFormattingRequired){
			String extraText = businessProps.getProperty("nykaa.price.extratext");
			if (priceTemp.contains(extraText)) {
				priceTemp = priceTemp.replace(extraText,"");
			}
		}
		BigDecimal price1=null;
		priceTemp = priceTemp.trim();
		try {
			NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
			Number number = format.parse(priceTemp);
			price1 = new BigDecimal(number.toString());
		} catch (IllegalArgumentException | ParseException e) {
			throw new IllegalArgumentException("Not able to parse price:\""
					+ price + "\"", e);
		}
		return price1;
	}

}
