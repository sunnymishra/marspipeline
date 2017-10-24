package com.marsplay.scraper.lib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.marsplay.repository.Job;
import com.marsplay.scraper.lib.Constants.Endsites;

public class Util {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Util.class);
	private static Properties businessProps = Constants.getBusinessProps();

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
	public static String logTime(Job job, Endsites endsite, String work, long startTime) {
		long localDuration = System.currentTimeMillis() - startTime;
		String duration=((int) (localDuration / 1000) % 60)	+ "s " + ((int) (localDuration % 1000)) + "m";
		if(endsite!=null)
			return ">>>DURATION."+endsite+"."+job.getId()+"."+work+":" + duration;
		else
			return ">>>DURATION."+job.getId()+"."+work+":" + duration;
	}
	public void sleep(int seconds) {
		try {
			TimeUnit.SECONDS.sleep(seconds);
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
	}
}
