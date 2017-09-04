package com.marsplay.scraper.kafka;

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

@Configuration
@EnableKafka
public class ReceiverConfig {

	@Value("${kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${kafka.scraper.groupid}")
	private String groupId;

	@Bean
	public Map<String, Object> consumerConfigs() {
		Map<String, Object> props = new HashMap<>();
		// list of host:port pairs used for establishing the initial connections
		// to the Kakfa cluster
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		// props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
		// StringDeserializer.class);
		// props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
		// JsonDeserializer.class);

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

	/*
	 * @Bean public Receiver receiver() { return new Receiver(); }
	 */
}