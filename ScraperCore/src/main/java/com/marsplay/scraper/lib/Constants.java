package com.marsplay.scraper.lib;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.marsplay.scraper.ScraperService;

public class Constants {
	private static Properties businessProp, applicationProp;
	static{
		populatePropertiesFile();
	}
	
	public enum Endsites{
		MYNTRA, AMAZON
	}
	public enum Currency{
		INR
	}
	public enum ElementType{
		ID, XPATH, CSS
	}
	
	/**
	 * Public method will return the Business properties file read into the Properties instance. {{Initialize Once in static block, use many times}}
	 * 
	 * @return Properties
	 */
	public static Properties getBusinessProps(){
		return businessProp;
	}
	/**
	 * Public method will return the Application properties file read into the Properties instance. {{Initialize Once in static block, use many times}}
	 * 
	 * @return Properties
	 */
	public static Properties getApplicationProps(){
		return applicationProp;
	}
	
	private static void populatePropertiesFile() {
		businessProp = new Properties();
		applicationProp = new Properties();
		InputStream input = null;

		try {
			input=ScraperService.class.getClassLoader().getResourceAsStream("business.properties");
			// load a properties file
			businessProp.load(input);
			
			input=ScraperService.class.getClassLoader().getResourceAsStream("application.properties");
			// load a properties file
			applicationProp.load(input);
			
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("Not able to read business.properties or application.properties file!!!");
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
