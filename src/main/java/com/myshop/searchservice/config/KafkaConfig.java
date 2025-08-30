package com.myshop.searchservice.config;

import com.myshop.searchservice.DTO.DeleteDTO;
import com.myshop.searchservice.DTO.ProductForSearch;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {


    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Общие настройки
    private Map<String, Object> baseConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "updateConsumer_v2");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false); // Отключаем headers
        return props;
    }


    @Bean
    public ConsumerFactory<String, ProductForSearch> productConsumerFactory() {
        Map<String, Object> props = baseConsumerConfigs();
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.myshop.searchservice.DTO.ProductForSearch");
        return new DefaultKafkaConsumerFactory<>(props);
    }


    @Bean
    public ConsumerFactory<String, DeleteDTO> deleteListConsumerFactory() {
        Map<String, Object> props = baseConsumerConfigs();
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.myshop.searchservice.DTO.DeleteDTO");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductForSearch> kafkaListenerContainerFactoryUpdate() {
        ConcurrentKafkaListenerContainerFactory<String, ProductForSearch> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productConsumerFactory());

        // Более надёжная обработка ошибок
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, e) -> {
                    log.error("Error processing product update: {}", consumerRecord.value(), e);
                },
                new FixedBackOff(1000L, 2)
        );
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeleteDTO> kafkaListenerContainerFactoryDelete() {
        ConcurrentKafkaListenerContainerFactory<String, DeleteDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deleteListConsumerFactory());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, e) -> {
                    log.error("Error processing delete request: {}", consumerRecord.value(), e);
                },
                new FixedBackOff(1000L, 2)
        );
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}