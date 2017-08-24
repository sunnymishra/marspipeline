package com.marsplay;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Constants {
	private static Properties prop;
	static{
		prop=readPropertiesFile();
	}
	
	
	public enum Endsites{
		MYNTRA
	}
	public enum Currency{
		INR
	}
	public enum ElementType{
		ID, XPATH, CSS
	}
	
	public static final int NO_OF_ITEMS_PER_ENDSITE=30;
	public static final int ENDSITE_VERTICAL_SCROLL_OFFSET=50;
	public static final int PAGE_LOAD_TIMEOUT_SECONDS=5;
	public static final int ELEMENT_LOAD_TIMEOUT_SECONDS=5;
	public final static long ELEMENT_LOAD_SLEEP_TIMEOUT_MILLIS = 500;
	public final static long ELEMENT_FETCH_RETRY_MAX = 3;
	
	
	
	/**
	 * Public method will return the properties file read into the Properties instance. {{Initialize Once in static block, use many times}}
	 * 
	 * @return Properties
	 */
	public static Properties getProps(){
		return prop;
	}
	
	
	private static Properties readPropertiesFile() {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			input=Scraper.class.getClassLoader().getResourceAsStream("business.properties");
			// load a properties file
			prop.load(input);
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("Not able to read business.properties file!!!");
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return prop;
	}
}
