package com.example.demo.attendance.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/external")
public class MockGovernmentApiController {

    @GetMapping("/minimum-wage")
    public Map<String, Object> getMinimumWage() throws InterruptedException {
        // Simulate a slow government API that takes 3 seconds
        Thread.sleep(3000);
        
        Map<String, Object> response = new HashMap<>();
        response.put("multiplier", new BigDecimal("1.0"));
        response.put("status", "SUCCESS");
        return response;
    }
}
