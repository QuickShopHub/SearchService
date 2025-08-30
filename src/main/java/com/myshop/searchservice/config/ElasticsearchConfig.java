package com.myshop.searchservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;


@Lazy
@Configuration
@Slf4j
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;










    @Bean
    public ElasticsearchClient elasticsearchClient() throws Exception {
        log.info("Настройка подключения к Elasticsearch: {}://{}:{}", "http", host, port);

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        // Используем правильный протокол
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "http"));

        builder.setHttpClientConfigCallback(httpAsyncClientBuilder ->
                httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
        );

        ElasticsearchTransport transport = new RestClientTransport(builder.build(), new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }



    @Bean
    @DependsOn("elasticsearchClient")
    public ApplicationRunner createIndexRunner(ElasticsearchClient elasticsearchClient) {
        return args -> {
            BooleanResponse existsResponse = elasticsearchClient.indices()
                    .exists(b -> b.index("products"));

            if (!existsResponse.value()) {
                log.info("Создаём индекс 'products'...");
                CreateIndexResponse createResponse = elasticsearchClient.indices()
                        .create(b -> b.index("products"));

                if (createResponse.acknowledged()) {
                    log.info("Индекс 'products' успешно создан");
                } else {
                    log.warn("Не удалось создать индекс 'products'");
                }
            } else {
                log.info("Индекс 'products' уже существует");
            }
        };
    }


}