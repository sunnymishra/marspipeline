package com.marsplay.scraper;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.marsplay.core.Item;
//import com.marsplay.core.Item;
import com.marsplay.scraper.Constants.ElementType;
import com.marsplay.scraper.Constants.Endsites;

public class MyntraExtractor extends Extractor {

	public MyntraExtractor(WebDriver driver) {
		super(driver);
		// PageFactory.initElements(driver, this);
	}

	@Override
	public void scrapeAction() throws InterruptedException {
		WebElement container = driver.findElement(By
				.xpath(props.getProperty("endsite.myntra.xpath.container")));
		List<WebElement> itemContainer = driver.findElements(By
				.xpath(props.getProperty("endsite.myntra.xpath.item")));
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
					WebElement url = item.findElement(By.xpath(props.getProperty("endsite.myntra.relative.xpath.url")));
					itemVO.setSiteUrl(url.getAttribute("href"));
					WebElement brand = item
							.findElement(By
									.xpath(props.getProperty("endsite.myntra.relative.xpath.brand")));
					itemVO.setBrand(brand.getText());
					// WebElement element =
					// 	webDriverWait.until(ExpectedConditions.elementToBeClickable(By.id("periodicElement")));
					WebElement name = item
							.findElement(By
									.xpath(props.getProperty("endsite.myntra.relative.xpath.name")));
					itemVO.setName(name.getText());
					System.out.println(itemVO);

					WebElement price = null;
					try {
						price = item
								.findElement(By
										.xpath(props.getProperty("endsite.myntra.relative.xpath.price1")));
					} catch (org.openqa.selenium.NoSuchElementException e) {
						price = item
								.findElement(By
										.xpath(props.getProperty("endsite.myntra.relative.xpath.price2")));
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
								props.getProperty("endsite.myntra.relative.xpath.image1"));
					} catch (org.openqa.selenium.NoSuchElementException e) {
						image = waitAndExtractElement(item, ElementType.XPATH,
								props.getProperty("endsite.myntra.relative.xpath.image2"));
					}
					itemVO.setImageUrl(image.getAttribute("src"));

					isElementLoaded = true;
				} catch (org.openqa.selenium.NoSuchElementException e) {
					Thread.sleep(2000);
					System.out.println(e.getMessage());
					// TODO If this Exception times out, then Log this error for
					// Flagging Scraping error
					isElementLoaded = false;
					retry++;
					if (retry >= Integer.parseInt(props.getProperty("endsite.common.element_fetch_retry_max")))
						throw e;
				}

			}
			
			if (scrollItemCount == -1)
				scrollItemCount = Util.getNoOfItemsInEachRow(container
						.getSize().getWidth(), item.getSize().getWidth());
			if (counter % scrollItemCount == 0) {
				Thread.sleep(700);
				JavascriptExecutor jse = (JavascriptExecutor) driver;
				if (scrollHeight == -1)
					scrollHeight = Util.ceilPixel(item.getSize().getHeight(),
							Integer.parseInt(props.getProperty("endsite.common.vertical_scroll_offset_pixel")));
				jse.executeScript("window.scrollBy(0," + scrollHeight + ")", "");
			}
			if (counter >= Integer.parseInt(props.getProperty("endsite.common.no_of_items_to_scrape")))
				break;

		}
	}

}
