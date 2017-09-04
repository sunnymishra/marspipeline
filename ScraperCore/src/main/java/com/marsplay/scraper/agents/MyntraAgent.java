package com.marsplay.scraper.agents;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.marsplay.repository.Item;
import com.marsplay.repository.ItemRepository;
import com.marsplay.scraper.lib.Constants.ElementType;
import com.marsplay.scraper.lib.Constants.Endsites;
import com.marsplay.scraper.lib.Util;
import com.mongodb.DuplicateKeyException;

//@Service
public class MyntraAgent extends Agent {
	ItemRepository itemRepository;
	
	public MyntraAgent(WebDriver driver, ItemRepository itemRepository) {
		super(driver);
		this.itemRepository=itemRepository;
		this.endsite=Endsites.MYNTRA;
		// PageFactory.initElements(driver, this);
	}

	@Override
	public void scrapeAction() throws Exception {
		WebElement container = driver.findElement(By
				.xpath(businessProps.getProperty("endsite.myntra.xpath.container")));
		List<WebElement> itemContainer = driver.findElements(By
				.xpath(businessProps.getProperty("endsite.myntra.xpath.item")));
		int counter = 0;
		int scrollHeight = -1, scrollItemCount = -1;
		for (WebElement item : itemContainer) {
			int retry = 0;
//			ItemVO itemVO = new ItemVO(Endsites.MYNTRA);
			Item itemVO = new Item();
			itemVO.setEndSite(Endsites.MYNTRA.name());
			++counter;
			// item.sendKeys( Keys.DOWN ); //simulate visual movement
			boolean isElementLoaded = false;
			while (!isElementLoaded) {
				try {
					WebElement url = item.findElement(By.xpath(businessProps.getProperty("endsite.myntra.relative.xpath.url")));
					itemVO.setEndsiteUrl(url.getAttribute("href"));
					WebElement brand = item
							.findElement(By
									.xpath(businessProps.getProperty("endsite.myntra.relative.xpath.brand")));
					itemVO.setBrand(brand.getText());
					// WebElement element =
					// 	webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("periodicElement")));
					WebElement name = item
							.findElement(By
									.xpath(businessProps.getProperty("endsite.myntra.relative.xpath.name")));
					itemVO.setName(name.getText());
					System.out.println(itemVO);

					WebElement price = null;
					try {
						price = item
								.findElement(By
										.xpath(businessProps.getProperty("endsite.myntra.relative.xpath.price1")));
					} catch (org.openqa.selenium.NoSuchElementException e) {
						price = item
								.findElement(By
										.xpath(businessProps.getProperty("endsite.myntra.relative.xpath.price2")));
					}
					try {
						itemVO.setPrice(Util.formatMyntraPrice(price.getText()));
					} catch (IllegalArgumentException e) {
						// TODO Log this error for Flagging Scraping error
						e.printStackTrace();
					}

					WebElement image;
					try {
						// Image load may take time, therefore using Selenium
						// FluentWait API below
						image = waitAndExtractElement(item, ElementType.XPATH,
								businessProps.getProperty("endsite.myntra.relative.xpath.image1"));
					} catch (org.openqa.selenium.NoSuchElementException e) {
						image = waitAndExtractElement(item, ElementType.XPATH,
								businessProps.getProperty("endsite.myntra.relative.xpath.image2"));
					}
					itemVO.setEndsiteImageUrl(image.getAttribute("src"));

					isElementLoaded = true;
				} catch (org.openqa.selenium.NoSuchElementException e) {
					Thread.sleep(2000);
					System.out.println(e.getMessage());
					// TODO If this Exception times out, then Log this error for
					// Flagging Scraping error
					isElementLoaded = false;
					retry++;
					if (retry >= Integer.parseInt(businessProps.getProperty("endsite.common.element_fetch_retry_max")))
						throw e;
				}

			}
			try {
				Map<String, Object> responseMap = uploadFile(itemVO.getEndsiteImageUrl());
				itemVO.setCdnImageUrl((String) responseMap.get("secure_url"));
				itemVO.setCdnImageId((String) responseMap.get("public_id"));
				itemRepository.save(itemVO);
			} catch (Exception e) {
				e.printStackTrace();
				Throwable rootException = ExceptionUtils.getRootCause(e);
				if(rootException instanceof DuplicateKeyException){
					// Eat exception here else throw exception
					System.out.println("Eating Mongo DuplicateKeyException here in Extractor");
				}else{
					throw e;
				}
				
			}	// MongoDB save in ITEM collection
			
			if (scrollItemCount == -1)
				scrollItemCount = Util.getNoOfItemsInEachRow(container
						.getSize().getWidth(), item.getSize().getWidth());
			if (counter % scrollItemCount == 0) {
				Thread.sleep(700);
				JavascriptExecutor jse = (JavascriptExecutor) driver;
				if (scrollHeight == -1)
					scrollHeight = Util.ceilPixel(item.getSize().getHeight(),
							Integer.parseInt(businessProps.getProperty("endsite.common.vertical_scroll_offset_pixel")));
				jse.executeScript("window.scrollBy(0," + scrollHeight + ")", "");
			}
			if (counter >= Integer.parseInt(businessProps.getProperty("endsite.common.no_of_items_to_scrape")))
				break;

		}
	}

}