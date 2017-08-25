package com.marsplay.scraper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.marsplay.Application;

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
			input=Application.class.getClassLoader().getResourceAsStream("business.properties");
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
