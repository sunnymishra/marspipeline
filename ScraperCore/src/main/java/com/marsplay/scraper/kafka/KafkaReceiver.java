package com.marsplay.scraper.kafka;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.marsplay.repository.Job;

@Component
public class KafkaReceiver {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(KafkaReceiver.class);

	private CountDownLatch latch = new CountDownLatch(1);

	public CountDownLatch getLatch() {
		return latch;
	}

	@KafkaListener(topics = "${kafka.topic.scrape}")
	public void receive(@Payload Job job, 
			  @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition){
//	public void doSomething(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, @Payload String payload) {
		LOGGER.info("#################################################################################"
				+ "received payload='{}', partitionId='{}', key='{}'", job, partition);
		latch.countDown();
	}
	
}
