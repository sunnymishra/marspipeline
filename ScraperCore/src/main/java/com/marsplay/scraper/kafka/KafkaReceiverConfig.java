package com.marspipeline.scraper.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.marspipeline.repository.Job;

@Configuration
@EnableKafka
public class KafkaReceiverConfig {

	@Value("${kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${kafka.scraper.groupid}")
	private String groupId;

	@Bean
	public Map<String, Object> consumerConfigs() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		return props;
	}

	@Bean
	public ConsumerFactory<String, Job> consumerFactory() {
		return new DefaultKafkaConsumerFactory<>(consumerConfigs(),
				new StringDeserializer(), new JsonDeserializer<>(Job.class));
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Job> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, Job> factory = new ConcurrentKafkaListenerContainerFactory<String, Job>();
		factory.setConsumerFactory(consumerFactory());

		return factory;
	}

}