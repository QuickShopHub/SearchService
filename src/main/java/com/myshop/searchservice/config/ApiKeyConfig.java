package com.myshop.searchservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyConfig {

    @Value("${app.api.key}")
    private String key;

    public String getKey() {
        return key;
    }
}