package com.marspipeline.scraper.kafka;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.marspipeline.repository.Job;
import com.marspipeline.scraper.ScraperService;
import com.marspipeline.scraper.lib.Util;

@Component
public class KafkaReceiver {

	@Autowired
	ScraperService scraper;
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(KafkaReceiver.class);

	private CountDownLatch latch = new CountDownLatch(1);

	public CountDownLatch getLatch() {
		return latch;
	}

	@KafkaListener(topics = "${kafka.topic.scrape}")
	public void receive(@Payload Job job, 
			  @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition) throws IOException, InterruptedException{
//	public void doSomething(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, @Payload String payload) {
		long start=System.currentTimeMillis();
		LOGGER.info("#####KafkaListener payload='{}', partitionId='{}'", job, partition);
		try {
			scraper.startScraping(job);
		} catch (Exception e) {
			LOGGER.error("Eating complete invokation exception in kafkaReceiver for job:"+job.getId(), e);
		}
		LOGGER.info("#####KafkaListener Completed jobId={}",job.getId());
		LOGGER.info(Util.logTime(job, null, "SCRAPE_COMPLETE", start));
	}
	
}
