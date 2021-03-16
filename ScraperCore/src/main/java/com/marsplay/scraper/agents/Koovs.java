package com.marspipeline.scraper.agents;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minidev.json.JSONArray;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import us.codecraft.xsoup.Xsoup;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.marspipeline.repository.Item;
import com.marspipeline.repository.ItemRepository;
import com.marspipeline.repository.Job;
import com.marspipeline.scraper.ScraperService;
import com.marspipeline.scraper.lib.Constants.Endsites;
import com.marspipeline.scraper.lib.Util;
import com.mongodb.DuplicateKeyException;

@Service("koovsAgent")
public class Koovs extends Agent {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
	@Autowired
	private ItemRepository itemRepository;

	public Koovs() {
		super();
		LOGGER.info("Constructor KoovsAgent()");
		this.endsite = Endsites.KOOVS;
	}

	@Override
	public Object launchEndsite(Job job) throws Exception {
		String endsiteBaseUrl = businessProps
				.getProperty("koovs.endsite.searchurl");
		String endsiteUrl = endsiteBaseUrl + job.getMessage();

		long start = System.currentTimeMillis();
		int pageLoadTimeout = (int) TimeUnit.SECONDS.toMillis(Long
				.parseLong(businessProps
						.getProperty("common.page_load_timeout_seconds")));
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

		LOGGER.info(Util.logTime(job, endsite, "ENDITE_KOOVS_OPEN", start));
		return document;

	}
	/*public static void main(String[] args) throws Exception {
		Koovs koovs = new Koovs();
		koovs.scrapeAction(new Job("jeans", new Date()));
	}*/
	@Override
	public void scrapeAction(Job job) throws Exception {
		LOGGER.info("Scraping {} for Job '{}'", endsite, job);

		Document document = (Document) launchEndsite(job);

		long start = System.currentTimeMillis();
		saveScrapedHtml(document, job.getId()); // Saving Scraped Html
		LOGGER.info(Util.logTime(job, endsite, "SAVE_HTML", start));

		Elements scripts = document.select("script"); // Get the script part
		DocumentContext json = null;
		for (Element script : scripts) {
			// Regex for the value of the key
			Pattern p = Pattern.compile(businessProps
					.getProperty("koovs.scripttag.jsonpath"));

			Matcher m = p.matcher(script.html());

			if (m.find()) {
				String jsonString = m.group(2);
				json = JsonPath.parse(jsonString);
				break;
			}
		}
		if (json == null) {
			LOGGER.error(endsite + "." + job.getId()
					+ ".JSON_SCRIPT_TAG_MISSING__RETURNING_CALLER");
			return;
		}
		Elements itemContainer = Xsoup
				.compile(businessProps.getProperty("koovs.xpath.container"))
				.evaluate(document).getElements();
		if (itemContainer.isEmpty()) {
			LOGGER.error(endsite + "." + job.getId()
					+ ".PARSING_FAILURE_EMPTY_CONTAINER");
			return;
		}
		Elements itemContainer1 = itemContainer.select(businessProps
				.getProperty("koovs.xpath.item"));
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
									.getProperty("koovs.relative.xpath.url1"))
					.evaluate(item).getElements();
			if (urlElem.isEmpty()) {
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
			}
			String baseUrl = businessProps.getProperty("koovs.endsite.baseurl");
			String uri = urlElem.attr("href");
			String completeUrl = baseUrl + uri;
			itemVO.setEndsiteUrl(completeUrl);

			++counter;
			Optional<NameValuePair> pair = getUrlId(uri, businessProps.getProperty("koovs.item.json.idkey"));
			if (!pair.isPresent()) {
				LOGGER.error(endsite + "." + job.getId()
						+ ".SKUID_MISSING_IN_ENDSITEURL__SKIPPING");
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
			String skuId = pair.get().getValue();
			// Filter idFilter = Filter.filter(Criteria.where("sku").eq(skuId));
			// JSONArray itemJsonArrayString = json.read("$.listData.data[?]",
			// idFilter);

			JSONArray itemJsonArrayString;
			try {
				String jsonItemPath= businessProps.getProperty("koovs.relative.json.item");
				jsonItemPath=jsonItemPath.replace("_SKUID_",skuId);
				itemJsonArrayString = json.read(jsonItemPath);
			} catch (PathNotFoundException e1) {
				LOGGER.error(endsite + "." + job.getId()
						+ ".ITEM_SKUID_NOT_FOUND_IN_JSON__SKIPPING.itemUrl:{}",
						completeUrl);
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
			if (itemJsonArrayString == null || itemJsonArrayString.isEmpty()) {
				LOGGER.error(endsite + "." + job.getId()
						+ ".ITEM_SKUID_NOT_FOUND_IN_JSON__SKIPPING.itemUrl:{}",
						completeUrl);
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
			Object itemJsonString = itemJsonArrayString.get(0);

			DocumentContext itemJson = JsonPath.parse(itemJsonString);
			String productName = null, brandName = null, imageUrl = null;
			Integer price = null;
			try {
				productName = itemJson.read(businessProps.getProperty("koovs.relative.json.name"));
				itemVO.setName(productName);
			} catch (PathNotFoundException e) {
				LOGGER.error(
						endsite
								+ "."
								+ job.getId()
								+ ".ITEM_PRODUCT_NAME_NOT_FOUND_IN_JSON__IGNORING.itemUrl:{}",
						completeUrl);
			}
			try {
				brandName = itemJson.read(businessProps.getProperty("koovs.relative.json.brand"));
				itemVO.setBrand(brandName);
			} catch (PathNotFoundException e) {
				LOGGER.error(
						endsite
								+ "."
								+ job.getId()
								+ ".ITEM_BRAND_NAME_NOT_FOUND_IN_JSON__IGNORING.itemUrl:{}",
						completeUrl);
			}
			try {
				price = itemJson.read(businessProps.getProperty("koovs.relative.json.price1"));
				if (price == null) {
					LOGGER.error(endsite + "." + job.getId()
							+ ".ITEM_PRICE_NULL_IN_JSON__IGNORING.itemUrl:{}",
							completeUrl);
				}
				itemVO.setPrice(new BigDecimal(price));
			} catch (PathNotFoundException e) {
				LOGGER.error(endsite + "." + job.getId()
						+ ".ITEM_PRICE_NOT_FOUND_IN_JSON__IGNORING.itemUrl:{}",
						completeUrl);
			}
			try {
				imageUrl = itemJson.read(businessProps.getProperty("koovs.relative.json.image1"));
				itemVO.setEndsiteImageUrl(imageUrl);
			} catch (PathNotFoundException e) {
				LOGGER.error(
						endsite
								+ "."
								+ job.getId()
								+ ".ITEM_IMAGE_URL_NOT_FOUND_IN_JSON__IGNORING.itemUrl:{}",
						completeUrl);
			}
			// This will log which itemVO attributes were not found
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
									+ ".CLOUDINARY_UPLOAD_EXCEPTION__SKIPPING_ITEM. itemUrl:{}",
							itemVO.getEndsiteUrl());
					break;
				}
			}
			try {
				start = System.currentTimeMillis();
				itemRepository.save(itemVO);
//				 System.out.println(itemVO);

				LOGGER.info(Util
						.logTime(job, endsite, "MONGO_SAVE_ITEM", start));
			} catch (Exception e) {
				Throwable rootException = ExceptionUtils.getRootCause(e);
				if (rootException instanceof DuplicateKeyException) {
					// Eat exception here else throw exception
					LOGGER.warn(
							endsite
									+ "."
									+ job.getId()
									+ ".MONGO_DuplicateKeyException__EATING.itemUrl:{}",
							itemVO.getEndsiteUrl());
				} else {
					LOGGER.error(endsite + "." + job.getId()
							+ ".MONGO_SAVE_FAILURE.itemUrl:{}",
							itemVO.getEndsiteUrl(), e);
					exceptionSkippingCounter++;
					if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
						LOGGER.error(endsite + "." + job.getId()
								+ ".EXCEPTION_SKIPPED_COUNTER.itemUrl:{}",
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

	private Optional<NameValuePair> getUrlId(String url, String key)
			throws UnsupportedEncodingException, URISyntaxException {
		List<NameValuePair> queryParams = new URIBuilder(url).getQueryParams();
		Optional<NameValuePair> valuePair = queryParams.stream()
				.filter(pair -> pair.getName().equals(key)).findFirst();
		return valuePair;
	}

	/**
	 * Scraped Price eg. "2186" This Util method converts the String intu
	 * BigDecimal
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
		BigDecimal price1;
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
