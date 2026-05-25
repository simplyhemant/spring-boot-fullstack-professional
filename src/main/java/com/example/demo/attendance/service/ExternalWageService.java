package com.example.demo.attendance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class ExternalWageService {

    private final RestTemplate restTemplate;

    public ExternalWageService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(4))
                .build();
    }

    public BigDecimal fetchMinimumWageMultiplier() {
        try {
            String url = "http://localhost:8080/api/external/minimum-wage";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object multiplier = response.getBody().get("multiplier");
                if (multiplier != null) {
                    return new BigDecimal(multiplier.toString());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch minimum wage from external API: {}. Falling back to default (1.0).", e.getMessage());
        }
        return BigDecimal.ONE;
    }
}
