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

    @Value("${elasticsearch.use-ssl}")
    private boolean useSsl;




    @Value("${elasticsearch.truststore-path}")
    private String truststorePath;
    @Value("${elasticsearch.truststore-password}")
    private String truststorePassword;

    private SSLContext createSslContext() throws Exception {
        log.info("Начинаем создание SSLContext...");
        log.info("Truststore path: {}", truststorePath);
        log.info("Truststore password: {}", truststorePassword);

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

        String resourcePath = truststorePath.replace("classpath:", "");
        log.info("Ищем truststore по пути: {}", resourcePath);

        try (InputStream is = ElasticsearchConfig.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.error("Truststore не найден: {}", resourcePath);
                throw new IllegalArgumentException("Truststore не найден: " + resourcePath);
            }
            trustStore.load(is, truststorePassword.toCharArray());
            log.info("Truststore успешно загружен");
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);

        log.info("SSLContext успешно создан");
        return sslContext;
    }



    @Bean
    public ElasticsearchClient elasticsearchClient() throws Exception {
        log.info("Настройка подключения к Elasticsearch: https://{}:{}", host, port);

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "https"));

        if (useSsl) {
            // Вот здесь вызывается createSslContext()
            SSLContext sslContext = createSslContext();

            builder.setHttpClientConfigCallback(httpAsyncClientBuilder ->
                    httpAsyncClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider)
                            .setSSLContext(sslContext)  // ← используем созданный SSLContext
            );
        } else {
            builder.setHttpClientConfigCallback(httpAsyncClientBuilder ->
                    httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }

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
                throw new RuntimeException("Индекс 'products' отсутствует");
            } else {
                log.info("Индекс 'products' уже существует");
            }
        };
    }


}