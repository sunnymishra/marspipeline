package com.marsplay.scraper.agents;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.marsplay.repository.Item;
import com.marsplay.repository.ItemRepository;
import com.marsplay.repository.Job;
import com.marsplay.scraper.ScraperService;
import com.marsplay.scraper.lib.Constants.Endsites;
import com.marsplay.scraper.lib.Util;
import com.mongodb.DuplicateKeyException;

@Service("flipkartAgent")
public class FlipkartAgent extends Agent {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ScraperService.class);
	@Autowired
	private ItemRepository itemRepository;

	public FlipkartAgent() {
		super();
		LOGGER.info("Constructor FlipkartAgent()");
		this.endsite = Endsites.FLIPKART;
	}

	@Override
	public Object launchEndsite(Job job) throws Exception {
		String endsiteBaseUrl = businessProps
				.getProperty("flipkart.endsite.searchurl");
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

		LOGGER.info(Util.logTime(job, endsite, "ENDITE_FLIPKART_OPEN", start));

		start = System.currentTimeMillis();
		saveScrapedHtml(document, job.getId()); // Saving Scraped Html
		LOGGER.info(Util.logTime(job, endsite, "SAVE_FLIPKART_HTML", start));

		return document;

	}

	@Override
	public void scrapeAction(Job job) throws Exception {
		LOGGER.info("Scraping {} for Job '{}'", endsite, job);

		Document document = (Document) launchEndsite(job);

		Elements scripts = document.select("script"); // Get the script part
		Object json = null;
		for (Element script : scripts) {
			// Regex for the value of the key
			Pattern p = Pattern.compile(businessProps
					.getProperty("flipkart.scripttag.jsonpath"));

			Matcher m = p.matcher(script.html());

			if (m.find()) {
				String jsonString = m.group(2);
				json = Configuration.defaultConfiguration().jsonProvider()
						.parse(jsonString);
			}
		}
		if (json == null)
			LOGGER.error("Endsite:" + endsite.name()
					+ " Price Exception JobId: {} Exception:{}", job.getId(),
					"No JSON script tag found.");
		Elements itemContainer = Xsoup
				.compile(businessProps.getProperty("flipkart.xpath.container"))
				.evaluate(document).getElements();
		if(itemContainer.isEmpty()){
			LOGGER.error(endsite+"."+job.getId()+".PARSING_FAILURE_EMPTY_CONTAINER");
			return;
		}
		Elements itemSubContainers = Xsoup
				.compile(
						businessProps
								.getProperty("flipkart.xpath.subcontainer"))
				.evaluate(itemContainer.first()).getElements();
		if(itemSubContainers.isEmpty()){
			LOGGER.error(endsite+"."+job.getId()+".PARSING_FAILURE_EMPTY_SUBCONTAINER");
			return;
		}
		int counter = 0;
		int exceptionSkippingCounter = 0;
		String exceptionSkippingMaxCountStr = businessProps
				.getProperty("exception.skipping.maxcount");
		int exceptionSkippingMaxCount = Integer
				.parseInt(exceptionSkippingMaxCountStr);

		for (Element subContainer : itemSubContainers) {
			if(itemSubContainers.isEmpty()){
				LOGGER.error(endsite+"."+job.getId()+".PARSING_FAILURE_EMPTY_ITEMTAG");
				continue;
			}
			Elements items = Xsoup
					.compile(businessProps.getProperty("flipkart.xpath.item"))
					.evaluate(subContainer).getElements();
			for (Element item : items) {
				Item itemVO = new Item();
				itemVO.setJob(job);
				itemVO.setEndSite(endsite.name());

				String baseUrl = businessProps
						.getProperty("flipkart.endsite.baseurl");
				Elements urlElem = Xsoup
						.compile(
								businessProps
										.getProperty("flipkart.relative.xpath.url1"))
						.evaluate(item).getElements();
				String uri = urlElem.attr("href");
				itemVO.setEndsiteUrl(baseUrl + uri);

				if (StringUtil.isBlank(itemVO.getEndsiteUrl())) {
					LOGGER.error(
							"endsiteUrl not found. Skipping jobId:{} for Endsite:{}",
							job.getId(), endsite.name());
					exceptionSkippingCounter++;
					if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
						LOGGER.error(endsite+"."+job.getId()+".EXCEPTION_SKIPPING_COUNTER_EXCEEDED__BREAKINGLOOP");
						break;
					} else
						continue;
				}
				++counter;

				String brandPath = businessProps
						.getProperty("flipkart.relative.xpath.brand");
				String brand = "";
				if (!StringUtil.isBlank(brandPath)) {
					Elements brandElem = Xsoup.compile(brandPath)
							.evaluate(item).getElements();
					brand = brandElem.text();
				}

				itemVO.setBrand(brand);
				Elements name = Xsoup
						.compile(
								businessProps
										.getProperty("flipkart.relative.xpath.name"))
						.evaluate(item).getElements();
				itemVO.setName(name.attr("title"));

				String pId = null;

				Optional<NameValuePair> pair = getUrlPid(uri);

				if (pair.isPresent()) {
					pId = pair.get().getValue();
					String baseUrlPath = businessProps
							.getProperty("flipkart.relative.xpath.image1");
					baseUrlPath = baseUrlPath.replace("_PID_", pId);
					String imageUrl = JsonPath.read(json, baseUrlPath);
					itemVO.setEndsiteImageUrl(formatImageUrl(imageUrl));
				} else
					LOGGER.error("Endsite:" + endsite.name()
							+ " Price Exception JobId: {} Exception:{}",
							job.getId(), "No pid found in URI queryParams.");

				Elements price = Xsoup
						.compile(
								businessProps
										.getProperty("flipkart.relative.xpath.price1"))
						.evaluate(item).getElements();
				try {
					itemVO.setPrice(formatPrice(price.text()));
				} catch (IllegalArgumentException e) {
					LOGGER.error("Price1 Exception for JobId:" + job.getId()
							+ " itemUrl:\"" + itemVO.getEndsiteUrl() + "\"::"
							+ e.getMessage());
				}

				validateScrapedHtml(job.getId(), itemVO); // This will log what
															// itemVO attributes
															// were not found

//				System.out.println(itemVO);
				long start;
				try {
					start = System.currentTimeMillis();
					Map<String, Object> responseMap = uploadFile(itemVO
							.getEndsiteImageUrl());
					LOGGER.info(Util.logTime(job, endsite, "CLOUDINARY_UPLOAD", start));

					itemVO.setCdnImageUrl((String) responseMap
							.get("secure_url"));
					itemVO.setCdnImageId((String) responseMap.get("public_id"));
				} catch (Exception e) {
					LOGGER.error(endsite+"."+job.getId()+".CLOUDINARY_UPLOAD_EXCEPTION__SKIPPING_ITEM",e);
					exceptionSkippingCounter++;
					if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
						LOGGER.error(endsite+"."+job.getId()+".CLOUDINARY_UPLOAD_EXCEPTION__SKIPPING_ITEM. itemUrl:{}"
								, itemVO.getEndsiteUrl());
						break;
					}
				}
				try {
					start = System.currentTimeMillis();
					itemRepository.save(itemVO);
					LOGGER.info(Util.logTime(job, endsite, "MONGO_SAVE_ITEM", start));
				} catch (Exception e) {
					// e.printStackTrace();
					Throwable rootException = ExceptionUtils.getRootCause(e);
					if (rootException instanceof DuplicateKeyException) {
						// Eat exception here else throw exception
						LOGGER.warn(endsite+"."+job.getId()+".MONGO_DuplicateKeyException_EATING itemUrl:{}",
								itemVO.getEndsiteUrl());
					} else {
						LOGGER.error(endsite+"."+job.getId()+".MONGO_SAVE_EXCEPTION itemUrl:{}",
								itemVO.getEndsiteUrl(),e);
						exceptionSkippingCounter++;
						if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
							LOGGER.error(endsite+"."+job.getId()+".EXCEPTION_SKIPPED_COUNTER itemUrl:{}",
									itemVO.getEndsiteUrl(),e);
							break;
						} else
							continue;
					}

				} // MongoDB save in ITEM collection
				if (counter >= Integer.parseInt(businessProps
						.getProperty("common.no_of_items_to_scrape")))
					break;
			}
			// Because Flipkart has Loop inside Loop therefore we have to break 2 times.
			if (counter >= Integer.parseInt(businessProps
					.getProperty("common.no_of_items_to_scrape")))
				break;
			if (exceptionSkippingCounter >= exceptionSkippingMaxCount) {
				break;
			}
				
		}
	}

	private Optional<NameValuePair> getUrlPid(String url)
			throws UnsupportedEncodingException, URISyntaxException {
		List<NameValuePair> queryParams = new URIBuilder(url).getQueryParams();
		Optional<NameValuePair> valuePair = queryParams.stream()
				.filter(pair -> pair.getName().equals("pid")).findFirst();
		return valuePair;
	}

	/**
	 * Sample ImageUrl: "http://rukmini1.flixcart.com/image/{@width}/
	 * {@height}
	 * /top/z/g/m/polka-dot-chiffon-top-sei-bello-m-original-imaejghpu3zfe79q.
	 * jpeg?q={@quality}"
	 * 
	 * @param imageUrl
	 * @return
	 */
	private String formatImageUrl(String imageUrl) {
		imageUrl = imageUrl
				.replace(businessProps.getProperty("flipkart.image.width.key"),
						businessProps.getProperty("flipkart.image.width.value"))
				.replace(
						businessProps.getProperty("flipkart.image.height.key"),
						businessProps
								.getProperty("flipkart.image.height.value"))
				.replace(
						businessProps.getProperty("flipkart.image.quality.key"),
						businessProps
								.getProperty("flipkart.image.quality.value"));
		return imageUrl;
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
		BigDecimal price1;
		priceTemp = priceTemp.replaceAll("\u20B9", "");
		priceTemp = priceTemp.trim();
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
