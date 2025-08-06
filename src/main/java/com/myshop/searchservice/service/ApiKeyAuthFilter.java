package com.myshop.searchservice.service;


import com.myshop.searchservice.config.ApiKeyConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ApiKeyAuthFilter implements Filter {

    @Autowired
    private ApiKeyConfig apiKeyConfig;

    private static final String API_KEY_HEADER = "X-API-Key";



    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String apiKey = httpRequest.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isEmpty()) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"error\": \"X-API-Key header is missing\"}");
            return;
        }

        // Защита от time-attack: используем constant-time сравнение
        if (!java.security.MessageDigest.isEqual(apiKey.getBytes(), apiKeyConfig.getKey().getBytes())) {
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.getWriter().write("{\"error\": \"Invalid API key\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}