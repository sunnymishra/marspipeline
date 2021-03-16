package com.marspipeline.web.lib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Util.class);
	
	public static String logTime(long startTime, String work) {
		long localDuration = System.currentTimeMillis() - startTime;
		return ">>>>>>>>Duration "+work+":" + ((int) (localDuration / 1000) % 60)
				+ "s " + ((int) (localDuration % 1000)) + "m";
	}
}
