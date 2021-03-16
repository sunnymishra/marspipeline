//package com.marspipeline.scraper.kafka;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import java.util.Date;
//import java.util.concurrent.TimeUnit;
//
//import org.junit.Before;
//import org.junit.ClassRule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
//import org.springframework.kafka.test.rule.KafkaEmbedded;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import com.marspipeline.repository.Job;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@DirtiesContext
//// This ensures correct embedded broker port is set matching with Kafka server
//public class SpringKafkaApplicationTest {
//	private static final Logger LOGGER = LoggerFactory
//			.getLogger(SpringKafkaApplicationTest.class);
//	private static String TOPIC_NAME = "scrape.t";
//
//	@Autowired
//	private KafkaSender sender;
//
//	@Autowired
//	private KafkaReceiver receiver;
//
//	@Autowired
//	private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
//
//	@ClassRule
//	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true,
//			TOPIC_NAME);
//
//	@Before
//	public void setUp() throws Exception {
//		// This to ensure that Message sending waits until the partitions are
//		// assigned to the Listeners
//		for (MessageListenerContainer messageListenerContainer : kafkaListenerEndpointRegistry
//				.getListenerContainers()) {
//			ContainerTestUtils.waitForAssignment(messageListenerContainer,
//					embeddedKafka.getPartitionsPerTopic());
//		}
//	}
//
//	/*@Test
//	public void testReceive() throws Exception {
//		LOGGER.info("Inside testReceive method ################################");
//		sender.send(TOPIC_NAME, new Job("a123bbvn1", "This is test message", new Date()));
//
//		receiver.getLatch().await(10000, TimeUnit.MILLISECONDS);
//		assertThat(receiver.getLatch().getCount()).isEqualTo(0);
//	}*/
//
//}