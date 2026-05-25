package com.example.demo.attendance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class SmsService {

    public void sendSmsForSettlement(String phone, String month, BigDecimal amount) {
        log.info("Sending SMS to {}: Your {} overtime of \u20b9{} has been settled.", phone, month, amount);
        // Simulating external network request latency or potential failure
        if ("9999999999".equals(phone)) {
            throw new RuntimeException("SMS Gateway network connection timed out (simulated failure)");
        }
    }
}
