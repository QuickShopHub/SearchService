package com.myshop.searchservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ElasticsearchConfig {

    private RestClient restClient;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // Создаём HTTP-клиент
        this.restClient = RestClient.builder(
                new HttpHost("localhost", 9200)
        ).build();

        // Создаём транспорт с Jackson-маппером
        ElasticsearchTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper()
        );

        return new ElasticsearchClient(transport);
    }

    @PreDestroy
    public void close() {
        try {
            if (restClient != null) {
                restClient.close();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}