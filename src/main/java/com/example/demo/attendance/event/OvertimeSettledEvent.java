package com.example.demo.attendance.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class OvertimeSettledEvent extends ApplicationEvent {
    private final Long workerId;
    private final String phone;
    private final String month;
    private final BigDecimal totalAmount;

    public OvertimeSettledEvent(Object source, Long workerId, String phone, String month, BigDecimal totalAmount) {
        super(source);
        this.workerId = workerId;
        this.phone = phone;
        this.month = month;
        this.totalAmount = totalAmount;
    }
}
