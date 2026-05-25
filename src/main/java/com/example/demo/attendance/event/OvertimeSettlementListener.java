package com.example.demo.attendance.event;

import com.example.demo.attendance.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OvertimeSettlementListener {

    private final SmsService smsService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOvertimeSettlement(OvertimeSettledEvent event) {
        log.info("Transaction committed successfully for worker {} month {}. Initiating SMS notification.", 
                event.getWorkerId(), event.getMonth());
        try {
            smsService.sendSmsForSettlement(event.getPhone(), event.getMonth(), event.getTotalAmount());
            log.info("SMS notification successfully dispatched to worker {} for month {}.", 
                    event.getWorkerId(), event.getMonth());
        } catch (Exception e) {
            log.error("Failed to send SMS notification for worker {} month {}: {}. Settlement data remains intact.", 
                    event.getWorkerId(), event.getMonth(), e.getMessage());
            // In a production application, we would queue a retry task here
        }
    }
}
