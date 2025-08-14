package com.myshop.searchservice.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class ApiKeyConfig {

    @Value("${app.api-key}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }
}