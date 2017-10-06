package com.marsplay.scraper.lib;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Util.class);
	private static Properties businessProps = Constants.getBusinessProps();
	/**
	 * Myntra Scraped Price eg. "Rs. 500". This Util method
	 * trims the String and strips Alphabets 'Rs.' and then
	 * returns parsed Double of the price
	 * @param String
	 * @return double
	 */
	public static BigDecimal formatMyntraPrice(String price){
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
	/**
	 * We want the pixel to be multiple of offset. 
	 * Eg. If actual pixel is 332px and offset is 50,
	 * then offsetted pixel should be ceiling of multiple of 50 = 350px
	 * 
	 * @param int pixel
	 * @param int offset
	 * @return int
	 */
	public static int ceilPixel(int pixel, int offset){
		double x = (double)pixel/offset;
		double ceilX = Math.ceil(x);
		int result=(int)ceilX*offset;
		return result;
	}
	/**
	 * This method returns the expected no. of items in each row
	 * @param containerWidth
	 * @param itemWidth
	 * @return int
	 */
	public static int getNoOfItemsInEachRow(int containerWidth, int itemWidth){
		int noOfItemsInEachRow = containerWidth / itemWidth;	// Using integer will trim the float values. This is expected calculation
		return noOfItemsInEachRow;
	}
	
	public static String getTime(){
		
		Date date = new Date();
		SimpleDateFormat dformat = new SimpleDateFormat(businessProps.getProperty("date.format"));
		String dte = dformat.format(date);
		return dte;
	 }
	public static String logTime(long startTime, String work) {
		long localDuration = System.currentTimeMillis() - startTime;
		return ">>>>>>>>Duration "+work+":" + ((int) (localDuration / 1000) % 60)
				+ "s " + ((int) (localDuration % 1000)) + "m";
	}
	public void sleep(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
}
