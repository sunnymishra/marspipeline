package com.marsplay.scraper.kafka;

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

import com.marsplay.repository.Job;
import com.marsplay.scraper.ScraperService;

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
		LOGGER.info("#################################################################################"
				+ "received payload='{}', partitionId='{}', key='{}'", job, partition);
//		latch.countDown();
		scraper.startScraping(job);
		LOGGER.info("##############################KafkaReceiver Completed 1 Job");
		long duration=System.currentTimeMillis()-start;
		LOGGER.info("Overall startScraping() duration:"+ ((int) (duration / 1000) % 60)+"s "+((int) (duration%1000))+"m");
	}
	
}
